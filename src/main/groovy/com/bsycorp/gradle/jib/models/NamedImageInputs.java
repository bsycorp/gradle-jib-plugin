package com.bsycorp.gradle.jib.models;

import com.bsycorp.gradle.jib.JibExtension;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

public interface NamedImageInputs extends BuiltImageInputs{

    @Input
    public abstract Property<String> getImageTag();

    default void setupNamedImageInputs(Project project, JibExtension jibExtension) {
        setupBuiltImageInputs(project, jibExtension);
        getImageTag().set(jibExtension.getImageTag());
    }

}
