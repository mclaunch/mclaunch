package net.minecraft.launcher.ui.bottombar;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.VersionFilter;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.MinecraftUserInterface;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.profile.RefreshedProfilesListener;
import net.minecraft.launcher.profile.UserChangedListener;

public class PlayerInfoPanel
extends JPanel
implements RefreshedVersionsListener,
RefreshedProfilesListener,
UserChangedListener {
    private final Launcher minecraftLauncher;
    private final JLabel welcomeText = new JLabel("", 0);
    private final JLabel versionText = new JLabel("", 0);
    private final JButton switchUserButton = new JButton("Switch User");

    public PlayerInfoPanel(final Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        minecraftLauncher.getProfileManager().addRefreshedProfilesListener(this);
        minecraftLauncher.getProfileManager().addUserChangedListener(this);
        this.checkState();
        this.createInterface();
        this.switchUserButton.setEnabled(false);
        this.switchUserButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                minecraftLauncher.getUserInterface().showLoginPrompt();
            }
        });
    }

    protected void createInterface() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        this.add((Component)this.welcomeText, constraints);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        ++constraints.gridy;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        this.add((Component)this.versionText, constraints);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        ++constraints.gridy;
        constraints.weightx = 0.5;
        constraints.fill = 0;
        this.add((Component)this.switchUserButton, constraints);
        constraints.weightx = 0.0;
        ++constraints.gridy;
    }

    @Override
    public void onProfilesRefreshed(ProfileManager manager) {
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                PlayerInfoPanel.this.checkState();
            }
        });
    }

    public void checkState() {
        UserAuthentication auth;
        VersionSyncInfo version;
        VersionSyncInfo requestedVersion;
        ProfileManager profileManager = this.minecraftLauncher.getProfileManager();
        UserAuthentication userAuthentication = auth = profileManager.getSelectedUser() == null ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        if (auth == null || !auth.isLoggedIn()) {
            this.welcomeText.setText("Welcome, guest! Please log in.");
        } else if (auth.getSelectedProfile() == null) {
            this.welcomeText.setText("<html>Welcome, player!</html>");
        } else {
            this.welcomeText.setText("<html>Welcome, <b>" + auth.getSelectedProfile().getName() + "</b></html>");
        }
        Profile profile = profileManager.getProfiles().isEmpty() ? null : profileManager.getSelectedProfile();
        List<VersionSyncInfo> versions = profile == null ? null : this.minecraftLauncher.getLauncher().getVersionManager().getVersions(profile.getVersionFilter());
        VersionSyncInfo versionSyncInfo = version = profile == null || versions.isEmpty() ? null : versions.get(0);
        if (profile != null && profile.getLastVersionId() != null && (requestedVersion = this.minecraftLauncher.getLauncher().getVersionManager().getVersionSyncInfo(profile.getLastVersionId())) != null && requestedVersion.getLatestVersion() != null) {
            version = requestedVersion;
        }
        if (version == null) {
            this.versionText.setText("Loading versions...");
        } else if (version.isUpToDate()) {
            this.versionText.setText("Ready to play Minecraft " + version.getLatestVersion().getId());
        } else if (version.isInstalled()) {
            this.versionText.setText("Ready to update & play Minecraft " + version.getLatestVersion().getId());
        } else if (version.isOnRemote()) {
            this.versionText.setText("Ready to download & play Minecraft " + version.getLatestVersion().getId());
        }
        this.switchUserButton.setEnabled(true);
    }

    @Override
    public void onVersionsRefreshed(VersionManager manager) {
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                PlayerInfoPanel.this.checkState();
            }
        });
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    @Override
    public void onUserChanged(ProfileManager manager) {
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                PlayerInfoPanel.this.checkState();
            }
        });
    }

}

