package com.mojang.launcher.versions;

import com.mojang.launcher.versions.ReleaseType;

public interface ReleaseTypeFactory<T extends ReleaseType>
extends Iterable<T> {
    public T getTypeByName(String var1);

    public T[] getAllTypes();

    public Class<T> getTypeClass();
}

