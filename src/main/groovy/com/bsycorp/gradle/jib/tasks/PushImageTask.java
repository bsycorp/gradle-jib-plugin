package com.bsycorp.gradle.jib.tasks;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.event.events.TimerEvent;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

public abstract class PushImageTask extends BaseTask {

    @Input
    public Property<String> imageTag;

    @Internal
    private String projectName;

    public PushImageTask() {
        super();
        dependsOn("buildImageLayers");
        projectName = getProject().getName();
    }

    public String getProjectName() {
        return projectName;
    }

    @TaskAction
    public void fire() throws Exception {
        super.fire();

        JibContainerBuilder containerBuilder = taskSupport.getJibContainer(this, sourceDistribution);
        Containerizer output = Containerizer
                .to(taskSupport.getJibRegistryImage(imageTag.get()));
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
