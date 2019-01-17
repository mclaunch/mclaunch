package net.minecraft.launcher.updater;

import com.google.gson.Gson;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.PartialVersion;
import net.minecraft.launcher.updater.VersionList;
import org.apache.commons.io.IOUtils;

public abstract class FileBasedVersionList
extends VersionList {
    public String getContent(String path) throws IOException {
        return IOUtils.toString(this.getFileInputStream(path)).replaceAll("\\r\\n", "\r").replaceAll("\\r", "\n");
    }

    protected abstract InputStream getFileInputStream(String var1) throws FileNotFoundException;

    @Override
    public CompleteMinecraftVersion getCompleteVersion(Version version) throws IOException {
        if (version instanceof CompleteVersion) {
            return (CompleteMinecraftVersion)version;
        }
        if (!(version instanceof PartialVersion)) {
            throw new IllegalArgumentException("Version must be a partial");
        }
        PartialVersion partial = (PartialVersion)version;
        CompleteMinecraftVersion complete = this.gson.fromJson(this.getContent("versions/" + version.getId() + "/" + version.getId() + ".json"), CompleteMinecraftVersion.class);
        MinecraftReleaseType type = (MinecraftReleaseType)version.getType();
        this.replacePartialWithFull(partial, complete);
        return complete;
    }
}

