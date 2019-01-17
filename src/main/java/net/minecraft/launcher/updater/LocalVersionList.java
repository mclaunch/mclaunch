package net.minecraft.launcher.updater;

import com.google.gson.Gson;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.FileBasedVersionList;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalVersionList
extends FileBasedVersionList {
    private static final Logger LOGGER = LogManager.getLogger();
    private final File baseDirectory;
    private final File baseVersionsDir;

    public LocalVersionList(File baseDirectory) {
        if (baseDirectory == null || !baseDirectory.isDirectory()) {
            throw new IllegalArgumentException("Base directory is not a folder!");
        }
        this.baseDirectory = baseDirectory;
        this.baseVersionsDir = new File(this.baseDirectory, "versions");
        if (!this.baseVersionsDir.isDirectory()) {
            this.baseVersionsDir.mkdirs();
        }
    }

    @Override
    protected InputStream getFileInputStream(String path) throws FileNotFoundException {
        return new FileInputStream(new File(this.baseDirectory, path));
    }

    @Override
    public void refreshVersions() throws IOException {
        this.clearCache();
        File[] files = this.baseVersionsDir.listFiles();
        if (files == null) {
            return;
        }
        for (File directory : files) {
            String id = directory.getName();
            File jsonFile = new File(directory, id + ".json");
            if (!directory.isDirectory() || !jsonFile.exists()) continue;
            try {
                String path = "versions/" + id + "/" + id + ".json";
                CompleteVersion version = this.gson.fromJson(this.getContent(path), CompleteMinecraftVersion.class);
                if (version.getType() == null) {
                    LOGGER.warn("Ignoring: " + path + "; it has an invalid version specified");
                    return;
                }
                if (version.getId().equals(id)) {
                    this.addVersion(version);
                    continue;
                }
                LOGGER.warn("Ignoring: " + path + "; it contains id: '" + version.getId() + "' expected '" + id + "'");
            }
            catch (RuntimeException ex) {
                LOGGER.error("Couldn't load local version " + jsonFile.getAbsolutePath(), (Throwable)ex);
            }
        }
        for (Version version : this.getVersions()) {
            MinecraftReleaseType type = (MinecraftReleaseType)version.getType();
            if (this.getLatestVersion(type) != null && !this.getLatestVersion(type).getUpdatedTime().before(version.getUpdatedTime())) continue;
            this.setLatestVersion(version);
        }
    }

    public void saveVersion(CompleteVersion version) throws IOException {
        String text = this.serializeVersion(version);
        File target = new File(this.baseVersionsDir, version.getId() + "/" + version.getId() + ".json");
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }
        PrintWriter writer = new PrintWriter(target);
        writer.print(text);
        writer.close();
    }

    public File getBaseDirectory() {
        return this.baseDirectory;
    }

    @Override
    public boolean hasAllFiles(CompleteMinecraftVersion version, OperatingSystem os) {
        Set<String> files = version.getRequiredFiles(os);
        for (String file : files) {
            if (new File(this.baseDirectory, file).isFile()) continue;
            return false;
        }
        return true;
    }

    @Override
    public void uninstallVersion(Version version) {
        super.uninstallVersion(version);
        File dir = new File(this.baseVersionsDir, version.getId());
        if (dir.isDirectory()) {
            FileUtils.deleteQuietly(dir);
        }
    }
}

