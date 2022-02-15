package com.bsycorp.gradle.jib;

import com.bsycorp.gradle.jib.tasks.ImageInputs;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopySpecBackedCopyActionProcessingStream;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.WorkResults;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class JibTaskSupport {

    protected Logger logger;

    protected JibExtension extension;

    protected ObjectFactory objectFactory;

    public JibTaskSupport(Logger logger, JibExtension extension, ObjectFactory objectFactory) {
        this.logger = logger;
        this.extension = extension;
        this.objectFactory = objectFactory;
    }

    public List<FileCopyDetails> getFilesForLayer(CopySpec sourceDistributionFiles, List<FileCopyDetails> filesAlreadyAddedToImage, Function<LayerFilterFile, LayerFilterFile> filter) {
        List<FileCopyDetails> layerFiles = new ArrayList();

        //use the innards of a CopyAction to resolve the CopySpec into a set of FileCopyDetails _after_ renaming / processing
        //the other option 'eachFile {}' fires before renames/other processing so isn't useful
        //a more conservative but less performant approach would just be to depend on distTar and source the files out of the tar file
        //but we can avoid the entire tar step doing it this way
        CopyAction copyAction = (stream) -> {
            stream.process(details -> {
                LayerFilterFile result = filter.apply(new LayerFilterFile(details, filesAlreadyAddedToImage.contains(details)));
                if(result != null) {
                    layerFiles.add(result.getDetails());
                    filesAlreadyAddedToImage.add(result.getDetails());
                    logger.info("Added file: " + result.getDetails().getFile() + "  " + result.getPath());
                }
            });
            return WorkResults.didWork(true);
        };
        copyAction.execute(new CopySpecBackedCopyActionProcessingStream((CopySpecInternal)sourceDistributionFiles, JibGroovyHelper.getObjectInstantiator(objectFactory), objectFactory, JibGroovyHelper.getPlaceholderFileSystem(), true));
        return layerFiles;
    }

    public FileEntriesLayer getLayerForFiles(List<FileCopyDetails> files, String destinationPath, Boolean timestampFromHash) {
        if (destinationPath == null) {
            destinationPath = "/";
        }
        if(timestampFromHash == null) {
            timestampFromHash = false;
        }

        FileEntriesLayer.Builder layerBuilder = FileEntriesLayer.builder();
        for (FileCopyDetails file : files) {
            AbsoluteUnixPath targetPath = AbsoluteUnixPath.fromPath(Paths.get(destinationPath, file.getPath()));
            FilePermissions filePermission = Files.isExecutable(file.getFile().toPath()) ? FilePermissions.fromOctalString("755") : FilePermissions.DEFAULT_FILE_PERMISSIONS;
            Instant mtime = FileEntriesLayer.DEFAULT_MODIFICATION_TIME;

            //nginx uses file size and mtime to generate an etag, this is bad for reproducibility as we want etag to change based on file content
            //this isn't configurable, so to ensure the etag changes when the file changes AND the image is reproducible we set file mtime
            //based on a truncate sha1 hash of the file, so essentially etag will be set by the file content.. indirectly via the mtime.
            //https://stackoverflow.com/questions/55860358/why-the-nginx-etag-value-generated-by-last-modified-time-and-content-length
            if (timestampFromHash && file.getFile().isFile()) {

                //the process is:
                // 1. get hex encoded sha1 of file contents
                // 2. truncate hash to 10 chars
                // 3. decode hex (base 16), encode to base 10 (integer)
                // 4. use integer as mtime, and set into layer file
                long mtimeFromHash = Long.getLong(new BigInteger(JibGroovyHelper.calcSha1ForFile(file.getFile()).substring(0, 10), 16).toString(10));
                mtime = Instant.ofEpochMilli(mtimeFromHash);
            }
            layerBuilder.addEntry(file.getFile().toPath(), targetPath, filePermission, mtime);
        }
        return layerBuilder.build();
    }

    public RegistryImage getJibRegistryImage(String imageTag) throws InvalidImageReferenceException {
        ImageReference imageReference = ImageReference.parse(imageTag);
        CredentialRetrieverFactory credsFactory = CredentialRetrieverFactory.forImage(imageReference, msg -> logger.warn("Jib: " + msg.getMessage()));
        RegistryImage registryImage = RegistryImage.named(imageReference);
        if (imageTag.contains(".ecr.")) {
            //if ecr use aws login helper
            registryImage.addCredentialRetriever(credsFactory.dockerCredentialHelper("docker-credential-ecr-login"));

        } else if (System.getenv("DOCKER_HUB_ACCESS_TOKEN_VALUE") != null) {
            //otherwise assume docker hub if have creds
            registryImage.addCredential("pro1svc", System.getenv("DOCKER_HUB_ACCESS_TOKEN_VALUE"));
        }
        return registryImage;
    }

    public JibContainerBuilder getJibContainer(ImageInputs imageInputs, CopySpec sourceDistributionFiles) throws Exception {
        String baseContainer = imageInputs.getBaseContainer().get();
        List<String> entrypoint = imageInputs.getImageEntrypoint().getOrNull();
        List<LayerFilter> layerFilters = imageInputs.getLayerFilters().get();
        Boolean timestampFromHash = imageInputs.getTimestampFromHash().get();

        JibContainerBuilder containerBuilder;
        if (baseContainer.equals("scratch")) {
            containerBuilder = Jib.fromScratch();
        } else {
            RegistryImage registryImage = getJibRegistryImage(baseContainer);
            containerBuilder = Jib.from(registryImage);
        }

        //add the layers as configured
        if (layerFilters == null || layerFilters.isEmpty()) {
            logger.warn("No layers configured!");
        }

        List<FileCopyDetails> filesAlreadyAddedToImage = new ArrayList<>();
        for (LayerFilter filter : layerFilters) {
            logger.info("Processing image layer named: " + filter.getName());
            List<FileCopyDetails> filterFiles = getFilesForLayer(sourceDistributionFiles, filesAlreadyAddedToImage, filter.getFilter());
            FileEntriesLayer filterLayer = getLayerForFiles(filterFiles, filter.getDestinationPath(), timestampFromHash);
            containerBuilder
                    .addFileEntriesLayer(filterLayer);
        }

        if (entrypoint != null) {
            containerBuilder.setEntrypoint(entrypoint);
        }
        return containerBuilder;
    }

    public Containerizer getContainerizer(RegistryImage image) {
        return Containerizer.to(image)
                .setBaseImageLayersCache(extension.getBaseCachePath().get())
                .setApplicationLayersCache(extension.getAppCachePath().get());
    }

    public Containerizer getContainerizer(TarImage image) {
        return Containerizer.to(image)
                .setBaseImageLayersCache(extension.getBaseCachePath().get())
                .setApplicationLayersCache(extension.getAppCachePath().get());
    }

}
