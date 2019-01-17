package com.mojang.launcher.updater;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.ReleaseTypeFactory;
import java.util.Collections;
import java.util.Set;

public class VersionFilter<T extends ReleaseType> {
    private final Set<T> types = Sets.newHashSet();
    private int maxCount = 5;

    public VersionFilter(ReleaseTypeFactory<T> factory) {
        Iterables.addAll(this.types, factory);
    }

    public Set<T> getTypes() {
        return this.types;
    }

    public /* varargs */ VersionFilter<T> onlyForTypes(T ... types) {
        this.types.clear();
        this.includeTypes(types);
        return this;
    }

    public /* varargs */ VersionFilter<T> includeTypes(T ... types) {
        if (types != null) {
            Collections.addAll(this.types, types);
        }
        return this;
    }

    public /* varargs */ VersionFilter<T> excludeTypes(T ... types) {
        if (types != null) {
            for (T type : types) {
                this.types.remove(type);
            }
        }
        return this;
    }

    public int getMaxCount() {
        return this.maxCount;
    }

    public VersionFilter<T> setMaxCount(int maxCount) {
        this.maxCount = maxCount;
        return this;
    }
}

