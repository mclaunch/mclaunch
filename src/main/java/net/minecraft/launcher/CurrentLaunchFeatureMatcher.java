package net.minecraft.launcher;

import com.google.common.base.Objects;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import net.minecraft.launcher.CompatibilityRule;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;

public class CurrentLaunchFeatureMatcher
implements CompatibilityRule.FeatureMatcher {
    private final Profile profile;
    private final CompleteMinecraftVersion version;
    private final UserAuthentication auth;

    public CurrentLaunchFeatureMatcher(Profile profile, CompleteMinecraftVersion version, UserAuthentication auth) {
        this.profile = profile;
        this.version = version;
        this.auth = auth;
    }

    @Override
    public boolean hasFeature(String name, Object value) {
        if (name.equals("is_demo_user")) {
            return Objects.equal(this.auth.getSelectedProfile() == null, value);
        }
        if (name.equals("has_custom_resolution")) {
            return Objects.equal(this.profile.getResolution() != null, value);
        }
        return false;
    }
}

