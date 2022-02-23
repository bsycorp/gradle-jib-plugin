package com.bsycorp.gradle.jib.models;

import com.bsycorp.gradle.jib.JibExtension;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

public interface BaseImageInputs {

    @Input
    Property<String> getBaseContainer();

    public default void setupBaseImageInputs(Project project, JibExtension extension) {
        getBaseContainer().set(extension.getBaseContainer());
    }

}
