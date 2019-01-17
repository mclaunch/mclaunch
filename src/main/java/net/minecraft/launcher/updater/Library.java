package net.minecraft.launcher.updater;

import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.download.ChecksummedDownloadable;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.versions.ExtractRules;
import java.io.File;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.launcher.CompatibilityRule;
import net.minecraft.launcher.updater.DownloadInfo;
import net.minecraft.launcher.updater.LibraryDownloadInfo;
import net.minecraft.launcher.updater.PreHashedDownloadable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

public class Library {
    private static final StrSubstitutor SUBSTITUTOR = new StrSubstitutor(new HashMap<String, String>(){
        {
            this.put("arch", System.getProperty("os.arch").contains("64") ? "64" : "32");
        }
    });
    private String name;
    private List<CompatibilityRule> rules;
    private Map<OperatingSystem, String> natives;
    private ExtractRules extract;
    private String url;
    private LibraryDownloadInfo downloads;

    public Library() {
    }

    public Library(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Library name cannot be null or empty");
        }
        this.name = name;
    }

    public Library(Library library) {
        this.name = library.name;
        this.url = library.url;
        if (library.extract != null) {
            this.extract = new ExtractRules(library.extract);
        }
        if (library.rules != null) {
            this.rules = new ArrayList<CompatibilityRule>();
            for (CompatibilityRule compatibilityRule : library.rules) {
                this.rules.add(new CompatibilityRule(compatibilityRule));
            }
        }
        if (library.natives != null) {
            this.natives = new LinkedHashMap<OperatingSystem, String>();
            for (Map.Entry entry : library.getNatives().entrySet()) {
                this.natives.put((OperatingSystem)((Object)entry.getKey()), (String)entry.getValue());
            }
        }
        if (library.downloads != null) {
            this.downloads = new LibraryDownloadInfo(library.downloads);
        }
    }

    public String getName() {
        return this.name;
    }

    public Library addNative(OperatingSystem operatingSystem, String name) {
        if (operatingSystem == null || !operatingSystem.isSupported()) {
            throw new IllegalArgumentException("Cannot add native for unsupported OS");
        }
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Cannot add native for null or empty name");
        }
        if (this.natives == null) {
            this.natives = new EnumMap<OperatingSystem, String>(OperatingSystem.class);
        }
        this.natives.put(operatingSystem, name);
        return this;
    }

    public List<CompatibilityRule> getCompatibilityRules() {
        return this.rules;
    }

    public boolean appliesToCurrentEnvironment(CompatibilityRule.FeatureMatcher featureMatcher) {
        if (this.rules == null) {
            return true;
        }
        CompatibilityRule.Action lastAction = CompatibilityRule.Action.DISALLOW;
        for (CompatibilityRule compatibilityRule : this.rules) {
            CompatibilityRule.Action action = compatibilityRule.getAppliedAction(featureMatcher);
            if (action == null) continue;
            lastAction = action;
        }
        return lastAction == CompatibilityRule.Action.ALLOW;
    }

    public Map<OperatingSystem, String> getNatives() {
        return this.natives;
    }

    public ExtractRules getExtractRules() {
        return this.extract;
    }

    public Library setExtractRules(ExtractRules rules) {
        this.extract = rules;
        return this;
    }

    public String getArtifactBaseDir() {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact dir of empty/blank artifact");
        }
        String[] parts = this.name.split(":", 3);
        return String.format("%s/%s/%s", parts[0].replaceAll("\\.", "/"), parts[1], parts[2]);
    }

    public String getArtifactPath() {
        return this.getArtifactPath(null);
    }

    public String getArtifactPath(String classifier) {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact path of empty/blank artifact");
        }
        return String.format("%s/%s", this.getArtifactBaseDir(), this.getArtifactFilename(classifier));
    }

    public String getArtifactFilename(String classifier) {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact filename of empty/blank artifact");
        }
        String[] parts = this.name.split(":", 3);
        Object[] arrobject = new Object[3];
        arrobject[0] = parts[1];
        arrobject[1] = parts[2];
        arrobject[2] = StringUtils.isEmpty(classifier) ? "" : "-" + classifier;
        String result = String.format("%s-%s%s.jar", arrobject);
        return SUBSTITUTOR.replace(result);
    }

    public String toString() {
        return "Library{name='" + this.name + '\'' + ", rules=" + this.rules + ", natives=" + this.natives + ", extract=" + this.extract + '}';
    }

    public Downloadable createDownload(Proxy proxy, String path, File local, boolean ignoreLocalFiles, String classifier) throws MalformedURLException {
        if (this.url != null) {
            URL url = new URL(this.url + path);
            return new ChecksummedDownloadable(proxy, url, local, ignoreLocalFiles);
        }
        if (this.downloads == null) {
            URL url = new URL("https://libraries.minecraft.net/" + path);
            return new ChecksummedDownloadable(proxy, url, local, ignoreLocalFiles);
        }
        DownloadInfo info = this.downloads.getDownloadInfo(SUBSTITUTOR.replace(classifier));
        if (info != null) {
            return new PreHashedDownloadable(proxy, info.getUrl(), local, ignoreLocalFiles, info.getSha1());
        }
        return null;
    }

}

