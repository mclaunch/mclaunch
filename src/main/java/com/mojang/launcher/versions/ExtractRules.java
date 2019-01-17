package com.mojang.launcher.versions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtractRules {
    private List<String> exclude = new ArrayList<String>();

    public ExtractRules() {
    }

    public /* varargs */ ExtractRules(String ... exclude) {
        if (exclude != null) {
            Collections.addAll(this.exclude, exclude);
        }
    }

    public ExtractRules(ExtractRules rules) {
        for (String exclude : rules.exclude) {
            this.exclude.add(exclude);
        }
    }

    public List<String> getExcludes() {
        return this.exclude;
    }

    public boolean shouldExtract(String path) {
        if (this.exclude != null) {
            for (String rule : this.exclude) {
                if (!path.startsWith(rule)) continue;
                return false;
            }
        }
        return true;
    }
}

