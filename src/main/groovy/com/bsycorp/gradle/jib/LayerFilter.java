package com.bsycorp.gradle.jib;

import java.io.Serializable;
import java.util.function.Function;

public class LayerFilter implements Serializable {

    private static final long serialVersionUID = 1948900767703009070L;

    public String name;

    public String destinationPath;

    public Function<LayerFilterFile, LayerFilterFile> filter;

    public LayerFilter(String name, String destinationPath, Function<LayerFilterFile, LayerFilterFile> filter) {
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

    public Function<LayerFilterFile, LayerFilterFile> getFilter() {
        return filter;
    }
}
