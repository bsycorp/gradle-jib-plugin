package com.bsycorp.gradle.jib.models;

import com.bsycorp.gradle.jib.JibExtension;
import com.bsycorp.gradle.jib.JibGroovyHelper;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.copy.DefaultCopySpec;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

public interface BuiltImageInputs extends BaseImageInputs {

    @Optional
    @Input
    ListProperty<String> getImageEntrypoint();

    @InputFiles
    Property<FileCollection> getSourceDistributionFiles();

    @Input
    ListProperty<LayerFilter> getLayerFilters();

    @Optional
    @Input
    Property<Boolean> getTimestampFromHash();

    @Internal
    Property<DefaultCopySpec> getSourceCopySpec();

    public default void setupBuiltImageInputs(Project project, JibExtension extension) {
        setupBaseImageInputs(project, extension);
        getSourceCopySpec().set(extension.getSourceCopySpec());
        //TODO enhance with addChildSpecListener as per AbstractCopyTask
        getSourceDistributionFiles().set(getSourceCopySpec().map(spec -> JibGroovyHelper.getFilesForCopySpec(spec)));
        getLayerFilters().set(extension.getLayerFilters());
        getTimestampFromHash().set(extension.getTimestampFromHash());
        getImageEntrypoint().set(extension.getImageEntrypoint());
    }

}
