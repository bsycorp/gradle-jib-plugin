package com.bsycorp.gradle.jib.tasks;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.TarImage;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskAction;

public abstract class BuildImageLayersTask extends BaseTask {

    public BuildImageLayersTask() {
        super();
        dependsOn("pullBaseImage");

        getProject().getPlugins().withType(ApplicationPlugin.class, newPlugin ->  {
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
        getOutputs().file(imageOutputTarPath);
    }

    @Override
    @TaskAction
    public void fire() throws Exception {
        super.fire();

        logger.info("Building image layers..");
        Containerizer tarContainer = Containerizer
            .to(TarImage.at(imageOutputTarPath.toPath()).named("image"))
            .setBaseImageLayersCache(extension.getBaseCachePath().get())
            .setApplicationLayersCache(extension.getAppCachePath().get())
            .setOfflineMode(true); //base pull via pullImageBase so no need for online

        taskSupport.getJibContainer(this, sourceDistribution)
            .containerize(tarContainer);
    }

}
