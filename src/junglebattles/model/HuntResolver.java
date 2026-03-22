package junglebattles.model;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

// This class handles predator and prey encounters where eating is the goal. It uses the food
// web for legality and then compares pursuit against escape to decide the result.
public class HuntResolver {
    private final FoodWeb foodWeb;

    // Hunting depends on the shared food web so this resolver never has to guess which
    // matches are legal.
    public HuntResolver(FoodWeb foodWeb) {
        this.foodWeb = Objects.requireNonNull(foodWeb, "foodWeb must not be null");
    }

    // The arena looks at the same food web to drive awareness and auto chasing behavior.
    public FoodWeb getFoodWeb() {
        return foodWeb;
    }

    // A hunt is built around pressure and escape rather than a straight brawl. Fast prey
    // can survive by slipping away, while a strong predator can finish the chase quickly.
    public HuntResult resolveHunt(Animal predator, Animal prey) {
        if (predator == null || prey == null) {
            return new HuntResult(false, false, false,
                    "A hunt needs both a predator and a prey animal.",
                    predator, prey, 0, 0);
        }

        if (!predator.isAlive()) {
            return new HuntResult(false, false, false,
                    predator.getName() + " is not alive and cannot hunt.",
                    predator, prey, 0, 0);
        }

        if (!prey.isAlive()) {
            return new HuntResult(false, false, false,
                    prey.getName() + " is already dead.",
                    predator, prey, 0, 0);
        }

        if (!foodWeb.canEat(predator, prey)) {
            return new HuntResult(false, false, false,
                    predator.getSpecies().getDisplayName() + " cannot eat " + prey.getSpecies().getDisplayName() + ".",
                    predator, prey, 0, 0);
        }

        int predatorRoll = predator.getStrength() * 2 + predator.getSpeed() + random(0, 20);
        int preyEscapeRoll = prey.getSpeed() * 2 + prey.getDefense() + random(0, 20);

        if (predatorRoll >= preyEscapeRoll) {
            int damageToPrey = prey.getHealth();
            prey.takeDamage(damageToPrey);
            int healAmount = Math.max(5, predator.getStrength() / 4);
            predator.heal(healAmount);
            String message = predator.getName() + " the " + predator.getSpecies().getDisplayName()
                    + " hunted and ate " + prey.getName() + " the " + prey.getSpecies().getDisplayName() + ".";
            return new HuntResult(true, true, true, message, predator, prey, 0, damageToPrey);
        }

        int counterDamage = Math.max(3, prey.getStrength() / 3 + prey.getDefense() / 4);
        predator.takeDamage(counterDamage);
        String message = predator.getName() + " the " + predator.getSpecies().getDisplayName()
                + " failed to catch " + prey.getName() + ". " + prey.getName() + " escaped and fought back.";
        return new HuntResult(true, false, false, message, predator, prey, counterDamage, 0);
    }

    // The random roll is small enough to keep stats meaningful while still making repeated
    // hunts feel a little less scripted.
    private int random(int min, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(min, maxInclusive + 1);
    }
}
