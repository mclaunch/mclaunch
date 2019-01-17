package com.mojang.launcher.versions;

import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.util.Date;

public interface CompleteVersion
extends Version {
    @Override
    public String getId();

    @Override
    public ReleaseType getType();

    @Override
    public Date getUpdatedTime();

    @Override
    public Date getReleaseTime();

    public int getMinimumLauncherVersion();

    public boolean appliesToCurrentEnvironment();

    public String getIncompatibilityReason();

    public boolean isSynced();

    public void setSynced(boolean var1);
}

