package mage.client.components.ability;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.ImageIcon;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.metal.MetalScrollButton;

import mage.client.util.TransformedImageCache;

/**
 * Buttons for scroll bar.
 *
 * @author nantuko
 */
public class MageScrollButton extends MetalScrollButton {

    private static final ImageIcon buttonLeft;
    private static final ImageIcon buttonRight;
    private static final ImageIcon buttonUp;
    private static final ImageIcon buttonDown;

    // v33: scaled icons per size instead of mutating the shared static icons in the constructor
    // (different scrollbars used to overwrite each other's image and used the old getScaledInstance API)
    private static final Map<Integer, ImageIcon[]> scaledIconsBySize = new ConcurrentHashMap<>();

    private int width;

    static {
        buttonLeft = new ImageIcon(MageScrollButton.class.getResource("/buttons/left.png"));
        buttonRight = new ImageIcon(MageScrollButton.class.getResource("/buttons/right.png"));
        buttonUp = new ImageIcon(MageScrollButton.class.getResource("/buttons/up.png"));
        buttonDown = new ImageIcon(MageScrollButton.class.getResource("/buttons/down.png"));
    }

    public MageScrollButton(int direction, int width, boolean freeStanding) {
        super(direction, width, freeStanding);
        setOpaque(false);
        this.width = width;
    }

    private static ImageIcon scaledIcon(int slot, ImageIcon base, int size) {
        ImageIcon[] icons = scaledIconsBySize.computeIfAbsent(size, s -> new ImageIcon[4]);
        synchronized (icons) {
            if (icons[slot] == null) {
                Image img = base.getImage();
                if (img instanceof BufferedImage) {
                    icons[slot] = new ImageIcon(TransformedImageCache.getResizedImage((BufferedImage) img, size, size));
                } else {
                    icons[slot] = new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH));
                }
            }
            return icons[slot];
        }
    }

    @Override
    public Dimension getMaximumSize() {
        return this.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return this.getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, width);
    }

    @Override
    public void paint(Graphics g) {
        switch (getDirection()) {
        case BasicArrowButton.WEST:
            scaledIcon(0, buttonLeft, width).paintIcon(null, g, 0, 0);
            break;
        case BasicArrowButton.EAST:
            scaledIcon(1, buttonRight, width).paintIcon(null, g, 0, 0);
            break;
        case BasicArrowButton.NORTH:
            scaledIcon(2, buttonUp, width).paintIcon(null, g, 0, 0);
            break;
        case BasicArrowButton.SOUTH:
            scaledIcon(3, buttonDown, width).paintIcon(null, g, 0, 0);
            break;
        }
    }
}
