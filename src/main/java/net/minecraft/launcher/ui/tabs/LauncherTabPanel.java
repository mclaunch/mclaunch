package net.minecraft.launcher.ui.tabs;

import java.awt.Component;
import javax.swing.JTabbedPane;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.ui.tabs.ConsoleTab;
import net.minecraft.launcher.ui.tabs.CrashReportTab;
import net.minecraft.launcher.ui.tabs.ProfileListTab;
import net.minecraft.launcher.ui.tabs.WebsiteTab;

public class LauncherTabPanel
extends JTabbedPane {
    private final Launcher minecraftLauncher;
    private final WebsiteTab blog;
    private final ConsoleTab console;
    private CrashReportTab crashReportTab;

    public LauncherTabPanel(Launcher minecraftLauncher) {
        super(1);
        this.minecraftLauncher = minecraftLauncher;
        this.blog = new WebsiteTab(minecraftLauncher);
        this.console = new ConsoleTab(minecraftLauncher);
        this.createInterface();
    }

    protected void createInterface() {
        this.addTab("Update Notes", this.blog);
        this.addTab("Launcher Log", this.console);
        this.addTab("Profile Editor", new ProfileListTab(this.minecraftLauncher));
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public WebsiteTab getBlog() {
        return this.blog;
    }

    public ConsoleTab getConsole() {
        return this.console;
    }

    public void showConsole() {
        this.setSelectedComponent(this.console);
    }

    public void setCrashReport(CrashReportTab newTab) {
        if (this.crashReportTab != null) {
            this.removeTab(this.crashReportTab);
        }
        this.crashReportTab = newTab;
        this.addTab("Crash Report", this.crashReportTab);
        this.setSelectedComponent(newTab);
    }

    protected void removeTab(Component tab) {
        for (int i = 0; i < this.getTabCount(); ++i) {
            if (this.getTabComponentAt(i) != tab) continue;
            this.removeTabAt(i);
            break;
        }
    }

    public void removeTab(String name) {
        int index = this.indexOfTab(name);
        if (index > -1) {
            this.removeTabAt(index);
        }
    }
}

