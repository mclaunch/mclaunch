package net.minecraft.launcher.game;

import com.google.common.collect.Maps;
import com.mojang.launcher.versions.ReleaseType;
import java.util.Map;

public enum MinecraftReleaseType implements ReleaseType
{
    SNAPSHOT("snapshot", "Enable experimental development versions (\"snapshots\")"),
    RELEASE("release", null),
    OLD_BETA("old_beta", "Allow use of old \"Beta\" Minecraft versions (From 2010-2011)"),
    OLD_ALPHA("old_alpha", "Allow use of old \"Alpha\" Minecraft versions (From 2010)");
    
    private static final String POPUP_DEV_VERSIONS = "Are you sure you want to enable development builds?\nThey are not guaranteed to be stable and may corrupt your world.\nYou are advised to run this in a separate directory or run regular backups.";
    private static final String POPUP_OLD_VERSIONS = "These versions are very out of date and may be unstable. Any bugs, crashes, missing features or\nother nasties you may find will never be fixed in these versions.\nIt is strongly recommended you play these in separate directories to avoid corruption.\nWe are not responsible for the damage to your nostalgia or your save files!";
    private static final Map<String, MinecraftReleaseType> LOOKUP;
    private final String name;
    private final String description;

    private MinecraftReleaseType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getPopupWarning() {
        if (this.description == null) {
            return null;
        }
        if (this == SNAPSHOT) {
            return POPUP_DEV_VERSIONS;
        }
        if (this == OLD_BETA) {
            return POPUP_OLD_VERSIONS;
        }
        if (this == OLD_ALPHA) {
            return POPUP_OLD_VERSIONS;
        }
        return null;
    }

    public static MinecraftReleaseType getByName(String name) {
        return LOOKUP.get(name);
    }

    static {
        LOOKUP = Maps.newHashMap();
        for (MinecraftReleaseType type : MinecraftReleaseType.values()) {
            LOOKUP.put(type.getName(), type);
        }
    }
}

