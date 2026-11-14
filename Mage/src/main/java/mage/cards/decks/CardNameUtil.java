package mage.cards.decks;

import java.util.regex.Pattern;

public class CardNameUtil {

    public static final Pattern CARD_NAME_PATTERN = Pattern.compile("[+ !\"&',\\-./0-9:A-Za-z]+");

    /**
     * Convert card names with unicode symbols to ascii, uses to deck import from a third party services
     */
    public static String normalizeCardName(String name) {
        // new symbols checks in verify test, no need to manually search it
        return name
                .replace("&amp;", "//")
                .replace("///", "//")
                .replace("Ã†", "Ae")
                .replace("Æ", "Ae")
                .replace("’", "'")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("Ã¶", "o") // v36: era "A" -- mojibake de ö en UTF-8 leido como CP1252 (Mjölnir -> MjAlnir)
                .replace("ö", "o")
                .replace("û", "u")
                .replace("í", "i")
                .replace("ï", "i")
                .replace("î", "i")
                .replace("â", "a")
                .replace("á", "a")
                .replace("à", "a")
                .replace("é", "e")
                .replace("ú", "u")
                .replace("†", "+")
                .replace("★", "*")
                .replace("Φ", "Ph")
                .replace("Ó", "O")
                .replace("ó", "o")
                .replace("ō", "o")
                .replace("õ", "o")
                .replace("ä", "a")
                .replace("ü", "u")
                .replace("É", "E")
                .replace("ñ", "n")
                .replace("꞉", "")
                .replace("®", "")
                .replace("—", "");
    }

    private CardNameUtil() {
        // utility class
    }

}
