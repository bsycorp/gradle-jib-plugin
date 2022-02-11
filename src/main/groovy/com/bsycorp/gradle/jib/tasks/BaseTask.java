package com.bsycorp.gradle.jib.tasks;

import com.bsycorp.gradle.jib.JibExtension;
import com.bsycorp.gradle.jib.JibTaskSupport;
import com.bsycorp.gradle.jib.NamedLockProvider;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.CopySpec;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Internal;
import java.io.File;
import javax.inject.Inject;

public abstract class BaseTask extends DefaultTask implements TaskProperties {
    @Inject
    protected ObjectFactory objectFactory;

    @Inject
    protected ProviderFactory providerFactory;

    @Internal
    protected Logger logger;

    @Internal
    protected JibTaskSupport taskSupport;

    @Internal
    protected File imageOutputTarPath;

    @Internal
    protected CopySpec sourceDistribution;

    @Internal
    protected abstract Property<NamedLockProvider> getLockProvider();

    protected JibExtension extension;

    public BaseTask() {
        //TODO make this configurable
        imageOutputTarPath = getProject().getBuildDir().toPath().resolve("image-tar/image.tar").toFile();
        setupProperties(getProject());
    }

    public void fire() throws Exception {
        if (sourceDistribution == null) {
            throw new RuntimeException("Source distribution named '" + getSourceDistributionName().get() + "' wasn't found");
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void logger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public JibExtension extension() {
        return extension;
    }

    @Override
    public void extension(JibExtension extension) {
        this.extension = extension;
    }

    public JibTaskSupport getTaskSupport() {
        return taskSupport;
    }

    @Override
    public void taskSupport(JibTaskSupport taskSupport) {
        this.taskSupport = taskSupport;
    }

    public CopySpec getSourceDistribution() {
        return sourceDistribution;
    }

    @Override
    public void sourceDistribution(CopySpec sourceDistribution) {
        this.sourceDistribution = sourceDistribution;
    }

    public File getImageOutputTarPath() {
        return imageOutputTarPath;
    }
}
