package com.bsycorp.gradle.jib.tasks;

import com.bsycorp.gradle.jib.JibExtension;
import com.bsycorp.gradle.jib.JibTaskSupport;
import com.bsycorp.gradle.jib.models.TaskProperties;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Internal;
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

    protected JibExtension extension;

    public BaseTask() {
        setupProperties(getProject());
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


}
