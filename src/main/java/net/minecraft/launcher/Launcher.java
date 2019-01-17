package net.minecraft.launcher;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.UserInterface;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.VersionFilter;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.ReleaseTypeFactory;
import com.mojang.launcher.versions.Version;
import com.mojang.util.UUIDTypeAdapter;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import javax.swing.JFrame;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;
import net.minecraft.launcher.CompatibilityRule;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.MinecraftUserInterface;
import net.minecraft.launcher.SwingUserInterface;
import net.minecraft.launcher.game.GameLaunchDispatcher;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.game.MinecraftReleaseTypeFactory;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.updater.AssetIndexInfo;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.Library;
import net.minecraft.launcher.updater.LocalVersionList;
import net.minecraft.launcher.updater.MinecraftVersionManager;
import net.minecraft.launcher.updater.RemoteVersionList;
import net.minecraft.launcher.updater.VersionList;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Launcher {
    private static Launcher INSTANCE;
    private static final Logger LOGGER;
    private final com.mojang.launcher.Launcher launcher;
    private final Integer bootstrapVersion;
    private final MinecraftUserInterface userInterface;
    private final ProfileManager profileManager;
    private final Gson gson = new Gson();
    private final GameLaunchDispatcher launchDispatcher;
    private boolean winTenHack = false;
    private UUID clientToken = UUID.randomUUID();
    private String requestedUser;

    public static Launcher getCurrentInstance() {
        return INSTANCE;
    }

    public Launcher(JFrame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args) {
        this(frame, workingDirectory, proxy, proxyAuth, args, 0);
    }

    public Launcher(JFrame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args, Integer bootstrapVersion) {
        INSTANCE = this;
        this.setupErrorHandling();
        this.bootstrapVersion = bootstrapVersion;
        this.userInterface = this.selectUserInterface(frame);
        if (bootstrapVersion < 4) {
            this.userInterface.showOutdatedNotice();
            System.exit(0);
            throw new Error("Outdated bootstrap");
        }
        LOGGER.info(this.userInterface.getTitle() + " (through bootstrap " + bootstrapVersion + ") started on " + OperatingSystem.getCurrentPlatform().getName() + "...");
        LOGGER.info("Current time is " + DateFormat.getDateTimeInstance(2, 2, Locale.US).format(new Date()));
        if (!OperatingSystem.getCurrentPlatform().isSupported()) {
            LOGGER.fatal("This operating system is unknown or unsupported, we cannot guarantee that the game will launch successfully.");
        }
        LOGGER.info("System.getProperty('os.name') == '" + System.getProperty("os.name") + "'");
        LOGGER.info("System.getProperty('os.version') == '" + System.getProperty("os.version") + "'");
        LOGGER.info("System.getProperty('os.arch') == '" + System.getProperty("os.arch") + "'");
        LOGGER.info("System.getProperty('java.version') == '" + System.getProperty("java.version") + "'");
        LOGGER.info("System.getProperty('java.vendor') == '" + System.getProperty("java.vendor") + "'");
        LOGGER.info("System.getProperty('sun.arch.data.model') == '" + System.getProperty("sun.arch.data.model") + "'");
        LOGGER.info("proxy == " + proxy);
        this.launchDispatcher = new GameLaunchDispatcher(this, this.processArgs(args));
        this.launcher = new com.mojang.launcher.Launcher(this.userInterface, workingDirectory, proxy, proxyAuth, new MinecraftVersionManager(new LocalVersionList(workingDirectory), new RemoteVersionList(LauncherConstants.PROPERTIES.getVersionManifest(), proxy)), Agent.MINECRAFT, MinecraftReleaseTypeFactory.instance(), 21);
        this.profileManager = new ProfileManager(this);
        ((SwingUserInterface)this.userInterface).initializeFrame();
        this.refreshVersionsAndProfiles();
    }

    public File findNativeLauncher() {
        File shortcut;
        String programData = System.getenv("ProgramData");
        if (programData == null) {
            programData = System.getenv("ALLUSERSPROFILE");
        }
        if (programData != null && (shortcut = new File(programData, "Microsoft\\Windows\\Start Menu\\Programs\\Minecraft\\Minecraft.lnk")).isFile()) {
            return shortcut;
        }
        return null;
    }

    public void runNativeLauncher(File executable, String[] args) {
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", executable.getAbsolutePath());
        try {
            pb.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private void setupErrorHandling() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOGGER.fatal("Unhandled exception in thread " + t, e);
            }
        });
    }

    private String[] processArgs(String[] args) {
        OptionSet optionSet;
        OptionParser optionParser = new OptionParser();
        optionParser.allowsUnrecognizedOptions();
        ArgumentAcceptingOptionSpec<String> userOption = optionParser.accepts("user").withRequiredArg().ofType(String.class);
        NonOptionArgumentSpec<String> nonOptions = optionParser.nonOptions();
        try {
            optionSet = optionParser.parse(args);
        }
        catch (OptionException e) {
            return args;
        }
        if (optionSet.has(userOption)) {
            this.requestedUser = optionSet.valueOf(userOption);
        }
        List<String> remainingOptions = optionSet.valuesOf(nonOptions);
        return remainingOptions.toArray(new String[remainingOptions.size()]);
    }

    public void refreshVersionsAndProfiles() {
        this.getLauncher().getVersionManager().getExecutorService().submit(new Runnable(){

            @Override
            public void run() {
                try {
                    Launcher.this.getLauncher().getVersionManager().refreshVersions();
                }
                catch (Throwable e) {
                    LOGGER.error("Unexpected exception refreshing version list", e);
                }
                try {
                    Launcher.this.profileManager.loadProfiles();
                    LOGGER.info("Loaded " + Launcher.this.profileManager.getProfiles().size() + " profile(s); selected '" + Launcher.this.profileManager.getSelectedProfile().getName() + "'");
                }
                catch (Throwable e) {
                    LOGGER.error("Unexpected exception refreshing profile list", e);
                }
                if (Launcher.this.requestedUser != null) {
                    AuthenticationDatabase authDatabase = Launcher.this.profileManager.getAuthDatabase();
                    boolean loggedIn = false;
                    try {
                        String uuid = UUIDTypeAdapter.fromUUID(UUIDTypeAdapter.fromString(Launcher.this.requestedUser));
                        UserAuthentication auth = authDatabase.getByUUID(uuid);
                        if (auth != null) {
                            Launcher.this.profileManager.setSelectedUser(uuid);
                            loggedIn = true;
                        }
                    }
                    catch (RuntimeException uuid) {
                        // empty catch block
                    }
                    if (!loggedIn && authDatabase.getByName(Launcher.this.requestedUser) != null) {
                        UserAuthentication auth = authDatabase.getByName(Launcher.this.requestedUser);
                        if (auth.getSelectedProfile() != null) {
                            Launcher.this.profileManager.setSelectedUser(UUIDTypeAdapter.fromUUID(auth.getSelectedProfile().getId()));
                        } else {
                            Launcher.this.profileManager.setSelectedUser("demo-" + auth.getUserID());
                        }
                    }
                }
                Launcher.this.ensureLoggedIn();
            }
        });
    }

    private MinecraftUserInterface selectUserInterface(JFrame frame) {
        return new SwingUserInterface(this, frame);
    }

    public com.mojang.launcher.Launcher getLauncher() {
        return this.launcher;
    }

    public MinecraftUserInterface getUserInterface() {
        return this.userInterface;
    }

    public Integer getBootstrapVersion() {
        return this.bootstrapVersion;
    }

    public void ensureLoggedIn() {
        UserAuthentication auth = this.profileManager.getAuthDatabase().getByUUID(this.profileManager.getSelectedUser());
        if (auth == null) {
            this.getUserInterface().showLoginPrompt();
        } else if (!auth.isLoggedIn()) {
            if (auth.canLogIn()) {
                try {
                    auth.logIn();
                    try {
                        this.profileManager.saveProfiles();
                    }
                    catch (IOException e) {
                        LOGGER.error("Couldn't save profiles after refreshing auth!", (Throwable)e);
                    }
                    this.profileManager.fireRefreshEvent();
                }
                catch (AuthenticationException e) {
                    LOGGER.error("Exception whilst logging into profile", (Throwable)e);
                    this.getUserInterface().showLoginPrompt();
                }
            } else {
                this.getUserInterface().showLoginPrompt();
            }
        } else if (!auth.canPlayOnline()) {
            try {
                LOGGER.info("Refreshing auth...");
                auth.logIn();
                try {
                    this.profileManager.saveProfiles();
                }
                catch (IOException e) {
                    LOGGER.error("Couldn't save profiles after refreshing auth!", (Throwable)e);
                }
                this.profileManager.fireRefreshEvent();
            }
            catch (InvalidCredentialsException e) {
                LOGGER.error("Exception whilst logging into profile", (Throwable)e);
                this.getUserInterface().showLoginPrompt();
            }
            catch (AuthenticationException e) {
                LOGGER.error("Exception whilst logging into profile", (Throwable)e);
            }
        }
    }

    public UUID getClientToken() {
        return this.clientToken;
    }

    public void setClientToken(UUID clientToken) {
        this.clientToken = clientToken;
    }

    public void cleanupOrphanedAssets() throws IOException {
        File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        File indexDir = new File(assetsDir, "indexes");
        File objectsDir = new File(assetsDir, "objects");
        HashSet<String> referencedObjects = Sets.newHashSet();
        if (!objectsDir.isDirectory()) {
            return;
        }
        for (VersionSyncInfo syncInfo : this.getLauncher().getVersionManager().getInstalledVersions()) {
            if (!(syncInfo.getLocalVersion() instanceof CompleteMinecraftVersion)) continue;
            CompleteMinecraftVersion version = (CompleteMinecraftVersion)syncInfo.getLocalVersion();
            String assetVersion = version.getAssetIndex().getId();
            File indexFile = new File(indexDir, assetVersion + ".json");
            AssetIndex index = this.gson.fromJson(FileUtils.readFileToString(indexFile, Charsets.UTF_8), AssetIndex.class);
            for (AssetIndex.AssetObject object : index.getUniqueObjects().keySet()) {
                referencedObjects.add(object.getHash().toLowerCase());
            }
        }
        File[] directories = objectsDir.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
        if (directories != null) {
            for (File directory : directories) {
                File[] files = directory.listFiles((FileFilter)FileFileFilter.FILE);
                if (files == null) continue;
                for (File file : files) {
                    if (referencedObjects.contains(file.getName().toLowerCase())) continue;
                    LOGGER.info("Cleaning up orphaned object {}", file.getName());
                    FileUtils.deleteQuietly(file);
                }
            }
        }
        Launcher.deleteEmptyDirectories(objectsDir);
    }

    public void cleanupOrphanedLibraries() throws IOException {
        File librariesDir = new File(this.getLauncher().getWorkingDirectory(), "libraries");
        HashSet<File> referencedLibraries = Sets.newHashSet();
        if (!librariesDir.isDirectory()) {
            return;
        }
        for (VersionSyncInfo syncInfo : this.getLauncher().getVersionManager().getInstalledVersions()) {
            if (!(syncInfo.getLocalVersion() instanceof CompleteMinecraftVersion)) continue;
            CompleteMinecraftVersion version = (CompleteMinecraftVersion)syncInfo.getLocalVersion();
            for (Library library : version.getRelevantLibraries(version.createFeatureMatcher())) {
                String file = null;
                if (library.getNatives() != null) {
                    String natives = library.getNatives().get((Object)OperatingSystem.getCurrentPlatform());
                    if (natives != null) {
                        file = library.getArtifactPath(natives);
                    }
                } else {
                    file = library.getArtifactPath();
                }
                if (file == null) continue;
                referencedLibraries.add(new File(librariesDir, file));
                referencedLibraries.add(new File(librariesDir, file + ".sha"));
            }
        }
        Collection<File> libraries = FileUtils.listFiles(librariesDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
        if (libraries != null) {
            for (File file : libraries) {
                if (referencedLibraries.contains(file)) continue;
                LOGGER.info("Cleaning up orphaned library {}", file);
                FileUtils.deleteQuietly(file);
            }
        }
        Launcher.deleteEmptyDirectories(librariesDir);
    }

    public void cleanupOldSkins() {
        File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        File skinsDir = new File(assetsDir, "skins");
        if (!skinsDir.isDirectory()) {
            return;
        }
        Collection<File> files = FileUtils.listFiles(skinsDir, new AgeFileFilter(System.currentTimeMillis() - 604800000L), TrueFileFilter.TRUE);
        if (files != null) {
            for (File file : files) {
                LOGGER.info("Cleaning up old skin {}", file.getName());
                FileUtils.deleteQuietly(file);
            }
        }
        Launcher.deleteEmptyDirectories(skinsDir);
    }

    public void cleanupOldVirtuals() throws IOException {
        File assetsDir = new File(this.getLauncher().getWorkingDirectory(), "assets");
        File virtualsDir = new File(assetsDir, "virtual");
        DateTypeAdapter dateAdapter = new DateTypeAdapter();
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, -5);
        Date cutoff = calendar.getTime();
        if (!virtualsDir.isDirectory()) {
            return;
        }
        File[] directories = virtualsDir.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
        if (directories != null) {
            for (File directory : directories) {
                File lastUsedFile = new File(directory, ".lastused");
                if (lastUsedFile.isFile()) {
                    Date lastUsed = dateAdapter.deserializeToDate(FileUtils.readFileToString(lastUsedFile));
                    if (!cutoff.after(lastUsed)) continue;
                    LOGGER.info("Cleaning up old virtual directory {}", directory);
                    FileUtils.deleteQuietly(directory);
                    continue;
                }
                LOGGER.info("Cleaning up strange virtual directory {}", directory);
                FileUtils.deleteQuietly(directory);
            }
        }
        Launcher.deleteEmptyDirectories(virtualsDir);
    }

    public void cleanupOldNatives() {
        File root = new File(this.launcher.getWorkingDirectory(), "versions/");
        LOGGER.info("Looking for old natives & assets to clean up...");
        AgeFileFilter ageFilter = new AgeFileFilter(System.currentTimeMillis() - 3600000L);
        if (!root.isDirectory()) {
            return;
        }
        File[] versions = root.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
        if (versions != null) {
            for (File version : versions) {
                File[] files = version.listFiles((FilenameFilter)FileFilterUtils.and(new PrefixFileFilter(version.getName() + "-natives-"), ageFilter));
                if (files == null) continue;
                for (File folder : files) {
                    LOGGER.debug("Deleting " + folder);
                    FileUtils.deleteQuietly(folder);
                }
            }
        }
    }

    public void cleanupOrphanedVersions() {
        Version version;
        LOGGER.info("Looking for orphaned versions to clean up...");
        HashSet<String> referencedVersions = Sets.newHashSet();
        for (Profile profile : this.getProfileManager().getProfiles().values()) {
            String lastVersionId = profile.getLastVersionId();
            VersionSyncInfo syncInfo = null;
            if (lastVersionId != null) {
                syncInfo = this.getLauncher().getVersionManager().getVersionSyncInfo(lastVersionId);
            }
            if (syncInfo == null || syncInfo.getLatestVersion() == null) {
                syncInfo = this.getLauncher().getVersionManager().getVersions(profile.getVersionFilter()).get(0);
            }
            if (syncInfo == null) continue;
            version = syncInfo.getLatestVersion();
            referencedVersions.add(version.getId());
            if (!(version instanceof CompleteMinecraftVersion)) continue;
            CompleteMinecraftVersion completeMinecraftVersion = (CompleteMinecraftVersion)version;
            referencedVersions.add(completeMinecraftVersion.getInheritsFrom());
            referencedVersions.add(completeMinecraftVersion.getJar());
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, -7);
        Date cutoff = calendar.getTime();
        for (VersionSyncInfo versionSyncInfo : this.getLauncher().getVersionManager().getInstalledVersions()) {
            if (!(versionSyncInfo.getLocalVersion() instanceof CompleteMinecraftVersion) || referencedVersions.contains((version = (CompleteVersion)versionSyncInfo.getLocalVersion()).getId()) || version.getType() != MinecraftReleaseType.SNAPSHOT) continue;
            if (versionSyncInfo.isOnRemote()) {
                LOGGER.info("Deleting orphaned version {} because it's a snapshot available on remote", version.getId());
                try {
                    this.getLauncher().getVersionManager().uninstallVersion((CompleteVersion)version);
                }
                catch (IOException e) {
                    LOGGER.warn("Couldn't uninstall version " + version.getId(), (Throwable)e);
                }
                continue;
            }
            if (!version.getUpdatedTime().before(cutoff)) continue;
            LOGGER.info("Deleting orphaned version {} because it's an unsupported old snapshot", version.getId());
            try {
                this.getLauncher().getVersionManager().uninstallVersion((CompleteVersion)version);
            }
            catch (IOException e) {
                LOGGER.warn("Couldn't uninstall version " + version.getId(), (Throwable)e);
            }
        }
    }

    private static Collection<File> listEmptyDirectories(File directory) {
        ArrayList<File> result = Lists.newArrayList();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) continue;
                File[] subFiles = file.listFiles();
                if (subFiles == null || subFiles.length == 0) {
                    result.add(file);
                    continue;
                }
                result.addAll(Launcher.listEmptyDirectories(file));
            }
        }
        return result;
    }

    private static void deleteEmptyDirectories(File directory) {
        block0 : do {
            Collection<File> files;
            if ((files = Launcher.listEmptyDirectories(directory)).isEmpty()) {
                return;
            }
            Iterator<File> i = files.iterator();
            do {
                if (!i.hasNext()) continue block0;
                File file = i.next();
                if (!FileUtils.deleteQuietly(file)) break block0;
                LOGGER.info("Deleted empty directory {}", file);
            } while (true);
            //break;
        } while (true);
    }

    public void performCleanups() throws IOException {
        this.cleanupOrphanedVersions();
        this.cleanupOrphanedAssets();
        this.cleanupOldSkins();
        this.cleanupOldNatives();
        this.cleanupOldVirtuals();
    }

    public ProfileManager getProfileManager() {
        return this.profileManager;
    }

    public GameLaunchDispatcher getLaunchDispatcher() {
        return this.launchDispatcher;
    }

    public boolean usesWinTenHack() {
        return this.winTenHack;
    }

    public void setWinTenHack() {
        this.winTenHack = true;
    }

    static {
        Thread.currentThread().setContextClassLoader(Launcher.class.getClassLoader());
        LOGGER = LogManager.getLogger();
    }

}

