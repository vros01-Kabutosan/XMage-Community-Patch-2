/*
 * Decompiled with CFR.
 */
package mage.client.cards;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JComponent;

class SelectionBox
extends JComponent {
    public SelectionBox() {
        this.setOpaque(false);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g = g.create();
        g.setColor(new Color(100, 100, 200, 128));
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        g.setColor(new Color(0, 0, 255));
        g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
        g.dispose();
    }
}
