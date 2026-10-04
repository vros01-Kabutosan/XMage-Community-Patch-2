package mage.client.util;

import mage.view.CardView;
import mage.view.PermanentView;

import java.util.Objects;

/**
 * Comparacion de CardView para detectar cambios en el panel de Battlefield / mano.
 * <p>
 * El oficial 1.4.62 elimino {@code CardView.cardViewEquals} (la deteccion de cambios
 * paso a la firma de render de la imagen). Nuestro mod de evitar actualizaciones
 * redundantes sigue necesitando la comparacion, asi que se reimplementa aqui con la
 * misma semantica que tenia en 1.4.61.
 */
public final class CardViewEquality {

    private CardViewEquality() {
    }

    public static boolean cardViewEquals(CardView a, CardView b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.getClass() != b.getClass()) {
            return false;
        }
        if (!(a.getDisplayName().equals(b.getDisplayName())
                && a.getPower().equals(b.getPower())
                && a.getToughness().equals(b.getToughness())
                && a.getLoyalty().equals(b.getLoyalty())
                && a.getDefense().equals(b.getDefense())
                && 0 == a.getColor().compareTo(b.getColor())
                && a.getCardTypes().equals(b.getCardTypes())
                && a.getSubTypes().equals(b.getSubTypes())
                && a.getSuperTypes().equals(b.getSuperTypes())
                && a.getManaCostStr().equals(b.getManaCostStr())
                && a.getRules().equals(b.getRules())
                && Objects.equals(a.getRarity(), b.getRarity())
                && a.getFrameStyle() == b.getFrameStyle()
                && Objects.equals(a.getCounters(), b.getCounters())
                && a.isFaceDown() == b.isFaceDown())) {
            return false;
        }
        if (!(Objects.equals(a.getExpansionSetCode(), b.getExpansionSetCode())
                && Objects.equals(a.getCardNumber(), b.getCardNumber())
                && Objects.equals(a.getImageNumber(), b.getImageNumber())
                && Objects.equals(a.getImageFileName(), b.getImageFileName())
                && Objects.equals(a.getUsesVariousArt(), b.getUsesVariousArt()))) {
            return false;
        }
        if (!(a instanceof PermanentView)) {
            return true;
        }
        PermanentView aa = (PermanentView) a;
        PermanentView bb = (PermanentView) b;
        return aa.hasSummoningSickness() == bb.hasSummoningSickness()
                && aa.getDamage() == bb.getDamage();
    }
}