package com.bsycorp.gradle.jib


import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.nativeintegration.filesystem.FileSystem
import org.gradle.internal.reflect.Instantiator

class JibGroovyHelper {

    static Instantiator getObjectInstantiator(ObjectFactory objectFactory) {
        return objectFactory.instantiator
    }

    static FileCollection getFilesForCopySpec(CopySpec spec) {
        return (spec as CopySpecInternal).buildRootResolver().allSource
    }

    static FileSystem getPlaceholderFileSystem() {
        return ({ } as FileSystem)
    }

    static String calcSha1ForFile(File file) {
        def digest = java.security.MessageDigest.getInstance("SHA-1")
        file.withInputStream { stream ->
            stream.eachByte 4096, { buffer, length ->
                digest.update( buffer, 0, length )
            }
        }
        return digest.digest().encodeHex() as String
    }
}
