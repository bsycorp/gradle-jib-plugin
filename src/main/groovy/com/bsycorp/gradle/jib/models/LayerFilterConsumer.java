package com.bsycorp.gradle.jib.models;

import org.gradle.api.Action;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class LayerFilterConsumer implements Action<LayerFilterFile>, Serializable {

    @SuppressWarnings("unused")
    private static void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }
}
