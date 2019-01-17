package net.minecraft.launcher.ui.tabs;

import com.mojang.util.QueueLogAppender;
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
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import net.minecraft.launcher.Launcher;

public class ConsoleTab
extends JScrollPane {
    private static final Font MONOSPACED = new Font("Monospaced", 0, 12);
    private final JTextArea console = new JTextArea();
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final JMenuItem copyTextButton = new JMenuItem("Copy All Text");
    private final Launcher minecraftLauncher;

    public ConsoleTab(Launcher minecraftLauncher) {
        this.minecraftLauncher = minecraftLauncher;
        this.popupMenu.add(this.copyTextButton);
        this.console.setComponentPopupMenu(this.popupMenu);
        this.copyTextButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    StringSelection ss = new StringSelection(ConsoleTab.this.console.getText());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                } catch (Exception ss) {
                    // empty catch block
                }
            }
        });
        this.console.setFont(MONOSPACED);
        this.console.setEditable(false);
        this.console.setMargin(null);
        this.setViewportView(this.console);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String line;
                while ((line = QueueLogAppender.getNextLogEvent("DevelopmentConsole")) != null) {
                    ConsoleTab.this.print(line);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public Launcher getMinecraftLauncher() {
        return this.minecraftLauncher;
    }

    public void print(final String line) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run() {
                    ConsoleTab.this.print(line);
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

}

