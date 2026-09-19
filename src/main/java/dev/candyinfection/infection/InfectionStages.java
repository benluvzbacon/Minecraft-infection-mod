package dev.candyinfection.infection;

import net.minecraft.util.math.MathHelper;

/**
 * The seven infection stages. Progression is driven by how long the infection
 * has been active in a world, so a world left alone slowly becomes worse while
 * a world where the player destroys cores stays manageable.
 *
 * <p>Each stage scales <em>variety, frequency and special abilities</em> rather
 * than raw mob health, so the endgame stays winnable.
 */
public enum InfectionStages {
    SEEDING(1, "SEEDING", 0.6F, 0.4F, 0.4F, "Isolated candy growths appear."),
    SPREADING(2, "SPREADING", 1.0F, 0.8F, 0.7F, "Small infected patches form."),
    COLONISING(3, "COLONISING", 1.4F, 1.2F, 1.0F, "Large infected regions take hold."),
    BLOOMING(4, "BLOOMING", 1.8F, 1.6F, 1.4F, "Candy forests and structures appear."),
    INFESTING(5, "INFESTING", 2.2F, 2.2F, 1.8F, "Dangerous monsters become common."),
    OVERGROWTH(6, "OVERGROWTH", 2.6F, 2.8F, 2.2F, "Massive candy colonies dominate."),
    TERMINAL(7, "TERMINAL", 3.0F, 3.4F, 2.6F, "The infection behaves like a world ending ecosystem.");

    public static final int MIN = 1;
    public static final int MAX = 7;

    private final int level;
    private final String label;
    private final float spreadMultiplier;
    private final float spawnMultiplier;
    private final float featureMultiplier;

    InfectionStages(int level, String label, float spreadMultiplier, float spawnMultiplier, float featureMultiplier) {
        this.level = level;
        this.label = label;
        this.spreadMultiplier = spreadMultiplier;
        this.spawnMultiplier = spawnMultiplier;
        this.featureMultiplier = featureMultiplier;
    }

    public static InfectionStages of(int level) {
        return values()[MathHelper.clamp(level, MIN, MAX) - 1];
    }

    public int level() {
        return this.level;
    }

    public String label() {
        return this.label;
    }

    public float spreadMultiplier() {
        return this.spreadMultiplier;
    }

    public float spawnMultiplier() {
        return this.spawnMultiplier;
    }

    public float featureMultiplier() {
        return this.featureMultiplier;
    }

    public String description() {
        return switch (this) {
            case SEEDING -> "Isolated candy growths appear.";
            case SPREADING -> "Small infected patches form.";
            case COLONISING -> "Large infected regions take hold.";
            case BLOOMING -> "Candy forests and structures appear.";
            case INFESTING -> "Dangerous candy monsters become common.";
            case OVERGROWTH -> "Massive candy colonies dominate the landscape.";
            case TERMINAL -> "The infection behaves like a world ending ecosystem.";
        };
    }
}
