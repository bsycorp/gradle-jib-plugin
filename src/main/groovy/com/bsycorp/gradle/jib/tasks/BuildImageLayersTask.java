package com.bsycorp.gradle.jib.tasks;

import com.bsycorp.gradle.jib.models.BuiltImageInputs;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.TarImage;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class BuildImageLayersTask extends BaseTask implements BuiltImageInputs {

    @OutputFile
    public abstract Property<File> getImageOutputTarFile();

    @OutputFile
    public abstract Property<File> getImageOutputImageIdFile();

    public BuildImageLayersTask() {
        super();
        setupBuiltImageInputs(getProject(), extension);

        dependsOn("pullBaseImage");
        getProject().getPlugins().withType(ApplicationPlugin.class, newPlugin -> {
            //otherwise the distribution won't contain the /bin/blah.bat etc files
            dependsOn("startScripts");
        });
        getProject().getPlugins().withType(JavaPlugin.class, newPlugin -> {
            dependsOn("jar");
        });
        //Might be a cleaner way to do this? we basically want to reach into the copyspec and add an input for the destination paths
        //If copyspec is an @Input maybe not needed
//        for (CopySpecInternal copies : ((CopySpecInternal)sourceDistribution).getChildren()) {
//            CopySpecResolver resolver = copies.buildRootResolver();
//            getInputs().files(resolver.getAllSource()).ignoreEmptyDirectories().withPathSensitivity(PathSensitivity.RELATIVE);
//            getInputs().property("destPath", resolver.getDestPath().getPathString());
//        }
        getImageOutputTarFile().set(extension.getImageOutputTarFile());
        getImageOutputImageIdFile().set(new File(extension.getImageOutputTarFile().get().getParentFile(), "image.image-id"));
    }

    @TaskAction
    public void fire() throws Exception {

        logger.info("Building image layers..");
        Containerizer tarContainer = taskSupport.getContainerizer(TarImage.at(extension.getImageOutputTarFile().get().toPath()).named("image"))
                .setOfflineMode(true); //base pull via pullImageBase so no need for online

        JibContainer builtContainer = taskSupport.getJibContainer(this, getSourceCopySpec().get())
                .containerize(tarContainer);
        //write out image id so can be used by other tasks
        Path imageIdFile = getImageOutputImageIdFile().get().toPath();
        Files.createDirectories(imageIdFile.getParent());
        Files.writeString(imageIdFile, builtContainer.getImageId().getHash());

    }
}
