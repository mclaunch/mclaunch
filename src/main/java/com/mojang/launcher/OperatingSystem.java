package com.mojang.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum OperatingSystem {
    LINUX("linux", "linux", "unix"),
    WINDOWS("windows", "win"),
    OSX("osx", "mac"),
    UNKNOWN("unknown", new String[0]);
    
    private static final Logger LOGGER;
    private final String name;
    private final String[] aliases;

    private /* varargs */ OperatingSystem(String name, String ... aliases) {
        this.name = name;
        this.aliases = aliases == null ? new String[]{} : aliases;
    }

    public String getName() {
        return this.name;
    }

    public String[] getAliases() {
        return this.aliases;
    }

    public boolean isSupported() {
        return this != UNKNOWN;
    }

    public String getJavaDir() {
        String separator = System.getProperty("file.separator");
        String path = System.getProperty("java.home") + separator + "bin" + separator;
        if (OperatingSystem.getCurrentPlatform() == WINDOWS && new File(path + "javaw.exe").isFile()) {
            return path + "javaw.exe";
        }
        return path + "java";
    }

    public static OperatingSystem getCurrentPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        for (OperatingSystem os : OperatingSystem.values()) {
            for (String alias : os.getAliases()) {
                if (!osName.contains(alias)) continue;
                return os;
            }
        }
        return UNKNOWN;
    }

    public static void openLink(URI link) {
        try {
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            Object o = desktopClass.getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
            desktopClass.getMethod("browse", URI.class).invoke(o, link);
        }
        catch (Throwable e) {
            if (OperatingSystem.getCurrentPlatform() == OSX) {
                try {
                    Runtime.getRuntime().exec(new String[]{"/usr/bin/open", link.toString()});
                }
                catch (IOException e1) {
                    LOGGER.error("Failed to open link " + link.toString(), (Throwable)e1);
                }
            }
            LOGGER.error("Failed to open link " + link.toString(), e);
        }
    }

    public static void openFolder(File path) {
        String absolutePath = path.getAbsolutePath();
        OperatingSystem os = OperatingSystem.getCurrentPlatform();
        if (os == OSX) {
            try {
                Runtime.getRuntime().exec(new String[]{"/usr/bin/open", absolutePath});
                return;
            }
            catch (IOException e) {
                LOGGER.error("Couldn't open " + path + " through /usr/bin/open", (Throwable)e);
            }
        } else if (os == WINDOWS) {
            String cmd = String.format("cmd.exe /C start \"Open file\" \"%s\"", absolutePath);
            try {
                Runtime.getRuntime().exec(cmd);
                return;
            }
            catch (IOException e) {
                LOGGER.error("Couldn't open " + path + " through cmd.exe", (Throwable)e);
            }
        }
        try {
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            Object desktop = desktopClass.getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
            desktopClass.getMethod("browse", URI.class).invoke(desktop, path.toURI());
        }
        catch (Throwable e) {
            LOGGER.error("Couldn't open " + path + " through Desktop.browse()", e);
        }
    }

    static {
        LOGGER = LogManager.getLogger();
    }
}

