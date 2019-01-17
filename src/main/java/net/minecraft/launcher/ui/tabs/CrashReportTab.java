package net.minecraft.launcher.ui.tabs;

import com.mojang.launcher.Http;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.versions.CompleteVersion;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.minecraft.hopper.HopperService;
import net.minecraft.hopper.Problem;
import net.minecraft.hopper.PublishResponse;
import net.minecraft.hopper.Report;
import net.minecraft.hopper.SubmitResponse;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.MinecraftUserInterface;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CrashReportTab
extends JPanel {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Launcher minecraftLauncher;
    private final CompleteVersion version;
    private final File reportFile;
    private final String report;
    private final JEditorPane reportEditor = new JEditorPane();
    private final JScrollPane scrollPane = new JScrollPane(this.reportEditor);
    private final CrashInfoPane crashInfoPane;
    private final boolean isModded;
    private SubmitResponse hopperServiceResponse = null;

    public CrashReportTab(final Launcher minecraftLauncher, final CompleteVersion version, File reportFile, final String report) {
        super(true);
        this.minecraftLauncher = minecraftLauncher;
        this.version = version;
        this.reportFile = reportFile;
        this.report = report;
        this.crashInfoPane = new CrashInfoPane(minecraftLauncher);
        this.isModded = !report.contains("Is Modded: Probably not") && !report.contains("Is Modded: Unknown");
        this.setLayout(new BorderLayout());
        this.createInterface();
        if (minecraftLauncher.getProfileManager().getSelectedProfile().getUseHopperCrashService()) {
            minecraftLauncher.getLauncher().getVersionManager().getExecutorService().submit(new Runnable(){

                @Override
                public void run() {
                    try {
                        HashMap<String, String> environment = new HashMap<String, String>();
                        environment.put("launcher.version", LauncherConstants.getVersionName());
                        environment.put("launcher.title", minecraftLauncher.getUserInterface().getTitle());
                        environment.put("bootstrap.version", String.valueOf(minecraftLauncher.getBootstrapVersion()));
                        CrashReportTab.this.hopperServiceResponse = HopperService.submitReport(minecraftLauncher.getLauncher().getProxy(), report, "Minecraft", version.getId(), environment);
                        LOGGER.info("Reported crash to Mojang (ID " + CrashReportTab.this.hopperServiceResponse.getReport().getId() + ")");
                        if (CrashReportTab.this.hopperServiceResponse.getProblem() != null) {
                            CrashReportTab.this.showKnownProblemPopup();
                        } else if (CrashReportTab.this.hopperServiceResponse.getReport().canBePublished()) {
                            CrashReportTab.this.showPublishReportPrompt();
                        }
                    }
                    catch (IOException e) {
                        LOGGER.error("Couldn't report crash to Mojang", (Throwable)e);
                    }
                }
            });
        }
    }

    private void showPublishReportPrompt() {
        Object[] options = new String[]{"Publish Crash Report", "Cancel"};
        JLabel message = new JLabel();
        message.setText("<html><p>Sorry, but it looks like the game crashed and we don't know why.</p><p>Would you mind publishing this report so that " + (this.isModded ? "the mod authors" : "Mojang") + " can fix it?</p></html>");
        int result = JOptionPane.showOptionDialog(this, message, "Uhoh, something went wrong!", 0, 1, null, options, options[0]);
        if (result == 0) {
            try {
                PublishResponse publishResponse = HopperService.publishReport(this.minecraftLauncher.getLauncher().getProxy(), this.hopperServiceResponse.getReport());
            }
            catch (IOException e) {
                LOGGER.error("Couldn't publish report " + this.hopperServiceResponse.getReport().getId(), (Throwable)e);
            }
        }
    }

    private void showKnownProblemPopup() {
        if (this.hopperServiceResponse.getProblem().getUrl() == null) {
            JOptionPane.showMessageDialog(this, this.hopperServiceResponse.getProblem().getDescription(), this.hopperServiceResponse.getProblem().getTitle(), 1);
        } else {
            Object[] options = new String[]{"Fix The Problem", "Cancel"};
            int result = JOptionPane.showOptionDialog(this, this.hopperServiceResponse.getProblem().getDescription(), this.hopperServiceResponse.getProblem().getTitle(), 0, 1, null, options, options[0]);
            if (result == 0) {
                try {
                    OperatingSystem.openLink(new URI(this.hopperServiceResponse.getProblem().getUrl()));
                }
                catch (URISyntaxException e) {
                    LOGGER.error("Couldn't open help page ( " + this.hopperServiceResponse.getProblem().getUrl() + "  ) for crash", (Throwable)e);
                }
            }
        }
    }

    protected void createInterface() {
        this.add((Component)this.crashInfoPane, "North");
        this.add((Component)this.scrollPane, "Center");
        this.reportEditor.setText(this.report);
        this.crashInfoPane.createInterface();
    }

    private class CrashInfoPane
    extends JPanel
    implements ActionListener {
        public static final String INFO_NORMAL = "<html><div style='width: 100%'><p><b>Uhoh, it looks like the game has crashed! Sorry for the inconvenience :(</b></p><p>Using magic and love, we've managed to gather some details about the crash and we will investigate this as soon as we can.</p><p>You can see the full report below.</p></div></html>";
        public static final String INFO_MODDED = "<html><div style='width: 100%'><p><b>Uhoh, it looks like the game has crashed! Sorry for the inconvenience :(</b></p><p>We think your game may be modded, and as such we can't accept this crash report.</p><p>However, if you do indeed use mods, please send this to the mod authors to take a look at!</p></div></html>";
        private final JButton submitButton = new JButton("Report to Mojang");
        private final JButton openFileButton = new JButton("Open report file");

        protected CrashInfoPane(Launcher minecraftLauncher) {
            this.submitButton.addActionListener(this);
            this.openFileButton.addActionListener(this);
        }

        protected void createInterface() {
            this.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.anchor = 13;
            constraints.fill = 2;
            constraints.insets = new Insets(2, 2, 2, 2);
            constraints.gridx = 1;
            this.add((Component)this.submitButton, constraints);
            constraints.gridy = 1;
            this.add((Component)this.openFileButton, constraints);
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;
            constraints.gridheight = 2;
            this.add((Component)new JLabel(CrashReportTab.this.isModded ? INFO_MODDED : INFO_NORMAL), constraints);
            if (CrashReportTab.this.isModded) {
                this.submitButton.setEnabled(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == this.submitButton) {
                if (CrashReportTab.this.hopperServiceResponse != null) {
                    if (CrashReportTab.this.hopperServiceResponse.getProblem() != null) {
                        CrashReportTab.this.showKnownProblemPopup();
                    } else if (CrashReportTab.this.hopperServiceResponse.getReport().canBePublished()) {
                        CrashReportTab.this.showPublishReportPrompt();
                    }
                } else {
                    try {
                        HashMap<String, Object> args = new HashMap<String, Object>();
                        args.put("pid", 10400);
                        args.put("issuetype", 1);
                        args.put("description", "Put the summary of the bug you're having here\n\n*What I expected to happen was...:*\nDescribe what you thought should happen here\n\n*What actually happened was...:*\nDescribe what happened here\n\n*Steps to Reproduce:*\n1. Put a step by step guide on how to trigger the bug here\n2. ...\n3. ...");
                        args.put("environment", this.buildEnvironmentInfo());
                        OperatingSystem.openLink(URI.create("https://bugs.mojang.com/secure/CreateIssueDetails!init.jspa?" + Http.buildQuery(args)));
                    }
                    catch (Throwable ex) {
                        LOGGER.error("Couldn't open bugtracker", ex);
                    }
                }
            } else if (e.getSource() == this.openFileButton) {
                OperatingSystem.openLink(CrashReportTab.this.reportFile.toURI());
            }
        }

        private String buildEnvironmentInfo() {
            StringBuilder result = new StringBuilder();
            result.append("OS: ");
            result.append(System.getProperty("os.name"));
            result.append(" (ver ");
            result.append(System.getProperty("os.version"));
            result.append(", arch ");
            result.append(System.getProperty("os.arch"));
            result.append(")\nJava: ");
            result.append(System.getProperty("java.version"));
            result.append(" (by ");
            result.append(System.getProperty("java.vendor"));
            result.append(")\nLauncher: ");
            result.append(CrashReportTab.this.minecraftLauncher.getUserInterface().getTitle());
            result.append(" (bootstrap ");
            result.append(CrashReportTab.this.minecraftLauncher.getBootstrapVersion());
            result.append(")\nMinecraft: ");
            result.append(CrashReportTab.this.version.getId());
            result.append(" (updated ");
            result.append(CrashReportTab.this.version.getUpdatedTime());
            result.append(")");
            return result.toString();
        }
    }

}

