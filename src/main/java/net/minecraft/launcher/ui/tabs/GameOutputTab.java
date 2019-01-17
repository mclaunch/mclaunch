package net.minecraft.launcher.ui.tabs;

import com.mojang.launcher.events.GameOutputLogProcessor;
import com.mojang.launcher.game.process.GameProcess;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import net.minecraft.launcher.Launcher;

public class GameOutputTab
extends JScrollPane
implements GameOutputLogProcessor {
    private static final Font MONOSPACED = new Font("Monospaced", 0, 12);
    private static final int MAX_LINE_COUNT = 1000;
    private final JTextArea console = new JTextArea();
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final JMenuItem copyTextButton = new JMenuItem("Copy All Text");
    private final Launcher minecraftLauncher;
    private boolean alreadyCensored = false;

    public GameOutputTab(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        this.popupMenu.add(this.copyTextButton);
        this.console.setComponentPopupMenu(this.popupMenu);
        this.copyTextButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    StringSelection ss = new StringSelection(GameOutputTab.this.console.getText());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                }
                catch (Exception ss) {
                    // empty catch block
                }
            }
        });
        this.console.setFont(MONOSPACED);
        this.console.setEditable(false);
        this.console.setMargin(null);
        this.setViewportView(this.console);
        this.console.getDocument().addDocumentListener(new DocumentListener(){

            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(new Runnable(){

                    @Override
                    public void run() {
                        Document document = GameOutputTab.this.console.getDocument();
                        Element root = document.getDefaultRootElement();
                        while (root.getElementCount() > 1001) {
                            try {
                                document.remove(0, root.getElement(0).getEndOffset());
                            }
                            catch (BadLocationException badLocationException) {}
                        }
                    }
                });
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

        });
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public void print(final String line) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable(){

                @Override
                public void run() {
                    GameOutputTab.this.print(line);
                }
            });
            return;
        }
        Document document = this.console.getDocument();
        JScrollBar scrollBar = this.getVerticalScrollBar();
        boolean shouldScroll = false;
        if (this.getViewport().getView() == this.console) {
            shouldScroll = (double)scrollBar.getValue() + scrollBar.getSize().getHeight() + (double)(MONOSPACED.getSize() * 4) > (double)scrollBar.getMaximum();
        }
        try {
            document.insertString(document.getLength(), line, null);
        }
        catch (BadLocationException badLocationException) {
            // empty catch block
        }
        if (shouldScroll) {
            scrollBar.setValue(Integer.MAX_VALUE);
        }
    }

    @Override
    public void onGameOutput(GameProcess process, String logLine) {
        int index;
        if (!this.alreadyCensored && (index = logLine.indexOf("(Session ID is")) > 0) {
            this.alreadyCensored = true;
            logLine = logLine.substring(0, index) + "(Session ID is <censored>)";
        }
        this.print(logLine + "\n");
    }

}

