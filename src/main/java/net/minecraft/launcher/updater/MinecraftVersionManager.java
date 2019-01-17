package net.minecraft.launcher.updater;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.ExceptionalThreadPoolExecutor;
import com.mojang.launcher.updater.VersionFilter;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.EtagDownloadable;
import com.mojang.launcher.updater.download.assets.AssetDownloadable;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.updater.AssetIndexInfo;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.DownloadInfo;
import net.minecraft.launcher.updater.DownloadType;
import net.minecraft.launcher.updater.LocalVersionList;
import net.minecraft.launcher.updater.PreHashedDownloadable;
import net.minecraft.launcher.updater.RemoteVersionList;
import net.minecraft.launcher.updater.VersionList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MinecraftVersionManager
implements VersionManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private final VersionList localVersionList;
    private final VersionList remoteVersionList;
    private final ThreadPoolExecutor executorService = new ExceptionalThreadPoolExecutor(4, 8, 30L, TimeUnit.SECONDS);
    private final List<RefreshedVersionsListener> refreshedVersionsListeners = Collections.synchronizedList(new ArrayList());
    private final Object refreshLock = new Object();
    private boolean isRefreshing;
    private final Gson gson = new Gson();

    public MinecraftVersionManager(VersionList localVersionList, VersionList remoteVersionList) {
        this.localVersionList = localVersionList;
        this.remoteVersionList = remoteVersionList;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void refreshVersions() throws IOException {
        Object object = this.refreshLock;
        synchronized (object) {
            this.isRefreshing = true;
        }
        try {
            LOGGER.info("Refreshing local version list...");
            this.localVersionList.refreshVersions();
            LOGGER.info("Refreshing remote version list...");
            this.remoteVersionList.refreshVersions();
        }
        catch (IOException ex2) {
            Object object2 = this.refreshLock;
            synchronized (object2) {
                this.isRefreshing = false;
            }
            throw ex2;
        }
        LOGGER.info("Refresh complete.");
        Object ex2 = this.refreshLock;
        synchronized (ex2) {
            this.isRefreshing = false;
        }
        for (RefreshedVersionsListener listener : Lists.newArrayList(this.refreshedVersionsListeners)) {
            listener.onVersionsRefreshed(this);
        }
    }

    @Override
    public List<VersionSyncInfo> getVersions() {
        return this.getVersions(null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public List<VersionSyncInfo> getVersions(VersionFilter<? extends ReleaseType> filter) {
        VersionSyncInfo syncInfo;
        Object object = this.refreshLock;
        synchronized (object) {
            if (this.isRefreshing) {
                return new ArrayList<VersionSyncInfo>();
            }
        }
        ArrayList<VersionSyncInfo> result = new ArrayList<VersionSyncInfo>();
        HashMap<String, VersionSyncInfo> lookup = new HashMap<String, VersionSyncInfo>();
        EnumMap<MinecraftReleaseType, Integer> counts = Maps.newEnumMap(MinecraftReleaseType.class);
        for (MinecraftReleaseType type : MinecraftReleaseType.values()) {
            counts.put(type, 0);
        }
        for (Version version : Lists.newArrayList(this.localVersionList.getVersions())) {
            if (version.getType() == null || version.getUpdatedTime() == null) continue;
            MinecraftReleaseType type = (MinecraftReleaseType)version.getType();
            if (filter != null && (!filter.getTypes().contains(type) || (Integer)counts.get(type) >= filter.getMaxCount())) continue;
            syncInfo = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
            lookup.put(version.getId(), syncInfo);
            result.add(syncInfo);
        }
        for (Version version : this.remoteVersionList.getVersions()) {
            if (version.getType() == null || version.getUpdatedTime() == null) continue;
            MinecraftReleaseType type = (MinecraftReleaseType)version.getType();
            if (lookup.containsKey(version.getId()) || filter != null && (!filter.getTypes().contains(type) || (Integer)counts.get(type) >= filter.getMaxCount())) continue;
            syncInfo = this.getVersionSyncInfo(this.localVersionList.getVersion(version.getId()), version);
            lookup.put(version.getId(), syncInfo);
            result.add(syncInfo);
            if (filter == null) continue;
            counts.put(type, (Integer)counts.get(type) + 1);
        }
        if (result.isEmpty()) {
            for (Version version : this.localVersionList.getVersions()) {
                if (version.getType() == null || version.getUpdatedTime() == null) continue;
                VersionSyncInfo syncInfo2 = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
                lookup.put(version.getId(), syncInfo2);
                result.add(syncInfo2);
                break;
            }
        }
        Collections.sort(result, new Comparator<VersionSyncInfo>(){

            @Override
            public int compare(VersionSyncInfo a, VersionSyncInfo b) {
                Version aVer = a.getLatestVersion();
                Version bVer = b.getLatestVersion();
                if (aVer.getReleaseTime() != null && bVer.getReleaseTime() != null) {
                    return bVer.getReleaseTime().compareTo(aVer.getReleaseTime());
                }
                return bVer.getUpdatedTime().compareTo(aVer.getUpdatedTime());
            }
        });
        return result;
    }

    @Override
    public VersionSyncInfo getVersionSyncInfo(Version version) {
        return this.getVersionSyncInfo(version.getId());
    }

    @Override
    public VersionSyncInfo getVersionSyncInfo(String name) {
        return this.getVersionSyncInfo(this.localVersionList.getVersion(name), this.remoteVersionList.getVersion(name));
    }

    @Override
    public VersionSyncInfo getVersionSyncInfo(Version localVersion, Version remoteVersion) {
        boolean installed;
        boolean upToDate = installed = localVersion != null;
        CompleteMinecraftVersion resolved = null;
        if (installed && remoteVersion != null) {
            boolean bl = upToDate = !remoteVersion.getUpdatedTime().after(localVersion.getUpdatedTime());
        }
        if (localVersion instanceof CompleteVersion) {
            try {
                resolved = ((CompleteMinecraftVersion)localVersion).resolve(this);
            }
            catch (IOException ex) {
                LOGGER.error("Couldn't resolve version " + localVersion.getId(), (Throwable)ex);
                resolved = (CompleteMinecraftVersion)localVersion;
            }
            upToDate &= this.localVersionList.hasAllFiles(resolved, OperatingSystem.getCurrentPlatform());
        }
        return new VersionSyncInfo(resolved, remoteVersion, installed, upToDate);
    }

    @Override
    public List<VersionSyncInfo> getInstalledVersions() {
        ArrayList<VersionSyncInfo> result = new ArrayList<VersionSyncInfo>();
        ArrayList<Version> versions = Lists.newArrayList(this.localVersionList.getVersions());
        for (Version version : versions) {
            if (version.getType() == null || version.getUpdatedTime() == null) continue;
            VersionSyncInfo syncInfo = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
            result.add(syncInfo);
        }
        return result;
    }

    public VersionList getRemoteVersionList() {
        return this.remoteVersionList;
    }

    public VersionList getLocalVersionList() {
        return this.localVersionList;
    }

    @Override
    public CompleteMinecraftVersion getLatestCompleteVersion(VersionSyncInfo syncInfo) throws IOException {
        if (syncInfo.getLatestSource() == VersionSyncInfo.VersionSource.REMOTE) {
            CompleteMinecraftVersion result = null;
            IOException exception = null;
            try {
                result = this.remoteVersionList.getCompleteVersion(syncInfo.getLatestVersion());
            }
            catch (IOException e) {
                exception = e;
                try {
                    result = this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
                }
                catch (IOException iOException) {
                    // empty catch block
                }
            }
            if (result != null) {
                return result;
            }
            throw exception;
        }
        return this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
    }

    @Override
    public DownloadJob downloadVersion(VersionSyncInfo syncInfo, DownloadJob job) throws IOException {
        if (!(this.localVersionList instanceof LocalVersionList)) {
            throw new IllegalArgumentException("Cannot download if local repo isn't a LocalVersionList");
        }
        if (!(this.remoteVersionList instanceof RemoteVersionList)) {
            throw new IllegalArgumentException("Cannot download if local repo isn't a RemoteVersionList");
        }
        CompleteMinecraftVersion version = this.getLatestCompleteVersion(syncInfo);
        File baseDirectory = ((LocalVersionList)this.localVersionList).getBaseDirectory();
        Proxy proxy = ((RemoteVersionList)this.remoteVersionList).getProxy();
        job.addDownloadables(version.getRequiredDownloadables(OperatingSystem.getCurrentPlatform(), proxy, baseDirectory, false));
        String jarFile = "versions/" + version.getJar() + "/" + version.getJar() + ".jar";
        DownloadInfo clientInfo = version.getDownloadURL(DownloadType.CLIENT);
        if (clientInfo == null) {
            job.addDownloadables(new EtagDownloadable(proxy, new URL("https://s3.amazonaws.com/Minecraft.Download/" + jarFile), new File(baseDirectory, jarFile), false));
        } else {
            job.addDownloadables(new PreHashedDownloadable(proxy, clientInfo.getUrl(), new File(baseDirectory, jarFile), false, clientInfo.getSha1()));
        }
        return job;
    }

    @Override
    public DownloadJob downloadResources(DownloadJob job, CompleteVersion version) throws IOException {
        File baseDirectory = ((LocalVersionList)this.localVersionList).getBaseDirectory();
        job.addDownloadables(this.getResourceFiles(((RemoteVersionList)this.remoteVersionList).getProxy(), baseDirectory, (CompleteMinecraftVersion)version));
        return job;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Set<Downloadable> getResourceFiles(Proxy proxy, File baseDirectory, CompleteMinecraftVersion version) {
        HashSet<Downloadable> result;
        result = new HashSet<Downloadable>();
        InputStream inputStream = null;
        File assets = new File(baseDirectory, "assets");
        File objectsFolder = new File(assets, "objects");
        File indexesFolder = new File(assets, "indexes");
        long start = System.nanoTime();
        AssetIndexInfo indexInfo = version.getAssetIndex();
        File indexFile = new File(indexesFolder, indexInfo.getId() + ".json");
        try {
            URL indexUrl = indexInfo.getUrl();
            inputStream = indexUrl.openConnection(proxy).getInputStream();
            String json = IOUtils.toString(inputStream);
            FileUtils.writeStringToFile(indexFile, json);
            AssetIndex index = this.gson.fromJson(json, AssetIndex.class);
            for (Map.Entry<AssetIndex.AssetObject, String> entry : index.getUniqueObjects().entrySet()) {
                AssetIndex.AssetObject object = entry.getKey();
                String filename = object.getHash().substring(0, 2) + "/" + object.getHash();
                File file = new File(objectsFolder, filename);
                if (file.isFile() && FileUtils.sizeOf(file) == object.getSize()) continue;
                AssetDownloadable downloadable = new AssetDownloadable(proxy, entry.getValue(), object, "http://resources.download.minecraft.net/", objectsFolder);
                downloadable.setExpectedSize(object.getSize());
                result.add(downloadable);
            }
            long end = System.nanoTime();
            long delta = end - start;
            LOGGER.debug("Delta time to compare resources: " + delta / 1000000L + " ms ");
        }
        catch (Exception ex) {
            MinecraftVersionManager.LOGGER.error("Couldn't download resources", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return result;
    }

    @Override
    public ThreadPoolExecutor getExecutorService() {
        return this.executorService;
    }

    @Override
    public void addRefreshedVersionsListener(RefreshedVersionsListener listener) {
        this.refreshedVersionsListeners.add(listener);
    }

    @Override
    public void removeRefreshedVersionsListener(RefreshedVersionsListener listener) {
        this.refreshedVersionsListeners.remove(listener);
    }

    @Override
    public VersionSyncInfo syncVersion(VersionSyncInfo syncInfo) throws IOException {
        CompleteMinecraftVersion remoteVersion = this.getRemoteVersionList().getCompleteVersion(syncInfo.getRemoteVersion());
        this.getLocalVersionList().removeVersion(syncInfo.getLocalVersion());
        this.getLocalVersionList().addVersion(remoteVersion);
        ((LocalVersionList)this.getLocalVersionList()).saveVersion(remoteVersion.getSavableVersion());
        return this.getVersionSyncInfo(remoteVersion);
    }

    @Override
    public void installVersion(CompleteVersion version) throws IOException {
        VersionList localVersionList;
        if (version instanceof CompleteMinecraftVersion) {
            version = ((CompleteMinecraftVersion)version).getSavableVersion();
        }
        if ((localVersionList = this.getLocalVersionList()).getVersion(version.getId()) != null) {
            localVersionList.removeVersion(version.getId());
        }
        localVersionList.addVersion(version);
        if (localVersionList instanceof LocalVersionList) {
            ((LocalVersionList)localVersionList).saveVersion(version);
        }
        LOGGER.info("Installed " + version);
    }

    @Override
    public void uninstallVersion(CompleteVersion version) throws IOException {
        VersionList localVersionList = this.getLocalVersionList();
        if (localVersionList instanceof LocalVersionList) {
            localVersionList.uninstallVersion(version);
            LOGGER.info("Uninstalled " + version);
        }
    }

}

