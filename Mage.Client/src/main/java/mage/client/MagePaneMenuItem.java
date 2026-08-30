/*
 * Decompiled with CFR.
 */
package mage.client;

import javax.swing.JCheckBoxMenuItem;
import mage.client.MagePane;

class MagePaneMenuItem
extends JCheckBoxMenuItem {
    private final MagePane frame;

    public MagePaneMenuItem(MagePane frame) {
        super(frame.getTitle());
        this.frame = frame;
    }

    public MagePane getFrame() {
        return this.frame;
    }
}
