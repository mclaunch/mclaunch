package net.minecraft.launcher.updater;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.game.process.GameProcessBuilder;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.launcher.CompatibilityRule;
import net.minecraft.launcher.CurrentLaunchFeatureMatcher;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.updater.Argument;
import net.minecraft.launcher.updater.ArgumentType;
import net.minecraft.launcher.updater.AssetIndexInfo;
import net.minecraft.launcher.updater.DownloadInfo;
import net.minecraft.launcher.updater.DownloadType;
import net.minecraft.launcher.updater.Library;
import net.minecraft.launcher.updater.MinecraftVersionManager;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CompleteMinecraftVersion
implements CompleteVersion {
    private static final Logger LOGGER = LogManager.getLogger();
    private String inheritsFrom;
    private String id;
    private Date time;
    private Date releaseTime;
    private ReleaseType type;
    private String minecraftArguments;
    private List<Library> libraries;
    private String mainClass;
    private int minimumLauncherVersion;
    private String incompatibilityReason;
    private String assets;
    private List<CompatibilityRule> compatibilityRules;
    private String jar;
    private CompleteMinecraftVersion savableVersion;
    private transient boolean synced = false;
    private Map<DownloadType, DownloadInfo> downloads = Maps.newEnumMap(DownloadType.class);
    private AssetIndexInfo assetIndex;
    private Map<ArgumentType, List<Argument>> arguments;

    public CompleteMinecraftVersion() {
    }

    public CompleteMinecraftVersion(CompleteMinecraftVersion version) {
        this.inheritsFrom = version.inheritsFrom;
        this.id = version.id;
        this.time = version.time;
        this.releaseTime = version.releaseTime;
        this.type = version.type;
        this.minecraftArguments = version.minecraftArguments;
        this.mainClass = version.mainClass;
        this.minimumLauncherVersion = version.minimumLauncherVersion;
        this.incompatibilityReason = version.incompatibilityReason;
        this.assets = version.assets;
        this.jar = version.jar;
        this.downloads = version.downloads;
        if (version.libraries != null) {
            this.libraries = Lists.newArrayList();
            for (Library library : version.getLibraries()) {
                this.libraries.add(new Library(library));
            }
        }
        if (version.arguments != null) {
            this.arguments = Maps.newEnumMap(ArgumentType.class);
            for (Map.Entry entry : version.arguments.entrySet()) {
                this.arguments.put((ArgumentType)((Object)entry.getKey()), new ArrayList((Collection)entry.getValue()));
            }
        }
        if (version.compatibilityRules != null) {
            this.compatibilityRules = Lists.newArrayList();
            for (CompatibilityRule compatibilityRule : version.compatibilityRules) {
                this.compatibilityRules.add(new CompatibilityRule(compatibilityRule));
            }
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public ReleaseType getType() {
        return this.type;
    }

    @Override
    public Date getUpdatedTime() {
        return this.time;
    }

    @Override
    public Date getReleaseTime() {
        return this.releaseTime;
    }

    public List<Library> getLibraries() {
        return this.libraries;
    }

    public String getMainClass() {
        return this.mainClass;
    }

    public String getJar() {
        return this.jar == null ? this.id : this.jar;
    }

    public void setType(ReleaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Release type cannot be null");
        }
        this.type = type;
    }

    public Collection<Library> getRelevantLibraries(CompatibilityRule.FeatureMatcher featureMatcher) {
        ArrayList<Library> result = new ArrayList<Library>();
        for (Library library : this.libraries) {
            if (!library.appliesToCurrentEnvironment(featureMatcher)) continue;
            result.add(library);
        }
        return result;
    }

    public Collection<File> getClassPath(OperatingSystem os, File base, CompatibilityRule.FeatureMatcher featureMatcher) {
        Collection<Library> libraries = this.getRelevantLibraries(featureMatcher);
        ArrayList<File> result = new ArrayList<File>();
        for (Library library : libraries) {
            if (library.getNatives() != null) continue;
            result.add(new File(base, "libraries/" + library.getArtifactPath()));
        }
        result.add(new File(base, "versions/" + this.getJar() + "/" + this.getJar() + ".jar"));
        return result;
    }

    public Set<String> getRequiredFiles(OperatingSystem os) {
        HashSet<String> neededFiles = new HashSet<String>();
        for (Library library : this.getRelevantLibraries(this.createFeatureMatcher())) {
            if (library.getNatives() != null) {
                String natives = library.getNatives().get((Object)os);
                if (natives == null) continue;
                neededFiles.add("libraries/" + library.getArtifactPath(natives));
                continue;
            }
            neededFiles.add("libraries/" + library.getArtifactPath());
        }
        return neededFiles;
    }

    public Set<Downloadable> getRequiredDownloadables(OperatingSystem os, Proxy proxy, File targetDirectory, boolean ignoreLocalFiles) throws MalformedURLException {
        HashSet<Downloadable> neededFiles = new HashSet<Downloadable>();
        for (Library library : this.getRelevantLibraries(this.createFeatureMatcher())) {
            Downloadable download;
            File local;
            String file = null;
            String classifier = null;
            if (library.getNatives() != null) {
                classifier = library.getNatives().get((Object)os);
                if (classifier != null) {
                    file = library.getArtifactPath(classifier);
                }
            } else {
                file = library.getArtifactPath();
            }
            if (file == null || (download = library.createDownload(proxy, file, local = new File(targetDirectory, "libraries/" + file), ignoreLocalFiles, classifier)) == null) continue;
            neededFiles.add(download);
        }
        return neededFiles;
    }

    public String toString() {
        return "CompleteVersion{id='" + this.id + '\'' + ", updatedTime=" + this.time + ", releasedTime=" + this.time + ", type=" + this.type + ", libraries=" + this.libraries + ", mainClass='" + this.mainClass + '\'' + ", jar='" + this.jar + '\'' + ", minimumLauncherVersion=" + this.minimumLauncherVersion + '}';
    }

    public String getMinecraftArguments() {
        return this.minecraftArguments;
    }

    @Override
    public int getMinimumLauncherVersion() {
        return this.minimumLauncherVersion;
    }

    @Override
    public boolean appliesToCurrentEnvironment() {
        if (this.compatibilityRules == null) {
            return true;
        }
        CompatibilityRule.Action lastAction = CompatibilityRule.Action.DISALLOW;
        for (CompatibilityRule compatibilityRule : this.compatibilityRules) {
            ProfileManager profileManager = Launcher.getCurrentInstance().getProfileManager();
            UserAuthentication auth = profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
            CompatibilityRule.Action action = compatibilityRule.getAppliedAction(new CurrentLaunchFeatureMatcher(profileManager.getSelectedProfile(), this, auth));
            if (action == null) continue;
            lastAction = action;
        }
        return lastAction == CompatibilityRule.Action.ALLOW;
    }

    @Override
    public String getIncompatibilityReason() {
        return this.incompatibilityReason;
    }

    @Override
    public boolean isSynced() {
        return this.synced;
    }

    @Override
    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public String getInheritsFrom() {
        return this.inheritsFrom;
    }

    public CompleteMinecraftVersion resolve(MinecraftVersionManager versionManager) throws IOException {
        return this.resolve(versionManager, Sets.<String>newHashSet());
    }

    protected CompleteMinecraftVersion resolve(MinecraftVersionManager versionManager, Set<String> resolvedSoFar) throws IOException {
        if (this.inheritsFrom == null) {
            return this;
        }
        if (!resolvedSoFar.add(this.id)) {
            throw new IllegalStateException("Circular dependency detected");
        }
        VersionSyncInfo parentSync = versionManager.getVersionSyncInfo(this.inheritsFrom);
        CompleteMinecraftVersion parent = versionManager.getLatestCompleteVersion(parentSync).resolve(versionManager, resolvedSoFar);
        CompleteMinecraftVersion result = new CompleteMinecraftVersion(parent);
        if (!parentSync.isInstalled() || !parentSync.isUpToDate() || parentSync.getLatestSource() != VersionSyncInfo.VersionSource.LOCAL) {
            versionManager.installVersion(parent);
        }
        result.savableVersion = this;
        result.inheritsFrom = null;
        result.id = this.id;
        result.time = this.time;
        result.releaseTime = this.releaseTime;
        result.type = this.type;
        if (this.minecraftArguments != null) {
            result.minecraftArguments = this.minecraftArguments;
        }
        if (this.mainClass != null) {
            result.mainClass = this.mainClass;
        }
        if (this.incompatibilityReason != null) {
            result.incompatibilityReason = this.incompatibilityReason;
        }
        if (this.assets != null) {
            result.assets = this.assets;
        }
        if (this.jar != null) {
            result.jar = this.jar;
        }
        if (this.libraries != null) {
            ArrayList<Library> newLibraries = Lists.newArrayList();
            for (Library library : this.libraries) {
                newLibraries.add(new Library(library));
            }
            for (Library library : result.libraries) {
                newLibraries.add(library);
            }
            result.libraries = newLibraries;
        }
        if (this.arguments != null) {
            if (result.arguments == null) {
                result.arguments = new EnumMap<ArgumentType, List<Argument>>(ArgumentType.class);
            }
            for (Map.Entry entry : this.arguments.entrySet()) {
                List<Argument> arguments = result.arguments.get(entry.getKey());
                if (arguments == null) {
                    arguments = new ArrayList<Argument>();
                    result.arguments.put((ArgumentType)((Object)entry.getKey()), arguments);
                }
                arguments.addAll((Collection)entry.getValue());
            }
        }
        if (this.compatibilityRules != null) {
            for (CompatibilityRule compatibilityRule : this.compatibilityRules) {
                result.compatibilityRules.add(new CompatibilityRule(compatibilityRule));
            }
        }
        return result;
    }

    public CompleteMinecraftVersion getSavableVersion() {
        return Objects.firstNonNull(this.savableVersion, this);
    }

    public DownloadInfo getDownloadURL(DownloadType type) {
        return this.downloads.get((Object)type);
    }

    public AssetIndexInfo getAssetIndex() {
        if (this.assetIndex == null) {
            this.assetIndex = new AssetIndexInfo(Objects.firstNonNull(this.assets, "legacy"));
        }
        return this.assetIndex;
    }

    public CompatibilityRule.FeatureMatcher createFeatureMatcher() {
        ProfileManager profileManager = Launcher.getCurrentInstance().getProfileManager();
        UserAuthentication auth = profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        return new CurrentLaunchFeatureMatcher(profileManager.getSelectedProfile(), this, auth);
    }

    public void addArguments(ArgumentType type, CompatibilityRule.FeatureMatcher featureMatcher, GameProcessBuilder builder, StrSubstitutor substitutor) {
        if (this.arguments != null) {
            List<Argument> args = this.arguments.get((Object)type);
            if (args != null) {
                for (Argument argument : args) {
                    argument.apply(builder, featureMatcher, substitutor);
                }
            }
        } else if (this.minecraftArguments != null) {
            if (type == ArgumentType.GAME) {
                for (String arg : this.minecraftArguments.split(" ")) {
                    builder.withArguments(substitutor.replace(arg));
                }
                if (featureMatcher.hasFeature("is_demo_user", true)) {
                    builder.withArguments("--demo");
                }
                if (featureMatcher.hasFeature("has_custom_resolution", true)) {
                    builder.withArguments("--width", substitutor.replace("${resolution_width}"), "--height", substitutor.replace("${resolution_height}"));
                }
            } else if (type == ArgumentType.JVM) {
                if (OperatingSystem.getCurrentPlatform() == OperatingSystem.WINDOWS) {
                    builder.withArguments("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump");
                    if (Launcher.getCurrentInstance().usesWinTenHack()) {
                        builder.withArguments("-Dos.name=Windows 10", "-Dos.version=10.0");
                    }
                } else if (OperatingSystem.getCurrentPlatform() == OperatingSystem.OSX) {
                    builder.withArguments(substitutor.replace("-Xdock:icon=${asset=icons/minecraft.icns}"), "-Xdock:name=Minecraft");
                }
                builder.withArguments(substitutor.replace("-Djava.library.path=${natives_directory}"));
                builder.withArguments(substitutor.replace("-Dminecraft.launcher.brand=${launcher_name}"));
                builder.withArguments(substitutor.replace("-Dminecraft.launcher.version=${launcher_version}"));
                builder.withArguments(substitutor.replace("-Dminecraft.client.jar=${primary_jar}"));
                builder.withArguments("-cp", substitutor.replace("${classpath}"));
            }
        }
    }
}

