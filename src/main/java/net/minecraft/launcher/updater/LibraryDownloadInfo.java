package net.minecraft.launcher.updater;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.launcher.updater.DownloadInfo;

public class LibraryDownloadInfo {
    private DownloadInfo artifact;
    private Map<String, DownloadInfo> classifiers;

    public LibraryDownloadInfo() {
    }

    public LibraryDownloadInfo(LibraryDownloadInfo other) {
        this.artifact = other.artifact;
        if (other.classifiers != null) {
            this.classifiers = new LinkedHashMap<String, DownloadInfo>();
            for (Map.Entry<String, DownloadInfo> entry : other.classifiers.entrySet()) {
                this.classifiers.put(entry.getKey(), new DownloadInfo(entry.getValue()));
            }
        }
    }

    public DownloadInfo getDownloadInfo(String classifier) {
        if (classifier == null) {
            return this.artifact;
        }
        return this.classifiers.get(classifier);
    }
}

