package com.bsycorp.gradle.jib.models;

import com.bsycorp.gradle.jib.JibExtension;
import com.bsycorp.gradle.jib.JibTaskSupport;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Internal;

public interface TaskProperties {

    void logger(Logger logger);

    void extension(JibExtension extension);

    void taskSupport(JibTaskSupport taskSupport);

    @Internal
    JibExtension extension();

    default void setupProperties(Project project) {
        logger(project.getLogger());
        extension(project.getExtensions().findByType(JibExtension.class));
        taskSupport(new JibTaskSupport(project.getLogger(), extension(), project.getObjects()));
    }
}
