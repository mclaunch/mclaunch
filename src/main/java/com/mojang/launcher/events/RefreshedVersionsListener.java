package com.mojang.launcher.events;

import com.mojang.launcher.updater.VersionManager;

public interface RefreshedVersionsListener {
    public void onVersionsRefreshed(VersionManager var1);
}

