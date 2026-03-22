package junglebattles.model;

import java.util.Objects;

// This class coordinates the higher level encounter rules. It decides whether a meeting
// becomes a hunt or a fight and hands the detailed math to the right resolver.
public class EncounterEngine {
    private final HuntResolver huntResolver;
    private final CombatResolver combatResolver;

    // The engine is built from two smaller rule systems so each one can stay focused on its
    // own job.
    public EncounterEngine(HuntResolver huntResolver, CombatResolver combatResolver) {
        this.huntResolver = Objects.requireNonNull(huntResolver, "huntResolver must not be null");
        this.combatResolver = Objects.requireNonNull(combatResolver, "combatResolver must not be null");
    }

    // These getters let the arena inspect the same shared rules that the engine already
    // uses for real encounters.
    public HuntResolver getHuntResolver() {
        return huntResolver;
    }

    // This keeps the fight logic easy to reach without duplicating resolver creation in the
    // user interface layer.
    public CombatResolver getCombatResolver() {
        return combatResolver;
    }

    /*
     * Hunting pulls one living animal from each side and lets the hunt resolver decide the
     * outcome. Group cleanup happens here so the caller gets a result that already matches
     * the current world state.
     */
    public HuntResult hunt(AnimalGroup<? extends Animal> predatorGroup, AnimalGroup<? extends Animal> preyGroup) {
        if (predatorGroup == null || preyGroup == null) {
            return new HuntResult(false, false, false,
                    "Choose both a hunter group and a prey group.",
                    null, null, 0, 0);
        }

        Animal predator = predatorGroup.findFirstAlive();
        Animal prey = preyGroup.findFirstAlive();

        if (predator == null) {
            return new HuntResult(false, false, false,
                    predatorGroup.getGroupName() + " has no living animals left.",
                    null, prey, 0, 0);
        }

        if (prey == null) {
            return new HuntResult(false, false, false,
                    preyGroup.getGroupName() + " has no living animals left.",
                    predator, null, 0, 0);
        }

        HuntResult result = huntResolver.resolveHunt(predator, prey);
        predatorGroup.removeDeadAnimals();
        preyGroup.removeDeadAnimals();
        return result;
    }

    /*
     * Fighting follows the same group pattern, but it uses the combat rules instead of the
     * food web. This keeps the public interface simple for both inspection and automatic
     * map events.
     */
    public CombatResult fight(AnimalGroup<? extends Animal> attackerGroup, AnimalGroup<? extends Animal> defenderGroup) {
        if (attackerGroup == null || defenderGroup == null) {
            return new CombatResult(false, false, false, false,
                    "Choose both Group A and Group B before fighting.",
                    null, null, 0, 0);
        }

        Animal attacker = attackerGroup.findFirstAlive();
        Animal defender = defenderGroup.findFirstAlive();

        if (attacker == null) {
            return new CombatResult(false, false, true, false,
                    attackerGroup.getGroupName() + " has no living animals left.",
                    null, defender, 0, 0);
        }

        if (defender == null) {
            return new CombatResult(false, true, false, true,
                    defenderGroup.getGroupName() + " has no living animals left.",
                    attacker, null, 0, 0);
        }

        CombatResult result = combatResolver.resolveFight(attacker, defender);
        attackerGroup.removeDeadAnimals();
        defenderGroup.removeDeadAnimals();
        return result;
    }
}
