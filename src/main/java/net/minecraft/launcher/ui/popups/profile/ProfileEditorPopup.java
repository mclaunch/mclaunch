package net.minecraft.launcher.ui.popups.profile;

import com.mojang.launcher.OperatingSystem;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.MinecraftUserInterface;
import net.minecraft.launcher.SwingUserInterface;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.ui.popups.profile.ProfileInfoPanel;
import net.minecraft.launcher.ui.popups.profile.ProfileJavaPanel;
import net.minecraft.launcher.ui.popups.profile.ProfileVersionPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProfileEditorPopup
extends JPanel
implements ActionListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Launcher minecraftLauncher;
    private final Profile originalProfile;
    private final Profile profile;
    private final JButton saveButton = new JButton("Save Profile");
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton browseButton = new JButton("Open Game Dir");
    private final ProfileInfoPanel profileInfoPanel;
    private final ProfileVersionPanel profileVersionPanel;
    private final ProfileJavaPanel javaInfoPanel;

    public ProfileEditorPopup(Launcher minecraftLauncher, Profile profile) {
        super(true);
        this.minecraftLauncher = minecraftLauncher;
        this.originalProfile = profile;
        this.profile = new Profile(profile);
        this.profileInfoPanel = new ProfileInfoPanel(this);
        this.profileVersionPanel = new ProfileVersionPanel(this);
        this.javaInfoPanel = new ProfileJavaPanel(this);
        this.saveButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        this.browseButton.addActionListener(this);
        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.setLayout(new BorderLayout(0, 5));
        this.createInterface();
    }

    protected void createInterface() {
        JPanel standardPanels = new JPanel(true);
        standardPanels.setLayout(new BoxLayout(standardPanels, 1));
        standardPanels.add(this.profileInfoPanel);
        standardPanels.add(this.profileVersionPanel);
        standardPanels.add(this.javaInfoPanel);
        this.add((Component)standardPanels, "Center");
        JPanel buttonPannel = new JPanel();
        buttonPannel.setLayout(new BoxLayout(buttonPannel, 0));
        buttonPannel.add(this.cancelButton);
        buttonPannel.add(Box.createGlue());
        buttonPannel.add(this.browseButton);
        buttonPannel.add(Box.createHorizontalStrut(5));
        buttonPannel.add(this.saveButton);
        this.add((Component)buttonPannel, "South");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.saveButton) {
            try {
                ProfileManager manager = this.minecraftLauncher.getProfileManager();
                Map<String, Profile> profiles = manager.getProfiles();
                String selected = manager.getSelectedProfile().getName();
                if (!this.originalProfile.getName().equals(this.profile.getName())) {
                    profiles.remove(this.originalProfile.getName());
                    while (profiles.containsKey(this.profile.getName())) {
                        this.profile.setName(this.profile.getName() + "_");
                    }
                }
                profiles.put(this.profile.getName(), this.profile);
                if (selected.equals(this.originalProfile.getName())) {
                    manager.setSelectedProfile(this.profile.getName());
                }
                manager.saveProfiles();
                manager.fireRefreshEvent();
            }
            catch (IOException ex) {
                LOGGER.error("Couldn't save profiles whilst editing " + this.profile.getName(), (Throwable)ex);
            }
            this.closeWindow();
        } else if (e.getSource() == this.browseButton) {
            OperatingSystem.openFolder(this.profile.getGameDir() == null ? this.minecraftLauncher.getLauncher().getWorkingDirectory() : this.profile.getGameDir());
        } else {
            this.closeWindow();
        }
    }

    private void closeWindow() {
        if (SwingUtilities.isEventDispatchThread()) {
            Window window = (Window)this.getTopLevelAncestor();
            window.dispatchEvent(new WindowEvent(window, 201));
        } else {
            SwingUtilities.invokeLater(new Runnable(){

                @Override
                public void run() {
                    ProfileEditorPopup.this.closeWindow();
                }
            });
        }
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public Profile getProfile() {
        return this.profile;
    }

    public static void showEditProfileDialog(Launcher minecraftLauncher, Profile profile) {
        JFrame frame = ((SwingUserInterface)minecraftLauncher.getUserInterface()).getFrame();
        JDialog dialog = new JDialog(frame, "Profile Editor", true);
        ProfileEditorPopup editor = new ProfileEditorPopup(minecraftLauncher, profile);
        dialog.add(editor);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

}

