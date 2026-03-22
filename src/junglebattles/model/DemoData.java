package junglebattles.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// This class builds the starting cast for the arena. The names and group sizes are gathered in
// one place so it is easy to tune the simulation without touching the movement or combat code.
public final class DemoData {
    private static final Map<Species, String[]> NAME_POOLS = new EnumMap<>(Species.class);

    static {
        NAME_POOLS.put(Species.LION, new String[]{
                "Simba", "Mufasa", "Nala", "Kito", "Zuberi", "Aza", "Kali", "Jengo", "Tamu", "Sefu"
        });
        NAME_POOLS.put(Species.HYENA, new String[]{
                "Shenzi", "Banzai", "Ed", "Kopa", "Tano", "Jengo", "Pili", "Sefu", "Leta", "Duma"
        });
        NAME_POOLS.put(Species.RHINO, new String[]{
                "Kifaru", "Baraka", "Jabali", "Tembo", "Zuri", "Imara", "Moyo", "Taji", "Pendo", "Rafiki"
        });
        NAME_POOLS.put(Species.ELEPHANT, new String[]{
                "Amani", "Zola", "Jabari", "Kito", "Nuru", "Lulu", "Sanaa", "Kijani", "Amara", "Bora"
        });
        NAME_POOLS.put(Species.ZEBRA, new String[]{
                "Zuri", "Pinda", "Mara", "Nia", "Kendi", "Tisa", "Binti", "Raini", "Mwezi", "Lela"
        });
        NAME_POOLS.put(Species.ANTELOPE, new String[]{
                "Asha", "Nuru", "Paka", "Lela", "Mosi", "Kio", "Taji", "Bibi", "Nia", "Moyo"
        });
    }

    private DemoData() {
    }

    // The default world starts with a few named groups so the map feels alive right away.
    // Each group is filled with animals that match the same species and role.
    public static List<AnimalGroup<Animal>> createDefaultGroups() {
        List<AnimalGroup<Animal>> groups = new ArrayList<>();
        groups.add(buildGroup("Herd of Antelope", Species.ANTELOPE, 12));
        groups.add(buildGroup("Pride of Lions", Species.LION, 3, "Simba", "Mufasa", "Nala"));
        groups.add(buildGroup("Clan of Hyenas", Species.HYENA, 9,
                "Shenzi", "Banzai", "Ed", "Kopa", "Tano", "Jengo", "Pili", "Sefu", "Leta"));
        groups.add(buildGroup("Crash of Rhinos", Species.RHINO, 4,
                "Kifaru", "Baraka", "Jabali", "Zuri"));
        groups.add(buildGroup("Herd of Zebras", Species.ZEBRA, 12));
        groups.add(buildGroup("Elephant Herd", Species.ELEPHANT, 9,
                "Amani", "Zola", "Jabari", "Kito", "Nuru", "Lulu", "Sanaa", "Kijani", "Amara"));
        return groups;
    }

    // This overload is the quick path for a normal group with generated names. It keeps
    // setup code short when a hand picked roster is not important.
    public static AnimalGroup<Animal> buildGroup(String groupName, Species species, int count) {
        AnimalGroup<Animal> group = new AnimalGroup<>(groupName, species);
        for (int i = 0; i < count; i++) {
            group.add(createAnimalForGroup(group));
        }
        return group;
    }

    /*
     * This version starts with any names that were provided and then fills the rest from the
     * matching name pool. That gives the opening cast some personality without adding extra
     * setup work everywhere else.
     */
    public static AnimalGroup<Animal> buildGroup(String groupName, Species species, int count, String... names) {
        AnimalGroup<Animal> group = new AnimalGroup<>(groupName, species);
        int usedNames = 0;
        if (names != null) {
            for (String name : names) {
                if (group.size() >= count) {
                    break;
                }
                group.add(new Animal(name, species));
                usedNames++;
            }
        }
        for (int i = usedNames; i < count; i++) {
            group.add(createAnimalForGroup(group));
        }
        return group;
    }

    public static Animal createAnimalForGroup(AnimalGroup<Animal> group) {
        if (group == null) {
            throw new IllegalArgumentException("group must not be null");
        }
        return new Animal(nextAnimalName(group.getSpecies(), group), group.getSpecies());
    }

    // Name selection tries the themed pool first and only falls back to numbered names when
    // the pool runs out. That keeps spawned animals feeling a little more alive.
    public static String nextAnimalName(Species species, AnimalGroup<Animal> group) {
        Set<String> usedNames = new HashSet<>();
        if (group != null) {
            for (Animal animal : group.getAnimals()) {
                usedNames.add(animal.getName());
            }
        }

        String[] pool = NAME_POOLS.get(species);
        if (pool != null) {
            for (String candidate : pool) {
                if (!usedNames.contains(candidate)) {
                    return candidate;
                }
            }
        }

        int nextIndex = 1;
        String prefix = species.getDisplayName();
        while (usedNames.contains(prefix + " " + nextIndex)) {
            nextIndex++;
        }
        return prefix + " " + nextIndex;
    }
}
