# Overview

This is a Gradle plugin to help efficiently build and push OCI-compliant container images via `jib-core` (https://github.com/GoogleContainerTools/jib) with good support for multi-module projects. 

## Why not jib-gradle-plugin from Jib?

This plugin isn't specifically focused around building Java apps/images, it uses a Distribution (or actually CopySpec..) as the source of the files to consider for the image, making no assumptions about what those files are (Java app or otherwise..). 
The `jib-gradle-plugin` really wants to the image you are building to be a Java app, there are a few workarounds/hacks to avoid the Java-specific behaviour but they shouldn't be necessary. 
`jib-gradle-plugin` also isn't built with multi-module projects in mind, there is no coordination between projects which can cause problems like pulling base images many times in parallel, and the registry throttling/errors that come with it.

`jib-core` does most things we need to efficiently build and push images, this is effectively just a wrapper around it that aims to give the best Gradle experience from Jib.

# Design

The plugin aims to take a Gradle Distribution (https://docs.gradle.org/current/userguide/distribution_plugin.html or Application) and feed it into Jib with an ability to configure the files into layers along the way. 

## Tasks

pullBaseImage: Normally used indirectly, used to pull the given base image from the registry into the `base-cache`

buildImageLayers: Normally used indirectly, used to take the base image and add layer(s) from the Distribution into the `app-cache`

buildDockerImage: Generate a Dockerfile and run `docker build ..` to incrementally build a docker image fast

pushImage: Push the image to the given registry (including pulling and building if required)

## Optimisations

- Use a Gradle Build Service to ensure sequential access to base images. 
- Use the Distribution _source_ files, as input to layer generation not the target, skipping the distribution tar step.
- Generate a Dockerfile and call `docker build ..` for fast incremental docker image builds.

# Getting started

add deps/plugin

## Add plugin

## Configure

Project-wide configuration looks like:

```
jib {
    //required
    imageTag = 'some-registry/image-name:version' or { -> "${someDynamicValue}" }
    //if the sha256 digest is not included a remote registry will occur for each execution 
    baseContainer = 'some-registry/image-name:version@sha256:digest' or 'scratch'
    imageEntrypoint = ["/entrypoint.sh"]
    
    //where most of the complexity will be, defining the layers in order
    layerFilters = [
        // layerFilter(String layerName, String destinationPath, closure taking FileCopyDetails and returning files to return)
        layerFilter('all', '/', { details -> })
    ]
    
    //optional
    sourceDistributionName = 'main' //normally 'main', can be different if desired
    
}
```

Other than image tag and source image references, the main configuration is in the ordered list of layerFilters, this is where the layers are defined as a closure defining what files to _include_ in the layer. A number of layers can be defined and they are built in order, this gives the ability to possibly include all 3rd part libraries or common files in an early layer and the project specific files or configuration in a later layer, promoting image layer re-use across components.

As the Closure accepts org.gradle.api.file.FileCopyDetails it is possible to rename or change the path of files as they apply to the layer if that is desirable, you can also do this in the Distribution itself. To exclude a file the Closure should return `null`.

Multi layer, multi module with dynamic values example:

```
jib {
    imageTag = { -> "some-registry/${project.name}:${appVersion}" }
    baseContainer = 'some-registry/base-image-name:1.0'
    imageEntrypoint = ["/bin/service"]
    layerFilters = [
        //exclude all files not in 'lib' as a common-base, don't include our 1st party jars in base
        layerFilter('common-base', '/', { if (!(it.path.startsWith('lib') && !it.name.startsWith('internal-'))) it.exclude() }),
        
        //exclude all files already in another layer, leaving files like /bin/ in a second layer and rename bin files to make name consistent for entrypoint
        layerFilter('app', '/', { ->
            if (it.alreadyAddedToImage) {
                it.exclude()
            }
        })
    ]
}

//name jar with a prefix so we can differentiate later when splitting into layers
tasks.named("jar").configure {
    archiveBaseName = "internal-${project.name}"
}
```
