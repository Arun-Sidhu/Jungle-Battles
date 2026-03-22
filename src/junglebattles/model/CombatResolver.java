package junglebattles.model;

import java.util.concurrent.ThreadLocalRandom;

// This class resolves direct fights between animals that choose to stand and trade force. It
// is separate from hunting so heavy herbivores can still be dangerous without being predators.
public class CombatResolver {
    /*
     * A fight compares offensive pressure against defensive resistance with a small random
     * swing. The numbers are meant to feel lively without making the outcome look
     * completely arbitrary.
     */
    public CombatResult resolveFight(Animal attacker, Animal defender) {
        if (attacker == null || defender == null) {
            return new CombatResult(false, false, false, false,
                    "A fight needs two living animals.", attacker, defender, 0, 0);
        }

        if (!attacker.isAlive()) {
            return new CombatResult(false, false, true, false,
                    attacker.getName() + " is not alive and cannot fight.", attacker, defender, 0, 0);
        }

        if (!defender.isAlive()) {
            return new CombatResult(false, true, false, true,
                    defender.getName() + " is already down.", attacker, defender, 0, 0);
        }

        int attackRoll = attacker.getStrength() * 2 + attacker.getSpeed() + getChargeBonus(attacker) + random(0, 20);
        int defendRoll = defender.getDefense() * 2 + defender.getStrength() + getBraceBonus(defender) + random(0, 20);

        int damageToAttacker = 0;
        int damageToDefender = 0;
        boolean attackerWon = attackRoll >= defendRoll;

        if (attackerWon) {
            damageToDefender = Math.max(8,
                    attacker.getStrength() + getChargeBonus(attacker) / 2 - defender.getDefense() / 2 + random(0, 8));
            defender.takeDamage(damageToDefender);

            damageToAttacker = Math.max(0,
                    defender.getStrength() / 4 + defender.getDefense() / 5 - attacker.getDefense() / 6 + random(0, 4));
            attacker.takeDamage(damageToAttacker);

            String message = attacker.getName() + " the " + attacker.getSpecies().getDisplayName()
                    + " won the clash against " + defender.getName() + " the " + defender.getSpecies().getDisplayName() + ".";
            if (!defender.isAlive()) {
                message += " " + defender.getName() + " was taken out in the fight.";
            }
            return new CombatResult(true, true, !attacker.isAlive(), !defender.isAlive(),
                    message, attacker, defender, damageToAttacker, damageToDefender);
        }

        damageToAttacker = Math.max(8,
                defender.getStrength() + getBraceBonus(defender) / 2 - attacker.getDefense() / 2 + random(0, 8));
        attacker.takeDamage(damageToAttacker);

        damageToDefender = Math.max(0,
                attacker.getStrength() / 4 - defender.getDefense() / 6 + random(0, 4));
        defender.takeDamage(damageToDefender);

        String message = defender.getName() + " the " + defender.getSpecies().getDisplayName()
                + " held the line and beat back " + attacker.getName() + " the " + attacker.getSpecies().getDisplayName() + ".";
        if (!attacker.isAlive()) {
            message += " " + attacker.getName() + " was taken out in the fight.";
        }
        return new CombatResult(true, false, !attacker.isAlive(), !defender.isAlive(),
                message, attacker, defender, damageToAttacker, damageToDefender);
    }

    // Heavy animals get a little extra push here so a straight line charge feels scary and
    // matches the role text shown in the interface.
    private int getChargeBonus(Animal animal) {
        Species species = animal.getSpecies();
        if (species == Species.RHINO) {
            return 10;
        }
        if (species == Species.ELEPHANT) {
            return 8;
        }
        if (species == Species.LION) {
            return 4;
        }
        return 0;
    }

    // Some animals are built to absorb pressure and hold position. This bonus gives that
    // feeling a direct place in the combat math.
    private int getBraceBonus(Animal animal) {
        Species species = animal.getSpecies();
        if (species == Species.ELEPHANT) {
            return 10;
        }
        if (species == Species.RHINO) {
            return 8;
        }
        return 0;
    }

    // A small random swing keeps repeated clashes from feeling mechanical while still
    // letting the stat lines matter most of the time.
    private int random(int min, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(min, maxInclusive + 1);
    }
}
