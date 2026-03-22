package junglebattles.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

// This class defines who can hunt whom. Keeping those rules in one place makes it easy to
// adjust predator behavior without scattering species checks all over the code.
public class FoodWeb {
    private final Map<Species, Set<Species>> preyByPredator = new EnumMap<>(Species.class);

    /*
     * The default table keeps the early simulation easy to understand. Most herbivores never
     * hunt, while lions and hyenas carry the predator pressure on the map.
     */
    public FoodWeb() {
        preyByPredator.put(Species.LION, EnumSet.of(Species.ZEBRA, Species.ANTELOPE, Species.HYENA));
        preyByPredator.put(Species.HYENA, EnumSet.of(Species.ZEBRA, Species.ANTELOPE));
        preyByPredator.put(Species.ELEPHANT, EnumSet.noneOf(Species.class));
        preyByPredator.put(Species.RHINO, EnumSet.noneOf(Species.class));
        preyByPredator.put(Species.ZEBRA, EnumSet.noneOf(Species.class));
        preyByPredator.put(Species.ANTELOPE, EnumSet.noneOf(Species.class));
    }

    public boolean canEat(Species predatorSpecies, Species preySpecies) {
        return preyByPredator
                .getOrDefault(predatorSpecies, EnumSet.noneOf(Species.class))
                .contains(preySpecies);
    }

    // This method answers the design question of whether one animal is prey for another.
    // It does not decide success, only whether the attempt makes sense in the real world.
    public boolean canEat(Animal predator, Animal prey) {
        return predator != null
                && prey != null
                && predator != prey
                && predator.isAlive()
                && prey.isAlive()
                && canEat(predator.getSpecies(), prey.getSpecies());
    }

    // The dialog panel uses this method to explain the hunting rules in plain language to
    // the player.
    public Set<Species> getPreyFor(Species predatorSpecies) {
        Set<Species> prey = preyByPredator.get(predatorSpecies);
        if (prey == null || prey.isEmpty()) {
            return EnumSet.noneOf(Species.class);
        }
        return EnumSet.copyOf(prey);
    }
}
