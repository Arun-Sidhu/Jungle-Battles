package junglebattles.model;

// This result object gives the rest of the program a friendly summary of one hunt. It keeps
// the message and damage details together so the sidebar can explain what just happened.
public class HuntResult {
    private final boolean attempted;
    private final boolean successful;
    private final boolean preyEaten;
    private final String message;
    private final Animal predator;
    private final Animal prey;
    private final int damageToPredator;
    private final int damageToPrey;

    /*
     * Like the combat result, this object packages everything the rest of the interface wants
     * to know about one event. It keeps the message and the numbers together in one place.
     */
    public HuntResult(boolean attempted,
                      boolean successful,
                      boolean preyEaten,
                      String message,
                      Animal predator,
                      Animal prey,
                      int damageToPredator,
                      int damageToPrey) {
        this.attempted = attempted;
        this.successful = successful;
        this.preyEaten = preyEaten;
        this.message = message;
        this.predator = predator;
        this.prey = prey;
        this.damageToPredator = damageToPredator;
        this.damageToPrey = damageToPrey;
    }

    public boolean wasAttempted() {
        return attempted;
    }

    public boolean wasSuccessful() {
        return successful;
    }

    public boolean wasPreyEaten() {
        return preyEaten;
    }

    public String getMessage() {
        return message;
    }

    public Animal getPredator() {
        return predator;
    }

    public Animal getPrey() {
        return prey;
    }

    public int getDamageToPredator() {
        return damageToPredator;
    }

    public int getDamageToPrey() {
        return damageToPrey;
    }
}
