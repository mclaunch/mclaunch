package net.minecraft.launcher.updater;

public enum DownloadType {
    CLIENT,
    SERVER,
    CLIENT_MAPPINGS, // Add mappings to fix 1.14.4+
    SERVER_MAPPINGS,
    WINDOWS_SERVER;
    

    private DownloadType() {
    }
}
