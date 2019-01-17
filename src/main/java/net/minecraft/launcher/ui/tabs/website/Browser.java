package net.minecraft.launcher.ui.tabs.website;

import java.awt.Component;
import java.awt.Dimension;

public interface Browser {
    public void loadUrl(String var1);

    public Component getComponent();

    public void resize(Dimension var1);
}

