package com.bsycorp.gradle.jib

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Paths

import static com.bsycorp.gradle.jib.FileUtil.countFiles

class BuildImageLayersTaskTest {

    @TempDir
    public File testProjectDir;
    public File settingsFile;
    public File buildFile;

    @BeforeEach
    public void setup() {
        settingsFile = new File(testProjectDir, "settings.gradle");
        settingsFile.text = "rootProject.name = 'test-case'";
        buildFile = new File(testProjectDir, "build.gradle");
    }

    @Test
    void shouldConfigureTaskSuccessfully() {
        buildFile.text = """
plugins { id 'distribution' }
plugins { id 'io.github.bsycorp.jib' }
jib {
    imageTag = 'test-image'
    baseContainer = 'bitnami/minideb:bullseye'
}
"""
        GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("buildImageLayers", "pushImage", "buildDockerImage", "--dry-run", "--stacktrace", "--configuration-cache")
                .withPluginClasspath()
                .build();
        //no exp means pass

        //run again to test config cache
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("buildImageLayers", "pushImage", "buildDockerImage", "--dry-run", "--stacktrace", "--configuration-cache")
                .withPluginClasspath()
                .build();
        Assertions.assertTrue(result.getOutput().contains("configuration cache"));
    }

    @Test
    void shouldFailWithMissingDistribution() {
        buildFile.text = """
plugins { id 'io.github.bsycorp.jib' }
plugins { id 'application' }
application {
    mainClass = 'test.Main'
}
jib {
    imageTag = 'test-image'
    baseContainer = 'scratch'
    sourceDistributionName = 'wrongname'
    layerFilters = [
        layerFilter('all', '/', { details -> details })
    ]
}
"""
        GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("buildImageLayers", "-i", "--stacktrace")
                .withPluginClasspath()
                .buildAndFail();
    }

    @Test
    void shouldBuildLayerFromScratchSuccessfully() {
        File mainClass = new File(testProjectDir, "src/main/java/test/Main.java");
        mainClass.getParentFile().mkdirs();
        mainClass.text = """
package test;
"""
        buildFile.text = """
plugins { id 'io.github.bsycorp.jib' }
plugins { id 'application' }
application {
    mainClass = 'test.Main'
}
jib {
    imageTag = 'test-image'
    imageOutputTarFile = new File("diff-image-path/image.tar", buildDir)
    baseContainer = 'scratch'
    layerFilters = [
        layerFilter('jars', '/', { details -> if (!details.name.endsWith(".jar")) details.exclude() }),
        layerFilter('scripts', '/', { details -> if(details.alreadyAddedToImage) details.exclude() })
    ]
}
"""
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("buildImageLayers", "-i", "--stacktrace")
                .withPluginClasspath()
                .build();
        Assertions.assertEquals(TaskOutcome.SKIPPED, result.tasks.find { it.path == ":pullBaseImage" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":jar" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":startScripts" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":buildImageLayers" }.outcome)

        //assert built output
        //from scratch so 0 base cache items
        Assertions.assertEquals(0, countFiles(Paths.get("${testProjectDir}/.gradle/jib-base-cache/")))
        //2 layers should exist
        Assertions.assertEquals(2, countFiles(Paths.get("${testProjectDir}/.gradle/jib-app-cache/layers/")))
        //and image tar should be there too
        Assertions.assertTrue(new File("${testProjectDir}/build/diff-image-path/image.tar").exists())
        Assertions.assertTrue(new File("${testProjectDir}/build/diff-image-path/image.image-id").exists())
    }

    @Test
    void shouldBuildMultiLayerFromScratchSuccessfully() {
        File mainClass = new File(testProjectDir, "src/main/java/test/Main.java");
        mainClass.getParentFile().mkdirs();
        mainClass.text = """
package test;
"""
        buildFile.text = """
plugins { id 'io.github.bsycorp.jib' }
plugins { id 'application' }
application {
    mainClass = 'test.Main'
}
jib {
    imageTag = 'test-image'
    baseContainer = 'scratch'
    layerFilters = [
        layerFilter('jars', '/', { details -> if (!details.name.endsWith(".jar")) details.exclude() }),
        layerFilter('scripts', '/', { details -> if (details.alreadyAddedToImage) details.exclude() })
    ]
}
"""
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("buildImageLayers", "-i", "--stacktrace")
                .withPluginClasspath()
                .build();
        Assertions.assertEquals(TaskOutcome.SKIPPED, result.tasks.find { it.path == ":pullBaseImage" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":jar" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":startScripts" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":buildImageLayers" }.outcome)

        //assert built output
        //from scratch so 0 base cache items
        Assertions.assertEquals(0, countFiles(Paths.get("${testProjectDir}/.gradle/jib-base-cache/")))
        //2 layers should exist
        Assertions.assertEquals(2, countFiles(Paths.get("${testProjectDir}/.gradle/jib-app-cache/layers/")))
        //and image tar should be there too
        Assertions.assertTrue(new File("${testProjectDir}/build/image-tar/image.tar").exists())
    }

    @Test
    void shouldBuildLayerFromImageSuccessfully() {
        File mainClass = new File(testProjectDir, "src/main/java/test/Main.java");
        mainClass.getParentFile().mkdirs();
        mainClass.text = """
package test;
"""
        buildFile.text = """
plugins { id 'application' }
plugins { id 'io.github.bsycorp.jib' }
application {
    mainClass = 'test.Main'
}
jib {
    imageTag = provider { 'test-image' }
    baseContainer = 'alpine:latest'
    layerFilters = [
        layerFilter('all', '/', { })
    ]
}
"""
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("buildImageLayers", "-i", "--stacktrace")
                .withPluginClasspath()
                .build();
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":pullBaseImage" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":jar" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":startScripts" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":buildImageLayers" }.outcome)

        //assert built output
        //from scratch so 0 base cache items
        Assertions.assertTrue(countFiles(Paths.get("${testProjectDir}/.gradle/jib-base-cache/")) > 0)
        //2 layers should exist
        Assertions.assertEquals(1, countFiles(Paths.get("${testProjectDir}/.gradle/jib-app-cache/layers/")))
        //and image tar should be there too
        Assertions.assertTrue(new File("${testProjectDir}/build/image-tar/image.tar").exists())
    }
}


