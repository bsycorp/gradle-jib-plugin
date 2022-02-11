package com.bsycorp.gradle.jib.tasks;

import com.bsycorp.gradle.jib.NamedLockProvider;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public abstract class PullBaseImageTask extends BaseTask {

    public PullBaseImageTask() {
        super();

        Provider<NamedLockProvider> lockProvider = getProject().getGradle().getSharedServices().registerIfAbsent("namedLocks", NamedLockProvider.class, spec -> {        });
        usesService(lockProvider);
        getLockProvider().set(lockProvider.get());

        //don't need to pull base image if base is scratch
        onlyIf(task -> !getBaseContainer().get().equals("scratch"));
    }

    @TaskAction
    public void fire() throws Exception {
        super.fire();

        //we need to put a lock around jib as its got some problem with concurrent pulls from docker hub, very weird but fails with UNAUTHORIZED if concurrency is too high
        //https://github.com/GoogleContainerTools/jib/issues/2007
        String containerBase = getBaseContainer().get();
        Lock baseContainerLock = getLockProvider().get().getLock("base-image-" + containerBase);
        if (baseContainerLock.tryLock(extension.getImagePullTimeout().get().toSeconds(), TimeUnit.SECONDS)) {
            try {
                RegistryImage registryImage = taskSupport.getJibRegistryImage(containerBase);
                JibContainerBuilder containerBuilder = Jib.from(registryImage);

                Containerizer tarContainer = Containerizer
                    .to(TarImage.at(Paths.get("/dev/null")).named("base"))
                    .setBaseImageLayersCache(extension.getBaseCachePath().get());

                containerBuilder
                    .containerize(tarContainer);
            } finally {
                baseContainerLock.unlock();
            }
        } else {
            throw new Exception("Timed out waiting for base image to be available: " + containerBase);
        }
    }

}
