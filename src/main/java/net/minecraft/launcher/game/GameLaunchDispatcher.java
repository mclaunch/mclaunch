package net.minecraft.launcher.game;

import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.game.runner.GameRunner;
import com.mojang.launcher.game.runner.GameRunnerListener;
import com.mojang.launcher.updater.VersionFilter;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.MinecraftUserInterface;
import net.minecraft.launcher.game.MinecraftGameRunner;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.LauncherVisibilityRule;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;

public class GameLaunchDispatcher
implements GameRunnerListener {
    private final Launcher launcher;
    private final String[] additionalLaunchArgs;
    private final ReentrantLock lock = new ReentrantLock();
    private final BiMap<UserAuthentication, MinecraftGameRunner> instances = HashBiMap.create();
    private boolean downloadInProgress = false;

    public GameLaunchDispatcher(Launcher launcher, String[] additionalLaunchArgs) {
        this.launcher = launcher;
        this.additionalLaunchArgs = additionalLaunchArgs;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public PlayStatus getStatus() {
        UserAuthentication user;
        ProfileManager profileManager = this.launcher.getProfileManager();
        Profile profile = profileManager.getProfiles().isEmpty() ? null : profileManager.getSelectedProfile();
        UserAuthentication userAuthentication = user = profileManager.getSelectedUser() == null ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        if (user == null || !user.isLoggedIn() || profile == null || this.launcher.getLauncher().getVersionManager().getVersions(profile.getVersionFilter()).isEmpty()) {
            return PlayStatus.LOADING;
        }
        this.lock.lock();
        try {
            if (this.downloadInProgress) {
                PlayStatus playStatus = PlayStatus.DOWNLOADING;
                return playStatus;
            }
            if (this.instances.containsKey(user)) {
                PlayStatus playStatus = PlayStatus.ALREADY_PLAYING;
                return playStatus;
            }
        }
        finally {
            this.lock.unlock();
        }
        if (user.getSelectedProfile() == null) {
            return PlayStatus.CAN_PLAY_DEMO;
        }
        if (user.canPlayOnline()) {
            return PlayStatus.CAN_PLAY_ONLINE;
        }
        return PlayStatus.CAN_PLAY_OFFLINE;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public GameInstanceStatus getInstanceStatus() {
        ProfileManager profileManager = this.launcher.getProfileManager();
        UserAuthentication user = profileManager.getSelectedUser() == null ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        this.lock.lock();
        try {
            GameRunner gameRunner = this.instances.get(user);
            if (gameRunner != null) {
                GameInstanceStatus gameInstanceStatus = gameRunner.getStatus();
                return gameInstanceStatus;
            }
        }
        finally {
            this.lock.unlock();
        }
        return GameInstanceStatus.IDLE;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void play() {
        final ProfileManager profileManager = this.launcher.getProfileManager();
        final Profile profile = profileManager.getSelectedProfile();
        final UserAuthentication user = (profileManager.getSelectedUser() == null) ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        final String lastVersionId = profile.getLastVersionId();
        final MinecraftGameRunner gameRunner = new MinecraftGameRunner(this.launcher, this.additionalLaunchArgs);
        gameRunner.setStatus(GameInstanceStatus.PREPARING);
        this.lock.lock();
        try {
            if (this.instances.containsKey(user) || this.downloadInProgress) {
                return;
            }
            this.instances.put(user, gameRunner);
            this.downloadInProgress = true;
        } finally {
            this.lock.unlock();
        }
        this.launcher.getLauncher().getVersionManager().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                gameRunner.setVisibility(Objects.firstNonNull(profile.getLauncherVisibilityOnGameClose(), Profile.DEFAULT_LAUNCHER_VISIBILITY));
                VersionSyncInfo syncInfo = null;
                if (lastVersionId != null) {
                    syncInfo = GameLaunchDispatcher.this.launcher.getLauncher().getVersionManager().getVersionSyncInfo(
                            lastVersionId);
                }
                if (syncInfo == null || syncInfo.getLatestVersion() == null) {
                    syncInfo = GameLaunchDispatcher.this.launcher.getLauncher().getVersionManager().getVersions(
                            profile.getVersionFilter()).get(0);
                }
                gameRunner.setStatus(GameInstanceStatus.IDLE);
                gameRunner.addListener(GameLaunchDispatcher.this);
                gameRunner.playGame(syncInfo);
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void onGameInstanceChangedState(GameRunner runner, GameInstanceStatus status) {
        this.lock.lock();
        try {
            if (status == GameInstanceStatus.IDLE) {
                this.instances.inverse().remove(runner);
            }
            this.downloadInProgress = false;
            for (GameRunner instance : this.instances.values()) {
                if (instance.getStatus() == GameInstanceStatus.PLAYING) continue;
                this.downloadInProgress = true;
                break;
            }
            this.launcher.getUserInterface().updatePlayState();
        }
        finally {
            this.lock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isRunningInSameFolder() {
        this.lock.lock();
        try {
            File currentGameDir = Objects.firstNonNull(this.launcher.getProfileManager().getSelectedProfile().getGameDir(), this.launcher.getLauncher().getWorkingDirectory());
            for (MinecraftGameRunner runner : this.instances.values()) {
                File otherGameDir;
                Profile profile = runner.getSelectedProfile();
                if (profile == null || !currentGameDir.equals(otherGameDir = Objects.firstNonNull(profile.getGameDir(), this.launcher.getLauncher().getWorkingDirectory()))) continue;
                boolean bl = true;
                return bl;
            }
        }
        finally {
            this.lock.unlock();
        }
        return false;
    }

    public static enum PlayStatus {
        LOADING("Loading...", false),
        CAN_PLAY_DEMO("Play Demo", true),
        CAN_PLAY_ONLINE("Play", true),
        CAN_PLAY_OFFLINE("Play Offline", true),
        ALREADY_PLAYING("Already Playing...", false),
        DOWNLOADING("Installing...", false);
        
        private final String name;
        private final boolean canPlay;

        private PlayStatus(String name, boolean canPlay) {
            this.name = name;
            this.canPlay = canPlay;
        }

        public String getName() {
            return this.name;
        }

        public boolean canPlay() {
            return this.canPlay;
        }
    }

}

