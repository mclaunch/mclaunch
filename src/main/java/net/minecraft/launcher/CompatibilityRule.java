package net.minecraft.launcher;

import com.mojang.launcher.OperatingSystem;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompatibilityRule {
    private Action action = Action.ALLOW;
    private OSRestriction os;
    private Map<String, Object> features;

    public CompatibilityRule() {
    }

    public CompatibilityRule(CompatibilityRule compatibilityRule) {
        this.action = compatibilityRule.action;
        if (compatibilityRule.os != null) {
            this.os = new OSRestriction(compatibilityRule.os);
        }
        if (compatibilityRule.features != null) {
            this.features = compatibilityRule.features;
        }
    }

    public Action getAppliedAction(FeatureMatcher featureMatcher) {
        if (this.os != null && !this.os.isCurrentOperatingSystem()) {
            return null;
        }
        if (this.features != null) {
            if (featureMatcher == null) {
                return null;
            }
            for (Map.Entry<String, Object> feature : this.features.entrySet()) {
                if (featureMatcher.hasFeature(feature.getKey(), feature.getValue())) continue;
                return null;
            }
        }
        return this.action;
    }

    public Action getAction() {
        return this.action;
    }

    public OSRestriction getOs() {
        return this.os;
    }

    public Map<String, Object> getFeatures() {
        return this.features;
    }

    public String toString() {
        return "Rule{action=" + (Object)((Object)this.action) + ", os=" + this.os + ", features=" + this.features + '}';
    }

    public static enum Action {
        ALLOW,
        DISALLOW;
        

        private Action() {
        }
    }

    public static interface FeatureMatcher {
        public boolean hasFeature(String var1, Object var2);
    }

    public class OSRestriction {
        private OperatingSystem name;
        private String version;
        private String arch;

        public OSRestriction() {
        }

        public OperatingSystem getName() {
            return this.name;
        }

        public String getVersion() {
            return this.version;
        }

        public String getArch() {
            return this.arch;
        }

        public OSRestriction(OSRestriction osRestriction) {
            this.name = osRestriction.name;
            this.version = osRestriction.version;
            this.arch = osRestriction.arch;
        }

        public boolean isCurrentOperatingSystem() {
            Matcher matcher;
            Pattern pattern2;
            if (this.name != null && this.name != OperatingSystem.getCurrentPlatform()) {
                return false;
            }
            if (this.version != null) {
                try {
                    pattern2 = Pattern.compile(this.version);
                    matcher = pattern2.matcher(System.getProperty("os.version"));
                    if (!matcher.matches()) {
                        return false;
                    }
                //} catch (Throwable pattern2) {
                } catch (Throwable e) { // pattern2 was already defined
                    // empty catch block
                }
            }
            if (this.arch != null) {
                try {
                    pattern2 = Pattern.compile(this.arch);
                    matcher = pattern2.matcher(System.getProperty("os.arch"));
                    if (!matcher.matches()) {
                        return false;
                    }
                } catch (Throwable pattern3) {
                    // empty catch block
                }
            }
            return true;
        }

        public String toString() {
            return "OSRestriction{name=" + (Object)((Object)this.name) + ", version='" + this.version + '\'' + ", arch='" + this.arch + '\'' + '}';
        }
    }

}

