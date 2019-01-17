package net.minecraft.launcher.ui.bottombar;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import javax.swing.JPanel;

public abstract class SidebarGridForm
extends JPanel {
    protected SidebarGridForm() {
    }

    protected void createInterface() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        this.setLayout(layout);
        this.populateGrid(constraints);
    }

    protected abstract void populateGrid(GridBagConstraints var1);

    protected <T extends Component> T add(T component, GridBagConstraints constraints, int x, int y, int weight, int width) {
        return this.add(component, constraints, x, y, weight, width, 10);
    }

    protected <T extends Component> T add(T component, GridBagConstraints constraints, int x, int y, int weight, int width, int anchor) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = weight;
        constraints.weighty = 1.0;
        constraints.gridwidth = width;
        constraints.anchor = anchor;
        this.add((Component)component, constraints);
        return component;
    }
}

