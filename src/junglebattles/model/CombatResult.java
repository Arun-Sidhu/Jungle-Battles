package junglebattles.model;

// This object is a simple record of what happened in one clash. It carries the result text and
// the important numbers so the world view and sidebar can explain the outcome clearly.
public class CombatResult {
    private final boolean attempted;
    private final boolean attackerWon;
    private final boolean attackerDefeated;
    private final boolean defenderDefeated;
    private final String message;
    private final Animal attacker;
    private final Animal defender;
    private final int damageToAttacker;
    private final int damageToDefender;

    /*
     * The result is stored as a plain data object because several parts of the program need
     * the same outcome at once. The map and the side panels can all read it without having
     * to rerun the fight.
     */
    public CombatResult(boolean attempted,
                        boolean attackerWon,
                        boolean attackerDefeated,
                        boolean defenderDefeated,
                        String message,
                        Animal attacker,
                        Animal defender,
                        int damageToAttacker,
                        int damageToDefender) {
        this.attempted = attempted;
        this.attackerWon = attackerWon;
        this.attackerDefeated = attackerDefeated;
        this.defenderDefeated = defenderDefeated;
        this.message = message;
        this.attacker = attacker;
        this.defender = defender;
        this.damageToAttacker = damageToAttacker;
        this.damageToDefender = damageToDefender;
    }

    public boolean wasAttempted() {
        return attempted;
    }

    public boolean didAttackerWin() {
        return attackerWon;
    }

    public boolean wasAttackerDefeated() {
        return attackerDefeated;
    }

    public boolean wasDefenderDefeated() {
        return defenderDefeated;
    }

    public String getMessage() {
        return message;
    }

    public Animal getAttacker() {
        return attacker;
    }

    public Animal getDefender() {
        return defender;
    }

    public int getDamageToAttacker() {
        return damageToAttacker;
    }

    public int getDamageToDefender() {
        return damageToDefender;
    }
}
