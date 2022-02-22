package com.bsycorp.gradle.jib

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BuildDockerImageTaskTest {

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
    void shouldRunSuccessfully() {
        buildFile.text = """
plugins { id 'io.github.bsycorp.jib' }
plugins { id 'application' }
application {
    mainClass = 'test.Main'
}
jib {
    imageTag = provider { -> 'test-image:' + new File("image-tar/image.image-id", buildDir).text }
    imageEntrypoint = ['/app/bin/test-case']
    baseContainer = 'scratch'
    layerFilters = [
        layerFilter('all', '/', { details -> details })
    ]
    dockerBinaryPath = 'true' //override docker path so tests dont actually call docker, just noop
}
"""
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("buildDockerImage", "-i", "--stacktrace")
                .withPluginClasspath()
                .build();

        Assertions.assertEquals(TaskOutcome.SKIPPED, result.tasks.find { it.path == ":pullBaseImage" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":jar" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":startScripts" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":buildImageLayers" }.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.tasks.find { it.path == ":buildDockerImage" }.outcome)

        //assert built output
        Assertions.assertEquals(2, FileUtils.listFiles(new File("${testProjectDir}/build/dockerfile/"), null, true).size());
        Assertions.assertEquals("""
FROM scratch
WORKDIR /app
ADD .gradle/jib-app-cache/layers/5ad64458460e882544104696769164dc2b61f1136ace3b21d9ae6be25cb2bfa8/b08623b0d7e1dd41da18d54e4a1e30386ed932a587449b29c9adb757b12b9abe /
ENTRYPOINT ["/app/bin/test-case"]
""".trim(),
            new File("${testProjectDir}/build/dockerfile/Dockerfile").text.trim()
        )
        Assertions.assertEquals("""
# Ignore everything
**
# Allow required layer tars
!.gradle/jib-app-cache/layers/5ad64458460e882544104696769164dc2b61f1136ace3b21d9ae6be25cb2bfa8/b08623b0d7e1dd41da18d54e4a1e30386ed932a587449b29c9adb757b12b9abe
""".trim(),
            new File("${testProjectDir}/build/dockerfile/Dockerfile.dockerignore").text.trim()
        )
    }
}