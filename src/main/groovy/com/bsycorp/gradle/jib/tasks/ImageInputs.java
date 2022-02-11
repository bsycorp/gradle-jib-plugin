package com.bsycorp.gradle.jib.tasks;

import com.bsycorp.gradle.jib.LayerFilter;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public interface ImageInputs {

    @Input
    Property<String> getImageTag();

    @Optional
    @Input
    ListProperty<String> getImageEntrypoint();

    @Input
    Property<String> getSourceDistributionName();

    @Input
    ListProperty<LayerFilter> getLayerFilters();

    @Input
    Property<String> getBaseContainer();

    @Optional
    @Input
    Property<Boolean> getTimestampFromHash();

}
