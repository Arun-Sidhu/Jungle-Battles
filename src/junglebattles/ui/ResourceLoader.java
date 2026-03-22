package junglebattles.ui;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;

// This utility class keeps image loading out of the gameplay code. If an image is missing the
// callers can fall back to text without having to know how resources are resolved.
public final class ResourceLoader {
    // No instances are needed here because the class is just a tiny home for one helper.
    private ResourceLoader() {
    }

    /*
     * Icons are loaded from the classpath and scaled to a predictable size for the arena.
     * Returning null on failure keeps the game resilient when image files are not present
     * yet.
     */
    public static ImageIcon loadIcon(String resourcePath, int width, int height) {
        URL url = ResourceLoader.class.getClassLoader().getResource(resourcePath);
        if (url == null) {
            return null;
        }

        ImageIcon icon = new ImageIcon(url);
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
