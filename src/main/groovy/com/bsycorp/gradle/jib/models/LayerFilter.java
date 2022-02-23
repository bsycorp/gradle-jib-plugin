package com.bsycorp.gradle.jib.models;

import groovy.lang.Closure;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class LayerFilter implements Serializable {

    private static final long serialVersionUID = 1948900767703009070L;

    public String name;

    public String destinationPath;

    //have to store closure vs other lambda/method reference separately so they can be serialized differently, kinda annoying.
    //otherwise ValueSnapshotting and/or Config Cache blows up with NotSerialiazableException and friends
    public Closure closure;
    public LayerFilterConsumer filter;

    public LayerFilter(String name, String destinationPath, Closure closure) {
        this.name = name;
        this.destinationPath = destinationPath;
        this.closure = closure.rehydrate(this, this, this);
    }

    public LayerFilter(String name, String destinationPath, LayerFilterConsumer filter) {
        this.name = name;
        this.destinationPath = destinationPath;
        this.filter = filter;
    }

    public String getName() {
        return name;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public LayerFilterConsumer getFilter() {
        if (this.closure != null) {
            Closure filterClosure = this.closure;
            return new LayerFilterConsumer() {
                @Override
                public void execute(LayerFilterFile layerFilterFile) {
                    filterClosure.call(layerFilterFile);
                }
            };
        } else {
            return this.filter;
        }
    }

    @SuppressWarnings("unused")
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    @SuppressWarnings("unused")
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        if (this.closure != null) {
            this.closure = this.closure.rehydrate(this, this, this);
        }
    }
}
