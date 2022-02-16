package com.bsycorp.gradle.jib;

import com.bsycorp.gradle.jib.tasks.BuildDockerImageTask;
import com.bsycorp.gradle.jib.tasks.BuildImageLayersTask;
import com.bsycorp.gradle.jib.tasks.PullBaseImageTask;
import com.bsycorp.gradle.jib.tasks.PushImageTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.Tar;
import org.gradle.api.tasks.bundling.Zip;

public class JibPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        JibExtension extension = project.getExtensions().create("jib", JibExtension.class, project);
        Provider<NamedLockProvider> lockProvider = project.getGradle().getSharedServices().registerIfAbsent("namedLocks", NamedLockProvider.class, spec -> {        });
        project.getTasks().register("buildDockerImage", BuildDockerImageTask.class);
        project.getTasks().register("buildImageLayers", BuildImageLayersTask.class);
        project.getTasks().register("pullBaseImage", PullBaseImageTask.class, task -> {
            task.getLockProvider().set(lockProvider);
        });
        project.getTasks().register("pushImage", PushImageTask.class);

        if (extension.getEnsureReproducible().get()) {
            project.getTasks().withType(Zip.class).configureEach(task -> {
                task.setPreserveFileTimestamps(false);
                task.setReproducibleFileOrder(true);
            });
            project.getTasks().withType(Jar.class).configureEach(task -> {
                task.setPreserveFileTimestamps(false);
                task.setReproducibleFileOrder(true);
            });
            project.getTasks().withType(Tar.class).configureEach(task -> {
                task.setPreserveFileTimestamps(false);
                task.setReproducibleFileOrder(true);
            });
        }
    }
}
