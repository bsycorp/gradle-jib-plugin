package com.bsycorp.gradle.jib.models;

import org.gradle.api.file.FileCopyDetails;

public class LayerFilterFile {

    private FileCopyDetails details;
    private boolean alreadyAddedToImage;
    private boolean includedInLayer = true;

    public LayerFilterFile(FileCopyDetails details, boolean alreadyAddedToImage) {
        this.details = details;
        this.alreadyAddedToImage = alreadyAddedToImage;
    }

    public FileCopyDetails getDetails() {
        return details;
    }

    public boolean isAlreadyAddedToImage() {
        return alreadyAddedToImage;
    }

    public void setName(String s) {
        details.setName(s);
    }

    public void setPath(String s) {
        details.setPath(s);
    }

    public String getName() {
        return details.getName();
    }

    public String getPath() {
        return details.getPath();
    }

    public void exclude() {
        includedInLayer = false;
    }

    public boolean isExcluded() {
        return !includedInLayer;
    }
}
