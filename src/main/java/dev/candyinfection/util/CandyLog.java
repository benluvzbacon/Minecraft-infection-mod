package dev.candyinfection.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central logger. Infection events are logged at coarse intervals only, so the
 * console is never spammed while large infected areas are ticking.
 */
public final class CandyLog {
    public static final Logger LOGGER = LoggerFactory.getLogger("CandyInfection");

    /**
     * Debug output is off by default and only ever emitted from coarse, low
     * frequency code paths, so the console is never spammed while large infected
     * areas tick. Flip it with {@code "debugLogging": true} in the config file.
     */
    private static boolean debugEnabled;

    private CandyLog() {
    }

    public static void setDebug(boolean enabled) {
        if (enabled != debugEnabled) {
            debugEnabled = enabled;
            LOGGER.info("[CandyInfection] Debug logging {}", enabled ? "enabled" : "disabled");
        }
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void init(String message) {
        LOGGER.info("[CandyInfection] {}", message);
    }

    public static void phase(String message) {
        LOGGER.info("[CandyInfection]   - {}", message);
    }

    public static void warn(String message) {
        LOGGER.warn("[CandyInfection] {}", message);
    }

    public static void debug(String message) {
        if (debugEnabled) {
            LOGGER.info("[CandyInfection] [debug] {}", message);
        }
    }
}
