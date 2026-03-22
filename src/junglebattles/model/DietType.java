package junglebattles.model;

// This enum keeps diet labels small and readable. It is mostly used for inspection text so the
// player can quickly tell what kind of role an animal plays in the world.
// The enum stays tiny on purpose because it only needs to answer one simple question about
// how an animal fits into the food rules of the world.
public enum DietType {
    HERBIVORE,
    CARNIVORE,
    OMNIVORE
}
