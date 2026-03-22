package junglebattles.model;

import java.util.Objects;

// This class keeps the core state for one animal. It stores the species, combat stats, and
// health so the rest of the game can treat each creature in a consistent way.
public class Animal {
    private final String name;
    private final Species species;
    private final int maxHealth;
    private int health;
    private int strength;
    private int speed;
    private int defense;

    // This constructor is the easy path for normal animals. It pulls the default stats
    // from the species so most callers do not need to think about raw numbers.
    public Animal(String name, Species species) {
        this(name,
                species,
                species.getDefaultHealth(),
                species.getDefaultStrength(),
                species.getDefaultSpeed(),
                species.getDefaultDefense());
    }

    /*
     * This constructor is the full control version for the class. It is useful when a
     * caller wants to override the normal species defaults and build a custom animal for
     * testing or future features.
     */
    public Animal(String name, Species species, int health, int strength, int speed, int defense) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.species = Objects.requireNonNull(species, "species must not be null");
        if (health <= 0 || strength < 0 || speed < 0 || defense < 0) {
            throw new IllegalArgumentException("health must be positive and stats cannot be negative");
        }
        this.maxHealth = health;
        this.health = health;
        this.strength = strength;
        this.speed = speed;
        this.defense = defense;
    }

    public String getName() {
        return name;
    }

    public Species getSpecies() {
        return species;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getStrength() {
        return strength;
    }

    public int getSpeed() {
        return speed;
    }

    public int getDefense() {
        return defense;
    }

    public String getImagePath() {
        return species.getImagePath();
    }

    public boolean isAlive() {
        return health > 0;
    }

    // This helper gives the rest of the game one quick number for rough comparisons.
    // It is not the whole story, but it is handy for summaries and quick checks.
    public int getCombatPower() {
        return strength + speed + defense;
    }

    // Damage handling stays here so every fight and trample uses the same rule. Health
    // never drops below zero, which keeps the rest of the code simple and predictable.
    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage must be non-negative");
        }
        health = Math.max(0, health - damage);
    }

    // Healing is clamped to the original maximum health. That keeps a wounded animal from
    // growing past the limits that came from its species.
    public void heal(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("heal amount must be non-negative");
        }
        health = Math.min(maxHealth, health + amount);
    }

    // The text view keeps debugging friendly because one animal can explain itself in a
    // compact line when it appears in dialogs or logs.
    @Override
    public String toString() {
        return species.getDisplayName() + " " + name +
                " [HP=" + health + "/" + maxHealth +
                ", STR=" + strength +
                ", SPD=" + speed +
                ", DEF=" + defense + "]";
    }
}
