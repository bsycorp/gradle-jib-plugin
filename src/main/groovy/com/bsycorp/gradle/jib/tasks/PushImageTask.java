package com.bsycorp.gradle.jib.tasks;

import com.bsycorp.gradle.jib.models.BuiltImageInputs;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.event.events.TimerEvent;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

public abstract class PushImageTask extends BaseTask implements BuiltImageInputs {

    @Input
    public abstract Property<String> getImageTag();

    @Internal
    private String projectName;

    public PushImageTask() {
        super();
        setupBuiltImageInputs(getProject(), extension);

        dependsOn("buildImageLayers");
        projectName = getProject().getName();
        getImageTag().set(extension().getImageTag());
    }

    public String getProjectName() {
        return projectName;
    }

    @TaskAction
    public void fire() throws Exception {
        JibContainerBuilder containerBuilder = taskSupport.getJibContainer(this, getSourceCopySpec().get());
        Containerizer output = taskSupport.getContainerizer(taskSupport.getJibRegistryImage(getImageTag().get()));
        if (extension.getLogProgress().get()) {
            output.addEventHandler(TimerEvent.class, timeEvent -> {
                logger.warn(projectName + ": " + timeEvent.getDescription() + " " + timeEvent.getState() + " after " + timeEvent.getElapsed().toMillis() + "ms");
            });
            output.addEventHandler(LogEvent.class, logEvent -> {
                if (logEvent.getLevel().toString() == "PROGRESS") {
                    logger.warn(projectName + ": " + logEvent.getMessage());
                }
            });
        }

        containerBuilder.containerize(output);
    }
}
