package net.minecraft.launcher.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.ui.bottombar.PlayButtonPanel;
import net.minecraft.launcher.ui.bottombar.PlayerInfoPanel;
import net.minecraft.launcher.ui.bottombar.ProfileSelectionPanel;

public class BottomBarPanel
extends JPanel {
    private final Launcher minecraftLauncher;
    private final ProfileSelectionPanel profileSelectionPanel;
    private final PlayerInfoPanel playerInfoPanel;
    private final PlayButtonPanel playButtonPanel;

    public BottomBarPanel(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        int border = 4;
        this.setBorder(new EmptyBorder(border, border, border, border));
        this.profileSelectionPanel = new ProfileSelectionPanel(minecraftLauncher);
        this.playerInfoPanel = new PlayerInfoPanel(minecraftLauncher);
        this.playButtonPanel = new PlayButtonPanel(minecraftLauncher);
        this.createInterface();
    }

    protected void createInterface() {
        this.setLayout(new GridLayout(1, 3));
        this.add(this.wrapSidePanel(this.profileSelectionPanel, 17));
        this.add(this.playButtonPanel);
        this.add(this.wrapSidePanel(this.playerInfoPanel, 13));
    }

    protected JPanel wrapSidePanel(JPanel target, int side) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = side;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        wrapper.add((Component)target, constraints);
        return wrapper;
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public ProfileSelectionPanel getProfileSelectionPanel() {
        return this.profileSelectionPanel;
    }

    public PlayerInfoPanel getPlayerInfoPanel() {
        return this.playerInfoPanel;
    }

    public PlayButtonPanel getPlayButtonPanel() {
        return this.playButtonPanel;
    }
}

