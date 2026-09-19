package dev.candyinfection.client;

/**
 * Client-side mirror of the server's infection data, updated by the
 * {@code infection_sync} payload. Kept in one place so the HUD and any future
 * client effects read a single, cheap value instead of asking the server.
 */
public final class CandyClientState {
    private static float infection;
    private static int stage = 1;
    private static int nearbyInfected;
    private static long lastUpdate;

    private CandyClientState() {
    }

    public static void update(float newInfection, int newStage, int newNearby) {
        infection = Math.max(0.0F, Math.min(100.0F, newInfection));
        stage = Math.max(1, Math.min(7, newStage));
        nearbyInfected = Math.max(0, newNearby);
        lastUpdate = System.currentTimeMillis();
    }

    public static float infection() {
        return infection;
    }

    public static int stage() {
        return stage;
    }

    public static int nearbyInfected() {
        return nearbyInfected;
    }

    /** Resets on disconnect so a stale meter never shows in the main menu. */
    public static void reset() {
        infection = 0.0F;
        stage = 1;
        nearbyInfected = 0;
        lastUpdate = 0L;
    }

    public static long lastUpdate() {
        return lastUpdate;
    }

    /** Stage name shown in the HUD. */
    public static String stageLabel() {
        return switch (stage) {
            case 1 -> "EARLY";
            case 2 -> "SPREADING";
            case 3 -> "ADVANCED";
            case 4 -> "CRITICAL";
            case 5 -> "SEVERE";
            case 6 -> "OVERRUN";
            default -> "COLONY";
        };
    }
}
