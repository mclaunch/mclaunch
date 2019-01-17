package net.minecraft.launcher.profile;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.mojang.launcher.updater.VersionFilter;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.ReleaseTypeFactory;
import java.io.File;
import java.util.Set;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.game.MinecraftReleaseTypeFactory;
import net.minecraft.launcher.profile.LauncherVisibilityRule;

public class Profile
implements Comparable<Profile> {
    public static final String DEFAULT_JRE_ARGUMENTS_64BIT = "-Xmx1G -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:-UseAdaptiveSizePolicy -Xmn128M";
    public static final String DEFAULT_JRE_ARGUMENTS_32BIT = "-Xmx512M -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:-UseAdaptiveSizePolicy -Xmn128M";
    public static final Resolution DEFAULT_RESOLUTION = new Resolution(854, 480);
    public static final LauncherVisibilityRule DEFAULT_LAUNCHER_VISIBILITY = LauncherVisibilityRule.CLOSE_LAUNCHER;
    public static final Set<MinecraftReleaseType> DEFAULT_RELEASE_TYPES = Sets.newHashSet(MinecraftReleaseType.RELEASE);
    private String name;
    private File gameDir;
    private String lastVersionId;
    private String javaDir;
    private String javaArgs;
    private Resolution resolution;
    private Set<MinecraftReleaseType> allowedReleaseTypes;
    private String playerUUID;
    private Boolean useHopperCrashService;
    private LauncherVisibilityRule launcherVisibilityOnGameClose;

    public Profile() {
    }

    public Profile(Profile copy) {
        this.name = copy.name;
        this.gameDir = copy.gameDir;
        this.playerUUID = copy.playerUUID;
        this.lastVersionId = copy.lastVersionId;
        this.javaDir = copy.javaDir;
        this.javaArgs = copy.javaArgs;
        this.resolution = copy.resolution == null ? null : new Resolution(copy.resolution);
        this.allowedReleaseTypes = copy.allowedReleaseTypes == null ? null : Sets.newHashSet(copy.allowedReleaseTypes);
        this.useHopperCrashService = copy.useHopperCrashService;
        this.launcherVisibilityOnGameClose = copy.launcherVisibilityOnGameClose;
    }

    public Profile(String name) {
        this.name = name;
    }

    public String getName() {
        return Objects.firstNonNull(this.name, "");
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getGameDir() {
        return this.gameDir;
    }

    public void setGameDir(File gameDir) {
        this.gameDir = gameDir;
    }

    public void setLastVersionId(String lastVersionId) {
        this.lastVersionId = lastVersionId;
    }

    public void setJavaDir(String javaDir) {
        this.javaDir = javaDir;
    }

    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    public String getLastVersionId() {
        return this.lastVersionId;
    }

    public String getJavaArgs() {
        return this.javaArgs;
    }

    public String getJavaPath() {
        return this.javaDir;
    }

    public Resolution getResolution() {
        return this.resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }

    @Deprecated
    public String getPlayerUUID() {
        return this.playerUUID;
    }

    @Deprecated
    public void setPlayerUUID(String playerUUID) {
        this.playerUUID = playerUUID;
    }

    public Set<MinecraftReleaseType> getAllowedReleaseTypes() {
        return this.allowedReleaseTypes;
    }

    public void setAllowedReleaseTypes(Set<MinecraftReleaseType> allowedReleaseTypes) {
        this.allowedReleaseTypes = allowedReleaseTypes;
    }

    public boolean getUseHopperCrashService() {
        return this.useHopperCrashService == null;
    }

    public void setUseHopperCrashService(boolean useHopperCrashService) {
        this.useHopperCrashService = useHopperCrashService ? null : Boolean.valueOf(false);
    }

    public VersionFilter<MinecraftReleaseType> getVersionFilter() {
        final VersionFilter<MinecraftReleaseType> filter = new VersionFilter<MinecraftReleaseType>(MinecraftReleaseTypeFactory.instance()).setMaxCount(Integer.MAX_VALUE);
        if (this.allowedReleaseTypes == null) {
            filter.onlyForTypes((MinecraftReleaseType[]) Profile.DEFAULT_RELEASE_TYPES.toArray(new MinecraftReleaseType[ Profile.DEFAULT_RELEASE_TYPES.size() ]));
        } else {
            filter.onlyForTypes((MinecraftReleaseType[]) this.allowedReleaseTypes.toArray(new MinecraftReleaseType[ this.allowedReleaseTypes.size() ]));
        }
        return filter;
    }

    public LauncherVisibilityRule getLauncherVisibilityOnGameClose() {
        return this.launcherVisibilityOnGameClose;
    }

    public void setLauncherVisibilityOnGameClose(LauncherVisibilityRule launcherVisibilityOnGameClose) {
        this.launcherVisibilityOnGameClose = launcherVisibilityOnGameClose;
    }

    @Override
    public int compareTo(Profile o) {
        if (o == null) {
            return -1;
        }
        return this.getName().compareTo(o.getName());
    }

    public static class Resolution {
        private int width;
        private int height;

        public Resolution() {
        }

        public Resolution(Resolution resolution) {
            this(resolution.getWidth(), resolution.getHeight());
        }

        public Resolution(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }
    }

}

