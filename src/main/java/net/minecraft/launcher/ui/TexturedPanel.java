package net.minecraft.launcher.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TexturedPanel
extends JPanel {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long serialVersionUID = 1L;
    private Image image;
    private Image bgImage;

    public TexturedPanel(String filename) {
        this.setOpaque(true);
        try {
            this.bgImage = ImageIO.read(TexturedPanel.class.getResource(filename)).getScaledInstance(32, 32, 16);
        }
        catch (IOException e) {
            LOGGER.error("Unexpected exception initializing textured panel", (Throwable)e);
        }
    }

    @Override
    public void update(Graphics g) {
        this.paint(g);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        int width = this.getWidth() / 2 + 1;
        int height = this.getHeight() / 2 + 1;
        if (this.image == null || this.image.getWidth(null) != width || this.image.getHeight(null) != height) {
            this.image = this.createImage(width, height);
            this.copyImage(width, height);
        }
        graphics.drawImage(this.image, 0, 0, width * 2, height * 2, null);
    }

    protected void copyImage(int width, int height) {
        Graphics imageGraphics = this.image.getGraphics();
        for (int x = 0; x <= width / 32; ++x) {
            for (int y = 0; y <= height / 32; ++y) {
                imageGraphics.drawImage(this.bgImage, x * 32, y * 32, null);
            }
        }
        if (imageGraphics instanceof Graphics2D) {
            this.overlayGradient(width, height, (Graphics2D)imageGraphics);
        }
        imageGraphics.dispose();
    }

    protected void overlayGradient(int width, int height, Graphics2D graphics) {
        int gh = 1;
        graphics.setPaint(new GradientPaint(new Point2D.Float(0.0f, 0.0f), new Color(553648127, true), new Point2D.Float(0.0f, gh), new Color(0, true)));
        graphics.fillRect(0, 0, width, gh);
        gh = height;
        graphics.setPaint(new GradientPaint(new Point2D.Float(0.0f, 0.0f), new Color(0, true), new Point2D.Float(0.0f, gh), new Color(1610612736, true)));
        graphics.fillRect(0, 0, width, gh);
    }
}

