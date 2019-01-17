package net.minecraft.launcher.updater;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.ReleaseTypeAdapterFactory;
import com.mojang.launcher.versions.ReleaseTypeFactory;
import com.mojang.launcher.versions.Version;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.game.MinecraftReleaseTypeFactory;
import net.minecraft.launcher.updater.Argument;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.PartialVersion;

public abstract class VersionList {
    protected final Gson gson;
    protected final Map<String, Version> versionsByName = new HashMap<String, Version>();
    protected final List<Version> versions = new ArrayList<Version>();
    protected final Map<MinecraftReleaseType, Version> latestVersions = Maps.newEnumMap(MinecraftReleaseType.class);

    public VersionList() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter((Type)((Object)Date.class), new DateTypeAdapter());
        builder.registerTypeAdapter((Type)((Object)ReleaseType.class), new ReleaseTypeAdapterFactory(MinecraftReleaseTypeFactory.instance()));
        builder.registerTypeAdapter((Type)((Object)Argument.class), new Argument.Serializer());
        builder.enableComplexMapKeySerialization();
        builder.setPrettyPrinting();
        this.gson = builder.create();
    }

    public Collection<Version> getVersions() {
        return this.versions;
    }

    public Version getLatestVersion(MinecraftReleaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        return this.latestVersions.get(type);
    }

    public Version getVersion(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        return this.versionsByName.get(name);
    }

    public abstract CompleteMinecraftVersion getCompleteVersion(Version var1) throws IOException;

    protected void replacePartialWithFull(PartialVersion version, CompleteVersion complete) {
        Collections.replaceAll(this.versions, version, complete);
        this.versionsByName.put(version.getId(), complete);
        if (this.latestVersions.get(version.getType()) == version) {
            this.latestVersions.put(version.getType(), complete);
        }
    }

    protected void clearCache() {
        this.versionsByName.clear();
        this.versions.clear();
        this.latestVersions.clear();
    }

    public abstract void refreshVersions() throws IOException;

    public CompleteVersion addVersion(CompleteVersion version) {
        if (version.getId() == null) {
            throw new IllegalArgumentException("Cannot add blank version");
        }
        if (this.getVersion(version.getId()) != null) {
            throw new IllegalArgumentException("Version '" + version.getId() + "' is already tracked");
        }
        this.versions.add(version);
        this.versionsByName.put(version.getId(), version);
        return version;
    }

    public void removeVersion(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        Version version = this.getVersion(name);
        if (version == null) {
            throw new IllegalArgumentException("Unknown version - cannot remove null");
        }
        this.removeVersion(version);
    }

    public void removeVersion(Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot remove null version");
        }
        this.versions.remove(version);
        this.versionsByName.remove(version.getId());
        for (MinecraftReleaseType type : MinecraftReleaseType.values()) {
            if (this.getLatestVersion(type) != version) continue;
            this.latestVersions.remove(type);
        }
    }

    public void setLatestVersion(Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot set latest version to null");
        }
        this.latestVersions.put((MinecraftReleaseType)version.getType(), version);
    }

    public void setLatestVersion(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        Version version = this.getVersion(name);
        if (version == null) {
            throw new IllegalArgumentException("Unknown version - cannot set latest version to null");
        }
        this.setLatestVersion(version);
    }

    public String serializeVersion(CompleteVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot serialize null!");
        }
        return this.gson.toJson(version);
    }

    public abstract boolean hasAllFiles(CompleteMinecraftVersion var1, OperatingSystem var2);

    public void uninstallVersion(Version version) {
        this.removeVersion(version);
    }
}

