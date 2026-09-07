package mage.client.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.log4j.Logger;

/** Supplies downloaded AI decks without repeated disk scans or immediate repeats. */
public final class AiDeckLibrary {
    private static final Logger LOGGER = Logger.getLogger(AiDeckLibrary.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, List<Path>> DECKS_BY_FORMAT = new ConcurrentHashMap<>();
    private static final Map<String, Deque<Path>> SHUFFLED_DECKS_BY_FORMAT = new ConcurrentHashMap<>();

    private AiDeckLibrary() {}

    public static String nextDeck(String deckType) {
        String format = supportedFormat(deckType);
        if (format == null) return null;
        synchronized (AiDeckLibrary.class) {
            List<Path> decks = DECKS_BY_FORMAT.computeIfAbsent(format, AiDeckLibrary::scanFormat);
            if (decks.isEmpty()) return null;
            Deque<Path> shuffled = SHUFFLED_DECKS_BY_FORMAT.computeIfAbsent(format, key -> new ArrayDeque<>());
            if (shuffled.isEmpty()) {
                List<Path> refill = new ArrayList<>(decks);
                Collections.shuffle(refill, RANDOM);
                shuffled.addAll(refill);
            }
            return shuffled.removeFirst().toString();
        }
    }

    private static String supportedFormat(String deckType) {
        if (deckType == null) return null;
        String normalized = deckType.trim().toLowerCase(Locale.ROOT);
        if ("modern".equals(normalized) || normalized.endsWith("- modern")) return "Modern";
        if ("pioneer".equals(normalized) || normalized.endsWith("- pioneer")) return "Pioneer";
        if ("standard".equals(normalized) || normalized.endsWith("- standard")) return "Standard";
        return null;
    }

    private static List<Path> scanFormat(String format) {
        Path folder = downloadedDeckRoot().resolve(format);
        if (!Files.isDirectory(folder)) {
            LOGGER.warn("AI deck folder not found: " + folder);
            return Collections.emptyList();
        }
        try (Stream<Path> paths = Files.walk(folder)) {
            List<Path> decks = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dck"))
                    .sorted().collect(Collectors.toList());
            LOGGER.info("AI deck pool loaded: " + format + " (" + decks.size() + " decks)");
            return Collections.unmodifiableList(decks);
        } catch (IOException | SecurityException ex) {
            LOGGER.warn("Unable to scan AI deck folder: " + folder, ex);
            return Collections.emptyList();
        }
    }

    private static Path downloadedDeckRoot() {
        Path current = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        Path direct = current.resolve("config").resolve("deck-downloader");
        if (Files.isDirectory(direct)) {
            return current.resolve("sample-decks").resolve("Descargados");
        }
        Path localDecks = current.resolve("sample-decks").resolve("Descargados");
        if (Files.isDirectory(localDecks)) {
            return localDecks;
        }
        Path bundledDecks = current.resolve("client").resolve("sample-decks").resolve("Descargados");
        if (Files.isDirectory(bundledDecks)) {
            return bundledDecks;
        }
        Path xmageClientDecks = current.resolve("xmage").resolve("mage-client")
                .resolve("sample-decks").resolve("Descargados");
        return xmageClientDecks;
    }
}
