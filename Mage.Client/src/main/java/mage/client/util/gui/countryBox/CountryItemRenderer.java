
package mage.client.util.gui.countryBox;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

 
/**
 * Customer renderer for JComboBox
 * @author www.codejava.net
 *
 */

public class CountryItemRenderer extends JPanel implements ListCellRenderer {
    private final JLabel labelItem = new JLabel();

    // v33: flags were re-read from disk on every repaint of the combo popup
    private static final Map<String, Icon> flagIcons = new ConcurrentHashMap<>();

    private static Icon flagIcon(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return flagIcons.computeIfAbsent(path, p -> new ImageIcon(p));
    }
     
    public CountryItemRenderer() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 0, 0, 0);
         
        labelItem.setOpaque(true);
        labelItem.setHorizontalAlignment(JLabel.LEFT);
         
        add(labelItem, constraints);
    }
     
    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        if (!(value instanceof String[])) {
            // v33: defensive, e.g. editable combo rendering a plain string
            labelItem.setText(value == null ? "" : value.toString());
            labelItem.setIcon(null);
        } else {
            String[] countryItem = (String[]) value;

            // set country name
            labelItem.setText(countryItem[0]);

            // set country flag (cached)
            labelItem.setIcon(flagIcon(countryItem[1]));
        }

        // v33: follow the list / current look&feel selection colors instead of hard-coded blue/yellow
        Color bg = list != null ? list.getBackground() : Color.WHITE;
        Color fg = list != null ? list.getForeground() : Color.BLACK;
        if (isSelected) {
            bg = list.getSelectionBackground();
            fg = list.getSelectionForeground();
        }
        labelItem.setBackground(bg);
        labelItem.setForeground(fg);
        setBackground(bg);
         
        return this;
    }
 
}