package com.bsycorp.gradle.jib;

import org.gradle.api.file.FileCopyDetails;
import java.io.Serializable;
import java.util.function.Function;

public class LayerFilter implements Serializable {

    private static final long serialVersionUID = 1948900767703009070L;

    public String name;

    public String destinationPath;

    public Function<FileCopyDetails, FileCopyDetails> filter;

    public LayerFilter(String name, String destinationPath, Function<FileCopyDetails, FileCopyDetails> filter) {
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

    public Function<FileCopyDetails, FileCopyDetails> getFilter() {
        return filter;
    }
}
