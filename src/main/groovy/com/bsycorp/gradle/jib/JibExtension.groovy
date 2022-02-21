package com.bsycorp.gradle.jib

import com.bsycorp.gradle.jib.tasks.ImageInputs
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.function.Function

public class JibExtension implements ImageInputs {

    public Property<Path> baseCachePath;
    public Property<Path> appCachePath;
    public Property<String> sourceDistributionName;
    public ListProperty<LayerFilter> layerFilters;
    public Property<String> baseContainer;
    public Property<Boolean> timestampFromHash;
    public Property<String> imageTag;
    public ListProperty<String> imageEntrypoint;
    public Property<Duration> imagePullTimeout;
    public Property<File> imageOutputTarFile;
    public Property<String> dockerBinaryPath;
    public Property<Boolean> ensureReproducible;
    public Property<Boolean> logProgress;

    public JibExtension(Project project) {
        baseCachePath = project.getObjects().property(Path.class)
            .convention(Paths.get(project.getRootProject().getProjectDir().getAbsolutePath(), "/.gradle/jib-base-cache"));
        appCachePath = project.getObjects().property(Path.class)
            .convention(Paths.get(project.getRootProject().getProjectDir().getAbsolutePath(), "/.gradle/jib-app-cache"));
        sourceDistributionName = project.getObjects().property(String.class).convention("main");
        layerFilters = project.getObjects().listProperty(LayerFilter.class).convention(Collections.emptyList());
        timestampFromHash = project.getObjects().property(Boolean.class).convention(false);
        baseContainer = project.getObjects().property(String.class);
        imageTag = project.getObjects().property(String.class);
        imageEntrypoint = project.getObjects().listProperty(String.class);
        imagePullTimeout = project.getObjects().property(Duration.class).convention(Duration.ofMinutes(2));
        imageOutputTarFile = project.getObjects().property(File.class).convention(new File(project.getBuildDir(), "image-tar/image.tar"));
        dockerBinaryPath = project.getObjects().property(String.class).convention("docker");
        ensureReproducible = project.getObjects().property(Boolean.class).convention(true);
        logProgress = project.getObjects().property(Boolean.class).convention(false);
    }

    public Property<Path> getBaseCachePath() {
        return baseCachePath;
    }

    public Property<Path> getAppCachePath() {
        return appCachePath;
    }

    @Override
    public Property<String> getSourceDistributionName() {
        return sourceDistributionName;
    }

    @Override
    public ListProperty<LayerFilter> getLayerFilters() {
        return layerFilters;
    }

    @Override
    public Property<String> getBaseContainer() {
        return baseContainer;
    }

    @Override
    public Property<Boolean> getTimestampFromHash() {
        return timestampFromHash;
    }

    Property<Duration> getImagePullTimeout() {
        return imagePullTimeout
    }

    Property<String> getImageTag() {
        return imageTag
    }

    ListProperty<String> getImageEntrypoint() {
        return imageEntrypoint
    }

    Property<File> getImageOutputTarFile() {
        return imageOutputTarFile
    }

    Property<String> getDockerBinaryPath() {
        return dockerBinaryPath
    }

    Property<Boolean> getEnsureReproducible() {
        return ensureReproducible
    }

    Property<Boolean> getLogProgress() {
        return logProgress
    }

    //Helper method for defining layers
    public LayerFilter layerFilter(String name, String destinationPath, Closure filter) {
        //gotta dehydrate so it can serialise
        return new LayerFilter(name, destinationPath, filter.dehydrate() as Function<LayerFilterFile, LayerFilterFile>)
    }
}
