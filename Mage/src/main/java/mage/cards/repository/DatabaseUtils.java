package mage.cards.repository;

import mage.util.DebugUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

/**
 * Helper class for database
 *
 * @author JayDi85
 */
public class DatabaseUtils {

    private static final Logger logger = Logger.getLogger(DatabaseUtils.class);

    static {
        // Ensure H2 binds and advertises localhost (127.0.0.1) for AUTO_SERVER
        // instead of binding to or advertising external / VPN / TAP virtual adapter addresses.
        if (System.getProperty("h2.bindAddress") == null) {
            System.setProperty("h2.bindAddress", "127.0.0.1");
        }
    }

    // warning, do not change names or db format
    // h2
    public static final String DB_NAME_FEEDBACK = "feedback.h2";
    public static final String DB_NAME_USERS = "authorized_user.h2";
    public static final String DB_NAME_CARDS = "cards.h2";
    // sqlite (usage reason: h2 database works bad with 1GB+ files and can break it)
    public static final String DB_NAME_RECORDS = "table_record.db";
    public static final String DB_NAME_STATS = "user_stats.db";

    /**
     * Prepare JDBC connection string and setup additional params for H2 databases
     *
     * @param dbName        database name like "cards.h2"
     * @param improveCaches use memory optimizations for cards database (no needs for other dbs)
     */
    public static String prepareH2Connection(String dbName, boolean improveCaches) {
        if (System.getProperty("h2.bindAddress") == null) {
            System.setProperty("h2.bindAddress", "127.0.0.1");
        }
        checkAndFixStaleH2Lock(dbName);

        // example: jdbc:h2:file:./db/cards.h2;AUTO_SERVER=TRUE;IGNORECASE=TRUE
        String res = String.format("jdbc:h2:file:./db/%s", dbName);

        // shared params
        res += ";AUTO_SERVER=TRUE"; // open database in mix mode (first open by new thread, second open by new jvm-process)
        res += ";IGNORECASE=TRUE"; // ignore char case for text searching

        // additional params
        // can be defined by connection string, by exec sql like "SET xxx = yyy", by settings from existing db-file

        if (improveCaches) {
            // CACHE_SIZE
            // max query cache size in kb (default: 65 Mb per 1 GB of java's max memory)
            // warning, xmage require 150Mb cache for big queries in AI games like all card names (db can be broken on lower cache)
            //res += ";CACHE_SIZE=150000";
            res += ";CACHE_SIZE=" + Math.round(Math.max(150000, Runtime.getRuntime().maxMemory() * 0.1 / 1024));


            // QUERY_CACHE_SIZE
            // queries amount per session to cache (default: 8)
            res += ";QUERY_CACHE_SIZE=32";
        }

        // add debug stats (see DebugUtil for usage instruction)
        if (DebugUtil.DATABASE_PROFILE_SQL_QUERIES_TO_FILE) {
            res += ";TRACE_LEVEL_FILE=2";
            res += ";QUERY_STATISTICS=TRUE";
        }

        return res;
    }

    /**
     * Check if a stale or unreachable .lock.db file exists from a previous crash,
     * killed process, or disconnected VPN/network interface, and delete it to prevent
     * SocketTimeoutException on startup.
     */
    public static void checkAndFixStaleH2Lock(String dbName) {
        try {
            File lockFile = new File("db/" + dbName + ".lock.db");
            if (!lockFile.exists() || !lockFile.isFile()) {
                return;
            }
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(lockFile)) {
                props.load(in);
            }
            String server = props.getProperty("server");
            if (server == null || server.trim().isEmpty()) {
                logger.warn("Cleaning corrupt H2 lock file: " + lockFile.getName());
                lockFile.delete();
                return;
            }
            String[] parts = server.split(":");
            String host = parts[0].trim();
            int port = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;

            boolean isDeadOrUnreachable = false;
            if (!"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host)) {
                // External / VPN / virtual TAP address recorded in lock file
                isDeadOrUnreachable = true;
            } else if (port > 0) {
                // Test if the server process on localhost is actually alive and listening
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, port), 300);
                } catch (IOException ex) {
                    isDeadOrUnreachable = true;
                }
            } else {
                isDeadOrUnreachable = true;
            }

            if (isDeadOrUnreachable) {
                logger.warn("Found stale/unreachable H2 lock file " + lockFile.getName() + " pointing to " + server + ". Cleaning up...");
                boolean deleted = lockFile.delete();
                if (!deleted) {
                    lockFile.deleteOnExit();
                }
            }
        } catch (Exception e) {
            logger.warn("Could not check/clean H2 lock file for " + dbName + ": " + e.getMessage());
        }
    }

    /**
     * Prepare JDBC connection string and setup additional params for SQLite databases
     *
     * @param dbName database name like "cards"
     */
    public static String prepareSqliteConnection(String dbName) {
        // example: jdbc:sqlite:./db/table_record.db
        return String.format("jdbc:sqlite:./db/%s", dbName);
    }
}
