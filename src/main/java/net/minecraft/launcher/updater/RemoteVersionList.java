package net.minecraft.launcher.updater;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.mojang.launcher.Http;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.Version;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.PartialVersion;
import net.minecraft.launcher.updater.VersionList;

public class RemoteVersionList
extends VersionList {
    private final URL manifestUrl;
    private final Proxy proxy;

    public RemoteVersionList(URL manifestUrl, Proxy proxy) {
        this.manifestUrl = manifestUrl;
        this.proxy = proxy;
    }

    @Override
    public CompleteMinecraftVersion getCompleteVersion(Version version) throws IOException {
        if (version instanceof CompleteVersion) {
            return (CompleteMinecraftVersion)version;
        }
        if (!(version instanceof PartialVersion)) {
            throw new IllegalArgumentException("Version must be a partial");
        }
        PartialVersion partial = (PartialVersion)version;
        CompleteMinecraftVersion complete = this.gson.fromJson(Http.performGet(partial.getUrl(), this.proxy), CompleteMinecraftVersion.class);
        this.replacePartialWithFull(partial, complete);
        return complete;
    }

    @Override
    public void refreshVersions() throws IOException {
        this.clearCache();
        RawVersionList versionList = this.gson.fromJson(this.getContent(this.manifestUrl), RawVersionList.class);
        for (Version version : versionList.getVersions()) {
            this.versions.add(version);
            this.versionsByName.put(version.getId(), version);
        }
        for (MinecraftReleaseType type : MinecraftReleaseType.values()) {
            this.latestVersions.put(type, this.versionsByName.get(versionList.getLatestVersions().get(type)));
        }
    }

    @Override
    public boolean hasAllFiles(CompleteMinecraftVersion version, OperatingSystem os) {
        return true;
    }

    public String getContent(URL url) throws IOException {
        return Http.performGet(url, this.proxy);
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    private static class RawVersionList {
        private List<PartialVersion> versions = new ArrayList<PartialVersion>();
        private Map<MinecraftReleaseType, String> latest = Maps.newEnumMap(MinecraftReleaseType.class);

        private RawVersionList() {
        }

        public List<PartialVersion> getVersions() {
            return this.versions;
        }

        public Map<MinecraftReleaseType, String> getLatestVersions() {
            return this.latest;
        }
    }

}

