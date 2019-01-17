package net.minecraft.launcher.ui;

import com.mojang.launcher.OperatingSystem;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.ui.BottomBarPanel;
import net.minecraft.launcher.ui.TexturedPanel;
import net.minecraft.launcher.ui.tabs.LauncherTabPanel;
import net.minecraft.launcher.ui.tabs.WebsiteTab;
import org.apache.commons.lang3.SystemUtils;

public class LauncherPanel extends JPanel {
    public static final String CARD_DIRT_BACKGROUND = "loading";
    public static final String CARD_LOGIN = "login";
    public static final String CARD_LAUNCHER = "launcher";
    private final CardLayout cardLayout;
    private final LauncherTabPanel tabPanel;
    private final BottomBarPanel bottomBar;
    private final JProgressBar progressBar;
    private final Launcher minecraftLauncher;
    private final JPanel loginPanel;
    private JLabel warningLabel;

    public LauncherPanel(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        this.cardLayout = new CardLayout();
        this.setLayout(this.cardLayout);
        this.progressBar = new JProgressBar();
        this.bottomBar = new BottomBarPanel(minecraftLauncher);
        this.tabPanel = new LauncherTabPanel(minecraftLauncher);
        this.loginPanel = new TexturedPanel("/dirt.png");
        this.createInterface();
    }

    protected void createInterface() {
        this.add((Component)this.createLauncherInterface(), CARD_LAUNCHER);
        this.add((Component)this.createDirtInterface(), CARD_DIRT_BACKGROUND);
        this.add((Component)this.createLoginInterface(), CARD_LOGIN);
    }

    protected JPanel createLauncherInterface() {
        String ver;
        String[] split;
        boolean upgradableOS;
        JPanel result = new JPanel(new BorderLayout());
        this.tabPanel.getBlog().setPage(LauncherConstants.URL_WEBSITE);
        boolean javaBootstrap = this.getMinecraftLauncher().getBootstrapVersion() < 100;
        boolean bl = upgradableOS = OperatingSystem.getCurrentPlatform() == OperatingSystem.WINDOWS;
        if (OperatingSystem.getCurrentPlatform() == OperatingSystem.OSX && (ver = SystemUtils.OS_VERSION) != null && !ver.isEmpty() && (split = ver.split("\\.", 3)).length >= 2) {
            try {
                int major = Integer.parseInt(split[0]);
                int minor = Integer.parseInt(split[1]);
                if (major == 10) {
                    upgradableOS = minor >= 8;
                } else if (major > 10) {
                    upgradableOS = true;
                }
            }
            catch (NumberFormatException major) {
                // empty catch block
            }
        }
        if (javaBootstrap && upgradableOS) {
            this.warningLabel = new JLabel();
            this.warningLabel.setForeground(Color.RED);
            this.warningLabel.setHorizontalAlignment(0);
            final URI url = OperatingSystem.getCurrentPlatform() == OperatingSystem.WINDOWS ? LauncherConstants.URL_UPGRADE_WINDOWS : LauncherConstants.URL_UPGRADE_OSX;
            if (SystemUtils.IS_JAVA_1_8) {
                if (OperatingSystem.getCurrentPlatform() == OperatingSystem.WINDOWS) {
                    this.warningLabel.setText("<html><p style='font-size: 1.1em'>You are running an old version of the launcher. Please consider <a href='" + url + "'>using the new launcher</a> which will improve the performance of both launcher and game.</p></html>");
                } else {
                    this.warningLabel.setText("<html><p style='font-size: 1em'>You are running an old version of the launcher. Please consider <a href='" + url + "'>using the new launcher</a> which will improve the performance of both launcher and game.</p></html>");
                }
            } else if (OperatingSystem.getCurrentPlatform() == OperatingSystem.WINDOWS) {
                this.warningLabel.setText("<html><p style='font-size: 1.1em'>You are running on an old version of Java. Please consider <a href='" + url + "'>using the new launcher</a> which doesn't require Java, as it will make your game faster.</p></html>");
            } else {
                this.warningLabel.setText("<html><p style='font-size: 1em'>You are running on an old version of Java. Please consider <a href='" + url + "'>using the new launcher</a> which doesn't require Java, as it will make your game faster.</p></html>");
            }
            result.add((Component)this.warningLabel, "North");
            result.addMouseListener(new MouseAdapter(){

                @Override
                public void mouseClicked(MouseEvent e) {
                    OperatingSystem.openLink(url);
                }
            });
        }
        JPanel center = new JPanel();
        center.setLayout(new BorderLayout());
        center.add((Component)this.tabPanel, "Center");
        center.add((Component)this.progressBar, "South");
        this.progressBar.setVisible(false);
        this.progressBar.setMinimum(0);
        this.progressBar.setMaximum(100);
        this.progressBar.setStringPainted(true);
        result.add((Component)center, "Center");
        result.add((Component)this.bottomBar, "South");
        return result;
    }

    protected JPanel createDirtInterface() {
        return new TexturedPanel("/dirt.png");
    }

    protected JPanel createLoginInterface() {
        this.loginPanel.setLayout(new GridBagLayout());
        return this.loginPanel;
    }

    public LauncherTabPanel getTabPanel() {
        return this.tabPanel;
    }

    public BottomBarPanel getBottomBar() {
        return this.bottomBar;
    }

    public JProgressBar getProgressBar() {
        return this.progressBar;
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public void setCard(String card, JPanel additional) {
        if (card.equals(CARD_LOGIN)) {
            this.loginPanel.removeAll();
            this.loginPanel.add(additional);
        }
        this.cardLayout.show(this, card);
    }

}

