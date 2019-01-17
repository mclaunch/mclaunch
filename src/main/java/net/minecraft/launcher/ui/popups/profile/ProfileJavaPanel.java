package net.minecraft.launcher.ui.popups.profile;

import com.mojang.launcher.OperatingSystem;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.ui.popups.profile.ProfileEditorPopup;

public class ProfileJavaPanel
extends JPanel {
    private final ProfileEditorPopup editor;
    private final JCheckBox javaPathCustom = new JCheckBox("Executable:");
    private final JTextField javaPathField = new JTextField();
    private final JCheckBox javaArgsCustom = new JCheckBox("JVM Arguments:");
    private final JTextField javaArgsField = new JTextField();

    public ProfileJavaPanel(ProfileEditorPopup editor) {
        this.editor = editor;
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createTitledBorder("Java Settings (Advanced)"));
        this.createInterface();
        this.fillDefaultValues();
        this.addEventHandlers();
    }

    protected void createInterface() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = 17;
        constraints.gridy = 0;
        this.add((Component)this.javaPathCustom, constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add((Component)this.javaPathField, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        ++constraints.gridy;
        this.add((Component)this.javaArgsCustom, constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add((Component)this.javaArgsField, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        ++constraints.gridy;
    }

    protected void fillDefaultValues() {
        String javaPath = this.editor.getProfile().getJavaPath();
        if (javaPath != null) {
            this.javaPathCustom.setSelected(true);
            this.javaPathField.setText(javaPath);
        } else {
            this.javaPathCustom.setSelected(false);
            this.javaPathField.setText(OperatingSystem.getCurrentPlatform().getJavaDir());
        }
        this.updateJavaPathState();
        String args = this.editor.getProfile().getJavaArgs();
        if (args != null) {
            this.javaArgsCustom.setSelected(true);
            this.javaArgsField.setText(args);
        } else {
            this.javaArgsCustom.setSelected(false);
            this.javaArgsField.setText("-Xmx1G -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:-UseAdaptiveSizePolicy -Xmn128M");
        }
        this.updateJavaArgsState();
    }

    protected void addEventHandlers() {
        this.javaPathCustom.addItemListener(new ItemListener(){

            @Override
            public void itemStateChanged(ItemEvent e) {
                ProfileJavaPanel.this.updateJavaPathState();
            }
        });
        this.javaPathField.getDocument().addDocumentListener(new DocumentListener(){

            @Override
            public void insertUpdate(DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaPath();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaPath();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaPath();
            }
        });
        this.javaArgsCustom.addItemListener(new ItemListener(){

            @Override
            public void itemStateChanged(ItemEvent e) {
                ProfileJavaPanel.this.updateJavaArgsState();
            }
        });
        this.javaArgsField.getDocument().addDocumentListener(new DocumentListener(){

            @Override
            public void insertUpdate(DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaArgs();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaArgs();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                ProfileJavaPanel.this.updateJavaArgs();
            }
        });
    }

    private void updateJavaPath() {
        if (this.javaPathCustom.isSelected()) {
            this.editor.getProfile().setJavaDir(this.javaPathField.getText());
        } else {
            this.editor.getProfile().setJavaDir(null);
        }
    }

    private void updateJavaPathState() {
        if (this.javaPathCustom.isSelected()) {
            this.javaPathField.setEnabled(true);
            this.editor.getProfile().setJavaDir(this.javaPathField.getText());
        } else {
            this.javaPathField.setEnabled(false);
            this.editor.getProfile().setJavaDir(null);
        }
    }

    private void updateJavaArgs() {
        if (this.javaArgsCustom.isSelected()) {
            this.editor.getProfile().setJavaArgs(this.javaArgsField.getText());
        } else {
            this.editor.getProfile().setJavaArgs(null);
        }
    }

    private void updateJavaArgsState() {
        if (this.javaArgsCustom.isSelected()) {
            this.javaArgsField.setEnabled(true);
            this.editor.getProfile().setJavaArgs(this.javaArgsField.getText());
        } else {
            this.javaArgsField.setEnabled(false);
            this.editor.getProfile().setJavaArgs(null);
        }
    }

}

