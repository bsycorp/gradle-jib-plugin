package com.bsycorp.gradle.jib

import com.bsycorp.gradle.jib.models.LayerFilter
import com.bsycorp.gradle.jib.models.LayerFilterConsumer
import org.gradle.api.Project
import org.gradle.api.distribution.Distribution
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

public class JibExtension {

    public Property<Path> baseCachePath;
    public Property<Path> appCachePath;
    public Property<String> sourceDistributionName;
    public Property<String> sourceCopySpec;
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
        //expect 'main' distribution by default, but can be changed
        sourceDistributionName = project.getObjects().property(String.class).convention("main");
        //don't expect CopySpec to be set manually, get it from distribution, but can be set and not use distribution at all..
        sourceCopySpec = project.getObjects().property(DefaultCopySpec.class).convention(
                sourceDistributionName.map((distributionName) -> {
                    DistributionContainer container = project.getExtensions().findByType(DistributionContainer.class);
                    Distribution distro = container.findByName(distributionName);
                    return distro?.getContents();
                })
        )
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

    public Property<String> getSourceDistributionName() {
        return sourceDistributionName;
    }

    public Property<DefaultCopySpec> getSourceCopySpec() {
        return sourceCopySpec;
    }

    public ListProperty<LayerFilter> getLayerFilters() {
        return layerFilters;
    }

    public Property<String> getBaseContainer() {
        return baseContainer;
    }

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
    public static LayerFilter layerFilter(String name, String destinationPath, Object filter) {
        if (filter instanceof Closure) {
            return new LayerFilter(name, destinationPath, (Closure) filter);

        } else if (filter instanceof LayerFilterConsumer) {
            return new LayerFilter(name, destinationPath, (LayerFilterConsumer) filter);

        } else {
            throw new RuntimeException("Unsupported type of filter: " + filter.getClass().getName())
        }
    }

}
