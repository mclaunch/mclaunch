package net.minecraft.launcher.ui.popups.profile;

import com.google.common.collect.Sets;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.VersionFilter;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.MinecraftUserInterface;
import net.minecraft.launcher.SwingUserInterface;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.ui.popups.profile.ProfileEditorPopup;

public class ProfileVersionPanel
extends JPanel
implements RefreshedVersionsListener {
    private final ProfileEditorPopup editor;
    private final JComboBox versionList = new JComboBox();
    private final List<ReleaseTypeCheckBox> customVersionTypes = new ArrayList<ReleaseTypeCheckBox>();

    public ProfileVersionPanel(ProfileEditorPopup editor) {
        this.editor = editor;
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createTitledBorder("Version Selection"));
        this.createInterface();
        this.addEventHandlers();
        List<VersionSyncInfo> versions = editor.getMinecraftLauncher().getLauncher().getVersionManager().getVersions(editor.getProfile().getVersionFilter());
        if (versions.isEmpty()) {
            editor.getMinecraftLauncher().getLauncher().getVersionManager().addRefreshedVersionsListener(this);
        } else {
            this.populateVersions(versions);
        }
    }

    protected void createInterface() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = 17;
        constraints.gridy = 0;
        for (MinecraftReleaseType type : MinecraftReleaseType.values()) {
            if (type.getDescription() == null) continue;
            ReleaseTypeCheckBox checkbox = new ReleaseTypeCheckBox(type);
            checkbox.setSelected(this.editor.getProfile().getVersionFilter().getTypes().contains(type));
            this.customVersionTypes.add(checkbox);
            constraints.fill = 2;
            constraints.weightx = 1.0;
            constraints.gridwidth = 0;
            this.add((Component)checkbox, constraints);
            constraints.gridwidth = 1;
            constraints.weightx = 0.0;
            constraints.fill = 0;
            ++constraints.gridy;
        }
        this.add((Component)new JLabel("Use version:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add((Component)this.versionList, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        ++constraints.gridy;
        this.versionList.setRenderer(new VersionListRenderer());
    }

    protected void addEventHandlers() {
        this.versionList.addItemListener(new ItemListener(){

            @Override
            public void itemStateChanged(ItemEvent e) {
                ProfileVersionPanel.this.updateVersionSelection();
            }
        });
        for (final ReleaseTypeCheckBox type : this.customVersionTypes) {
            type.addItemListener(new ItemListener(){
                private boolean isUpdating = false;

                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (this.isUpdating) {
                        return;
                    }
                    if (e.getStateChange() == 1 && type.getType().getPopupWarning() != null) {
                        int result = JOptionPane.showConfirmDialog(((SwingUserInterface)ProfileVersionPanel.this.editor.getMinecraftLauncher().getUserInterface()).getFrame(), type.getType().getPopupWarning() + "\n\nAre you sure you want to continue?");
                        this.isUpdating = true;
                        if (result == 0) {
                            type.setSelected(true);
                            ProfileVersionPanel.this.updateCustomVersionFilter();
                        } else {
                            type.setSelected(false);
                        }
                        this.isUpdating = false;
                    } else {
                        ProfileVersionPanel.this.updateCustomVersionFilter();
                    }
                }
            });
        }
    }

    private void updateCustomVersionFilter() {
        Profile profile = this.editor.getProfile();
        HashSet<MinecraftReleaseType> newTypes = Sets.newHashSet(Profile.DEFAULT_RELEASE_TYPES);
        for (ReleaseTypeCheckBox type : this.customVersionTypes) {
            if (type.isSelected()) {
                newTypes.add(type.getType());
                continue;
            }
            newTypes.remove(type.getType());
        }
        if (newTypes.equals(Profile.DEFAULT_RELEASE_TYPES)) {
            profile.setAllowedReleaseTypes(null);
        } else {
            profile.setAllowedReleaseTypes(newTypes);
        }
        this.populateVersions(this.editor.getMinecraftLauncher().getLauncher().getVersionManager().getVersions(this.editor.getProfile().getVersionFilter()));
        this.editor.getMinecraftLauncher().getLauncher().getVersionManager().removeRefreshedVersionsListener(this);
    }

    private void updateVersionSelection() {
        Object selection = this.versionList.getSelectedItem();
        if (selection instanceof VersionSyncInfo) {
            Version version = ((VersionSyncInfo)selection).getLatestVersion();
            this.editor.getProfile().setLastVersionId(version.getId());
        } else {
            this.editor.getProfile().setLastVersionId(null);
        }
    }

    private void populateVersions(List<VersionSyncInfo> versions) {
        String previous = this.editor.getProfile().getLastVersionId();
        VersionSyncInfo selected = null;
        this.versionList.removeAllItems();
        this.versionList.addItem("Use Latest Version");
        for (VersionSyncInfo version : versions) {
            if (version.getLatestVersion().getId().equals(previous)) {
                selected = version;
            }
            this.versionList.addItem(version);
        }
        if (selected == null && !versions.isEmpty()) {
            this.versionList.setSelectedIndex(0);
        } else {
            this.versionList.setSelectedItem(selected);
        }
    }

    @Override
    public void onVersionsRefreshed(final VersionManager manager) {
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                List<VersionSyncInfo> versions = manager.getVersions(ProfileVersionPanel.this.editor.getProfile().getVersionFilter());
                ProfileVersionPanel.this.populateVersions(versions);
                ProfileVersionPanel.this.editor.getMinecraftLauncher().getLauncher().getVersionManager().removeRefreshedVersionsListener(ProfileVersionPanel.this);
            }
        });
    }

    private static class ReleaseTypeCheckBox extends JCheckBox {
        private final MinecraftReleaseType type;

        private ReleaseTypeCheckBox(MinecraftReleaseType type) {
            super(type.getDescription());
            this.type = type;
        }

        public MinecraftReleaseType getType() {
            return this.type;
        }
    }

    private static class VersionListRenderer
    extends BasicComboBoxRenderer {
        private VersionListRenderer() {
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof VersionSyncInfo) {
                VersionSyncInfo syncInfo = (VersionSyncInfo)value;
                Version version = syncInfo.getLatestVersion();
                value = String.format("%s %s", version.getType().getName(), version.getId());
            }
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return this;
        }
    }

}

