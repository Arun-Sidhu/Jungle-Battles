package junglebattles.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// This class represents one named group on the map. It keeps animals of the same species
// together and provides a small set of helpers for counting, cleanup, and display.
public class AnimalGroup<T extends Animal> {
    private final String groupName;
    private final Species species;
    private final List<T> animals;

    // A group is tied to one species from the start so the arena can trust its label and
    // keep the simulation logic simple.
    public AnimalGroup(String groupName, Species species) {
        this.groupName = Objects.requireNonNull(groupName, "groupName must not be null");
        this.species = Objects.requireNonNull(species, "species must not be null");
        this.animals = new ArrayList<>();
    }

    public String getGroupName() {
        return groupName;
    }

    public Species getSpecies() {
        return species;
    }

    // Groups only accept animals of the species they were built for. That guard prevents
    // accidental mix ups and makes the zone labels match what the player sees.
    public void add(T animal) {
        Objects.requireNonNull(animal, "animal must not be null");
        if (animal.getSpecies() != species) {
            throw new IllegalArgumentException(
                    "Cannot add " + animal.getSpecies().getDisplayName() + " to group for " + species.getDisplayName());
        }
        animals.add(animal);
    }

    public T removeFirst() {
        if (animals.isEmpty()) {
            return null;
        }
        return animals.remove(0);
    }

    public boolean remove(T animal) {
        return animals.remove(animal);
    }

    public List<T> getAnimals() {
        return Collections.unmodifiableList(animals);
    }

    // Many world events only need one living animal from the group. This method keeps that
    // choice consistent everywhere it is used.
    public T findFirstAlive() {
        for (T animal : animals) {
            if (animal.isAlive()) {
                return animal;
            }
        }
        return null;
    }

    // The living count is shown all over the interface, so it lives here with the group
    // instead of being recomputed in every panel.
    public int livingCount() {
        int count = 0;
        for (T animal : animals) {
            if (animal.isAlive()) {
                count++;
            }
        }
        return count;
    }

    // Dead animals are removed in one pass after world events resolve. This keeps the live
    // list clean without forcing every combat path to duplicate removal logic.
    public void removeDeadAnimals() {
        animals.removeIf(animal -> !animal.isAlive());
    }

    public int size() {
        return animals.size();
    }

    public boolean isEmpty() {
        return animals.isEmpty();
    }

    /*
     * This summary is meant for quick inspection windows. It reads like a roster and makes
     * it easy to see who is still standing without opening the full map view.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(groupName)
                .append(" (")
                .append(livingCount())
                .append(" alive of ")
                .append(size())
                .append("):\n");
        for (T animal : animals) {
            sb.append(" - ").append(animal);
            if (!animal.isAlive()) {
                sb.append(" [DEAD]");
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
