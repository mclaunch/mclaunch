package net.minecraft.launcher.ui.popups.login;

import com.google.common.base.Objects;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.util.UUIDTypeAdapter;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.profile.AuthenticationDatabase;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.ui.popups.login.AuthErrorForm;
import net.minecraft.launcher.ui.popups.login.LogInPopup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExistingUserListForm
extends JPanel
implements ActionListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final LogInPopup popup;
    private final JComboBox userDropdown = new JComboBox();
    private final AuthenticationDatabase authDatabase;
    private final JButton playButton = new JButton("Play");
    private final JButton logOutButton = new JButton("Log Out");
    private final ProfileManager profileManager;

    public ExistingUserListForm(LogInPopup popup) {
        this.popup = popup;
        this.profileManager = popup.getMinecraftLauncher().getProfileManager();
        this.authDatabase = popup.getMinecraftLauncher().getProfileManager().getAuthDatabase();
        this.fillUsers();
        this.createInterface();
        this.playButton.addActionListener(this);
        this.logOutButton.addActionListener(this);
    }

    private void fillUsers() {
        for (String user : this.authDatabase.getKnownNames()) {
            this.userDropdown.addItem(user);
            if (this.profileManager.getSelectedUser() == null || !Objects.equal(this.authDatabase.getByUUID(this.profileManager.getSelectedUser()), this.authDatabase.getByName(user))) continue;
            this.userDropdown.setSelectedItem(user);
        }
    }

    protected void createInterface() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.gridx = 0;
        constraints.gridy = -1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        this.add(Box.createGlue());
        String currentUser = this.authDatabase.getKnownNames().size() == 1 ? this.authDatabase.getKnownNames().iterator().next() : "" + this.authDatabase.getKnownNames().size() + " different users";
        String thisOrThese = this.authDatabase.getKnownNames().size() == 1 ? "this account" : "one of these accounts";
        this.add((Component)new JLabel("You're already logged in as " + currentUser + "."), constraints);
        this.add((Component)new JLabel("You may use " + thisOrThese + " and skip authentication."), constraints);
        this.add(Box.createVerticalStrut(5), constraints);
        JLabel usernameLabel = new JLabel("Existing User:");
        Font labelFont = usernameLabel.getFont().deriveFont(1);
        usernameLabel.setFont(labelFont);
        this.add((Component)usernameLabel, constraints);
        constraints.gridwidth = 1;
        this.add((Component)this.userDropdown, constraints);
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(0, 5, 0, 0);
        this.add((Component)this.playButton, constraints);
        constraints.gridx = 2;
        this.add((Component)this.logOutButton, constraints);
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.weightx = 1.0;
        constraints.gridx = 0;
        constraints.gridy = -1;
        constraints.gridwidth = 2;
        this.add(Box.createVerticalStrut(5), constraints);
        this.add((Component)new JLabel("Alternatively, log in with a new account below:"), constraints);
        this.add((Component)new JPopupMenu.Separator(), constraints);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object selected = this.userDropdown.getSelectedItem();
        final UserAuthentication auth;
        final String uuid;
        if (selected != null && selected instanceof String) {
            auth = this.authDatabase.getByName((String) selected);
            if (auth.getSelectedProfile() == null) {
                uuid = "demo-" + auth.getUserID();
            } else {
                uuid = UUIDTypeAdapter.fromUUID(auth.getSelectedProfile().getId());
            }
        } else {
            auth = null;
            uuid = null;
        }
        if (e.getSource() == this.playButton) {
            this.popup.setCanLogIn(false);
            this.popup.getMinecraftLauncher().getLauncher().getVersionManager().getExecutorService().execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (auth != null && uuid != null) {
                                try {
                                    if (!auth.canPlayOnline()) {
                                        auth.logIn();
                                    }
                                    ExistingUserListForm.this.popup.setLoggedIn(uuid);
                                } catch (AuthenticationException ex) {
                                    ExistingUserListForm.this.popup.getErrorForm().displayError(ex,"We couldn't log you back in as " + selected + ".", "Please try to log in again.");
                                    ExistingUserListForm.this.removeUser((String) selected, uuid);
                                    ExistingUserListForm.this.popup.setCanLogIn(true);
                                }
                            } else {
                                ExistingUserListForm.this.popup.setCanLogIn(true);
                            }
                        }
                    });
        } else if (e.getSource() == this.logOutButton) {
            this.removeUser((String) selected, uuid);
        }
    }

    protected void removeUser(final String name, final String uuid) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable(){

                @Override
                public void run() {
                    ExistingUserListForm.this.removeUser(name, uuid);
                }
            });
        } else {
            this.userDropdown.removeItem(name);
            this.authDatabase.removeUUID(uuid);
            try {
                this.profileManager.saveProfiles();
            }
            catch (IOException e) {
                LOGGER.error("Couldn't save profiles whilst removing " + name + " / " + uuid + " from database", (Throwable)e);
            }
            if (this.userDropdown.getItemCount() == 0) {
                this.popup.remove(this);
            }
        }
    }

}

