package net.minecraft.launcher.updater;

import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.net.URL;
import java.util.Date;
import net.minecraft.launcher.game.MinecraftReleaseType;

public class PartialVersion
implements Version {
    private String id;
    private Date time;
    private Date releaseTime;
    private MinecraftReleaseType type;
    private URL url;

    public PartialVersion() {
    }

    public PartialVersion(String id, Date releaseTime, Date updateTime, MinecraftReleaseType type, URL url) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        if (releaseTime == null) {
            throw new IllegalArgumentException("Release time cannot be null");
        }
        if (updateTime == null) {
            throw new IllegalArgumentException("Update time cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Release type cannot be null");
        }
        this.id = id;
        this.releaseTime = releaseTime;
        this.time = updateTime;
        this.type = type;
        this.url = url;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public MinecraftReleaseType getType() {
        return this.type;
    }

    @Override
    public Date getUpdatedTime() {
        return this.time;
    }

    public void setUpdatedTime(Date time) {
        if (time == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
        this.time = time;
    }

    @Override
    public Date getReleaseTime() {
        return this.releaseTime;
    }

    public void setReleaseTime(Date time) {
        if (time == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
        this.releaseTime = time;
    }

    public void setType(MinecraftReleaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Release type cannot be null");
        }
        this.type = type;
    }

    public URL getUrl() {
        return this.url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String toString() {
        return "PartialVersion{id='" + this.id + '\'' + ", updateTime=" + this.time + ", releaseTime=" + this.releaseTime + ", type=" + this.type + ", url=" + this.url + '}';
    }
}

