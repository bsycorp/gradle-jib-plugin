package com.bsycorp.gradle.jib.tasks;

import com.bsycorp.gradle.jib.JibExtension;
import com.bsycorp.gradle.jib.JibTaskSupport;
import com.bsycorp.gradle.jib.models.NamedImageInputs;
import com.bsycorp.gradle.jib.models.TaskProperties;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.buildplan.ContainerBuildPlan;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.LayerObject;
import com.google.cloud.tools.jib.cache.Cache;
import com.google.cloud.tools.jib.cache.CachedLayer;
import com.google.common.collect.ImmutableList;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.TaskAction;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class BuildDockerImageTask extends Exec implements TaskProperties, NamedImageInputs {

    protected JibTaskSupport taskSupport;
    protected JibExtension extension;
    protected Logger logger;
    private Path rootProjectPath;
    private File projectBuildDir;

    public BuildDockerImageTask() {
        super();
        setupProperties(getProject());
        setupNamedImageInputs(getProject(), extension);

        dependsOn("buildImageLayers");

        this.rootProjectPath = getProject().getRootProject().getProjectDir().toPath();
        this.projectBuildDir = getProject().getBuildDir();

    }

    @Override
    @TaskAction
    public void exec() {
        try {
            //need to deconstruct distribution at config time and pull out requisite pieces to reconstruct later?
            JibContainerBuilder containerBuilder = taskSupport.getJibContainer(this, getSourceCopySpec().get());
            ContainerBuildPlan containerBuildPlan = containerBuilder.toContainerBuildPlan();
            Cache jibCache = Cache.withDirectory(extension.getAppCachePath().get());
            String dockerBinaryPath = extension.getDockerBinaryPath().get();

            List<String> layerTars = new ArrayList();
            for (LayerObject layerObject : containerBuildPlan.getLayers()) {
                if (!(layerObject instanceof FileEntriesLayer)) {
                    throw new RuntimeException("Unsupported layer type: " + layerObject.getClass().getName());
                }

                FileEntriesLayer layer = (FileEntriesLayer) layerObject;
                if (layer.getEntries().size() > 0) {
                    //guard against missing / unbuilt layers, should be built already by buildImageLayers
                    Optional<CachedLayer> cacheForLayer = jibCache.retrieve(ImmutableList.copyOf(layer.getEntries()));
                    if (cacheForLayer.isEmpty()) {
                        throw new Exception("Cached layer is missing for layer " + layer.getName());
                    }
                    Path cacheTarForLayer = extension.getAppCachePath().get().resolve("layers/" + cacheForLayer.get().getDigest().getHash() + "/" + cacheForLayer.get().getDiffId().getHash());
                    layerTars.add(rootProjectPath.relativize(cacheTarForLayer).toString());
                }
            }

            //This is a bit hacky but functional
            StringBuilder dockerFileContent = new StringBuilder();
            dockerFileContent.append("FROM " + getBaseContainer().get() + "\n");
            dockerFileContent.append("WORKDIR /app" + "\n"); //TODO make configurable
            StringBuilder dockerIgnoreFileContent = new StringBuilder();
            dockerIgnoreFileContent.append("# Ignore everything" + "\n");
            dockerIgnoreFileContent.append("**" + "\n");
            dockerIgnoreFileContent.append("# Allow required layer tars" + "\n");
            for (String layerTar : layerTars) {
                dockerFileContent.append("ADD " + layerTar + " /" + "\n");
                dockerIgnoreFileContent.append("!" + layerTar + "\n");
            }
            if (containerBuildPlan.getEntrypoint() != null) {
                dockerFileContent.append("ENTRYPOINT [" + containerBuildPlan.getEntrypoint().stream().map( i -> "\"" + i + "\"").collect(Collectors.joining(", ")) + "]");
            }
            //TODO make this configurable
            Path dockerFile = Paths.get(projectBuildDir.getAbsolutePath(), "dockerfile/Dockerfile");
            Path dockerIgnoreFile = Paths.get(projectBuildDir.getAbsolutePath(), "dockerfile/Dockerfile.dockerignore");
            Files.createDirectories(dockerFile.getParent());

            Files.writeString(dockerFile, dockerFileContent.toString());
            Files.writeString(dockerIgnoreFile, dockerIgnoreFileContent.toString());

            environment("DOCKER_BUILDKIT", "1");
            commandLine(dockerBinaryPath, "build", "-f", dockerFile, "-t", getImageTag().get(), rootProjectPath.toAbsolutePath());

            //call actual exec task
            super.exec();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void logger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public JibExtension extension() {
        return extension;
    }

    @Override
    public void extension(JibExtension extension) {
        this.extension = extension;
    }

    @Override
    public void taskSupport(JibTaskSupport taskSupport) {
        this.taskSupport = taskSupport;
    }

}
