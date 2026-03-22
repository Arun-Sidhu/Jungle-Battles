package junglebattles.model;

/*
 * This enum is the home for species wide defaults such as health, speed, defense, and role
 * text. That lets individual animals stay lightweight while still feeling different from one
 * another.
 */
public enum Species {
    ANTELOPE("Antelope", "images/antelope.png", DietType.HERBIVORE, 65, 12, 18, 6,
            "Fast prey animal. Relies on speed and evasive movement more than brute force."),
    ELEPHANT("Elephant", "images/elephant.png", DietType.HERBIVORE, 140, 30, 8, 28,
            "Defensive tank. Soaks damage well and can crush attackers with a heavy counter-charge."),
    HYENA("Hyena", "images/hyena.png", DietType.CARNIVORE, 75, 16, 15, 10,
            "Pack fighter. Decent hunter and scrappy in direct combat."),
    LION("Lion", "images/lion.png", DietType.CARNIVORE, 95, 22, 17, 14,
            "Top predator. Strong hunter that also performs well in direct fights."),
    RHINO("Rhino", "images/rhino.png", DietType.HERBIVORE, 130, 28, 9, 24,
            "Bruiser and area-holder. Not a predator, but dangerous in a straight charge."),
    ZEBRA("Zebra", "images/zebra.png", DietType.HERBIVORE, 70, 10, 16, 8,
            "Alert prey animal. Can kick back in a fight but mainly survives by escaping.");

    private final String displayName;
    private final String imagePath;
    private final DietType dietType;
    private final int defaultHealth;
    private final int defaultStrength;
    private final int defaultSpeed;
    private final int defaultDefense;
    private final String combatRole;

    // The enum constructor keeps the species table compact while still carrying enough data
    // to drive combat movement and inspection text.
    Species(String displayName,
            String imagePath,
            DietType dietType,
            int defaultHealth,
            int defaultStrength,
            int defaultSpeed,
            int defaultDefense,
            String combatRole) {
        this.displayName = displayName;
        this.imagePath = imagePath;
        this.dietType = dietType;
        this.defaultHealth = defaultHealth;
        this.defaultStrength = defaultStrength;
        this.defaultSpeed = defaultSpeed;
        this.defaultDefense = defaultDefense;
        this.combatRole = combatRole;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public DietType getDietType() {
        return dietType;
    }

    public int getDefaultHealth() {
        return defaultHealth;
    }

    public int getDefaultStrength() {
        return defaultStrength;
    }

    public int getDefaultSpeed() {
        return defaultSpeed;
    }

    public int getDefaultDefense() {
        return defaultDefense;
    }

    // This text is written for the player, so it stays descriptive instead of sounding like
    // raw engine data.
    public String getCombatRole() {
        return combatRole;
    }
}
