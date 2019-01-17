package net.minecraft.launcher.updater;

import java.net.URL;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.updater.DownloadInfo;

public class AssetIndexInfo
extends DownloadInfo {
    protected long totalSize;
    protected String id;
    protected boolean known = true;

    public AssetIndexInfo() {
    }

    public AssetIndexInfo(String id) {
        this.id = id;
        this.url = LauncherConstants.constantURL("https://s3.amazonaws.com/Minecraft.Download/indexes/" + id + ".json");
        this.known = false;
    }

    public long getTotalSize() {
        return this.totalSize;
    }

    public String getId() {
        return this.id;
    }

    public boolean sizeAndHashKnown() {
        return this.known;
    }
}

