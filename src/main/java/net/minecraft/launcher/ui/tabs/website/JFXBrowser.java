/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  javafx.application.Platform
 *  javafx.beans.property.ReadOnlyObjectProperty
 *  javafx.beans.value.ChangeListener
 *  javafx.beans.value.ObservableValue
 *  javafx.collections.ObservableList
 *  javafx.concurrent.Worker
 *  javafx.concurrent.Worker$State
 *  javafx.embed.swing.JFXPanel
 *  javafx.scene.Group
 *  javafx.scene.Parent
 *  javafx.scene.Scene
 *  javafx.scene.web.WebEngine
 *  javafx.scene.web.WebView
 */
package net.minecraft.launcher.ui.tabs.website;

import com.mojang.launcher.OperatingSystem;
import java.awt.Component;
import java.awt.Dimension;
import java.net.URI;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import net.minecraft.launcher.ui.tabs.website.Browser;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

public class JFXBrowser
implements Browser {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Object lock = new Object();
    private final JFXPanel fxPanel = new JFXPanel();
    private String urlToBrowseTo;
    private Dimension size;
    private WebView browser;
    private WebEngine webEngine;

    public JFXBrowser() {
        Platform.runLater((Runnable)new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                Group root = new Group();
                Scene scene = new Scene((Parent)root);
                JFXBrowser.this.fxPanel.setScene(scene);
                Object object = JFXBrowser.this.lock;
                synchronized (object) {
                    JFXBrowser.this.browser = new WebView();
                    JFXBrowser.this.browser.setContextMenuEnabled(false);
                    if (JFXBrowser.this.size != null) {
                        JFXBrowser.this.resize(JFXBrowser.this.size);
                    }
                    JFXBrowser.this.webEngine = JFXBrowser.this.browser.getEngine();
                    JFXBrowser.this.webEngine.setJavaScriptEnabled(false);
                    JFXBrowser.this.webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
                        public void changed(final ObservableValue<? extends Worker.State> observableValue,
                                            final Worker.State oldState, final Worker.State newState) {
                            if (newState == Worker.State.SUCCEEDED) {
                                final EventListener listener = new EventListener() {
                                    @Override
                                    public void handleEvent(final Event event) {
                                        if (event.getTarget() instanceof Element) {
                                            Element element;
                                            String href;
                                            for (element = (Element) event.getTarget(), href = element.getAttribute("href"); StringUtils.isEmpty(href) && element.getParentNode() instanceof Element;
                                                 element = (Element) element.getParentNode(), href = element.getAttribute("href")) {
                                            }
                                            if (href != null && href.length() > 0) {
                                                try {
                                                    OperatingSystem.openLink(new URI(href));
                                                } catch (Exception e) {
                                                    JFXBrowser.LOGGER.error("Unexpected exception opening link " + href, e);
                                                }
                                                event.preventDefault();
                                                event.stopPropagation();
                                            }
                                        }
                                    }
                                };
                                final Document doc = JFXBrowser.this.webEngine.getDocument();
                                if (doc != null) {
                                    final NodeList elements = doc.getElementsByTagName("a");
                                    for (int i = 0; i < elements.getLength(); ++i) {
                                        final Node item = elements.item(i);
                                        if (item instanceof EventTarget) {
                                            ((EventTarget) item).addEventListener("click", listener, false);
                                        }
                                    }
                                }
                            }
                        }
                    });
                    if (JFXBrowser.this.urlToBrowseTo != null) {
                        JFXBrowser.this.loadUrl(JFXBrowser.this.urlToBrowseTo);
                    }
                }
                root.getChildren().add(JFXBrowser.this.browser);
            }

        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void loadUrl(final String url) {
        Object object = this.lock;
        synchronized (object) {
            this.urlToBrowseTo = url;
            if (this.webEngine != null) {
                Platform.runLater((Runnable)new Runnable(){

                    @Override
                    public void run() {
                        JFXBrowser.this.webEngine.load(url);
                    }
                });
            }
        }
    }

    @Override
    public Component getComponent() {
        return this.fxPanel;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void resize(Dimension size) {
        Object object = this.lock;
        synchronized (object) {
            this.size = size;
            if (this.browser != null) {
                this.browser.setMinSize(size.getWidth(), size.getHeight());
                this.browser.setMaxSize(size.getWidth(), size.getHeight());
                this.browser.setPrefSize(size.getWidth(), size.getHeight());
            }
        }
    }

}

