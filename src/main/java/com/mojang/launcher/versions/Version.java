package com.mojang.launcher.versions;

import com.mojang.launcher.versions.ReleaseType;
import java.util.Date;

public interface Version {
    public String getId();

    public ReleaseType getType();

    public Date getUpdatedTime();

    public Date getReleaseTime();
}

