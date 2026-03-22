package junglebattles.ui;

import junglebattles.model.Animal;
import junglebattles.model.AnimalGroup;
import junglebattles.model.CombatResult;
import junglebattles.model.DietType;
import junglebattles.model.EncounterEngine;
import junglebattles.model.FoodWeb;
import junglebattles.model.HuntResult;
import junglebattles.model.Species;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/*
 * This panel is the living map where animals move, react, and collide. It owns the roaming
 * sprites and updates them over time so the world feels active even when the user does
 * nothing.
 */
public class ArenaPanel extends JPanel {
    // This callback gives the outer panel a clean way to hear about world events without
    // turning the arena into a giant user interface class.
    public interface WorldEventListener {
        void onWorldEvent(String text);
    }

    // Selection changes travel through this callback so the arena can stay focused on map
    // behavior while the sidebar handles text.
    public interface AnimalSelectionListener {
        void onAnimalSelected(String text);
    }

    public static final int WORLD_WIDTH = 1500;
    public static final int WORLD_HEIGHT = 900;

    private static final int SPRITE_SIZE = 58;
    private static final double CONTACT_DISTANCE = 46.0;
    private static final double CHASE_RADIUS = 220.0;
    private static final double THREAT_RADIUS = 180.0;

    private final List<AnimalGroup<Animal>> groups;
    private final EncounterEngine encounterEngine;
    private final FoodWeb foodWeb;
    private final WorldEventListener worldEventListener;
    private final AnimalSelectionListener animalSelectionListener;
    private final Map<Animal, Sprite> sprites;
    private final Map<Species, Rectangle> spawnAreas;
    private final Map<Species, Image> scaledImages;
    private final Timer timer;

    private AnimalGroup<Animal> selectedGroupA;
    private AnimalGroup<Animal> selectedGroupB;
    private Animal selectedAnimal;
    private int worldEventCooldownFrames;

    public ArenaPanel(List<AnimalGroup<Animal>> groups,
                      EncounterEngine encounterEngine,
                      WorldEventListener worldEventListener,
                      AnimalSelectionListener animalSelectionListener) {
        this.groups = groups;
        this.encounterEngine = encounterEngine;
        this.foodWeb = encounterEngine.getHuntResolver().getFoodWeb();
        this.worldEventListener = worldEventListener;
        this.animalSelectionListener = animalSelectionListener;
        this.sprites = new IdentityHashMap<>();
        this.spawnAreas = buildZones();
        this.scaledImages = new EnumMap<>(Species.class);

        setPreferredSize(new Dimension(WORLD_WIDTH, WORLD_HEIGHT));
        setMinimumSize(new Dimension(900, 600));
        setBackground(new Color(10, 132, 61));
        setOpaque(true);

        for (Species species : Species.values()) {
            ImageIcon icon = ResourceLoader.loadIcon(species.getImagePath(), SPRITE_SIZE, SPRITE_SIZE);
            scaledImages.put(species, icon == null ? null : icon.getImage());
        }

        syncAnimals();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e);
            }
        });

        timer = new Timer(33, e -> {
            tick();
            repaint();
        });
        timer.start();
    }

    /*
     * The zones act as soft home areas and as visual landmarks for the map. Animals can
     * roam beyond them, but the labels still help the player understand where each group
     * started.
     */
    private Map<Species, Rectangle> buildZones() {
        Map<Species, Rectangle> map = new EnumMap<>(Species.class);
        map.put(Species.ANTELOPE, new Rectangle(30, 40, 360, 290));
        map.put(Species.LION, new Rectangle(35, 610, 280, 220));
        map.put(Species.ZEBRA, new Rectangle(470, 250, 380, 360));
        map.put(Species.ELEPHANT, new Rectangle(975, 35, 360, 280));
        map.put(Species.RHINO, new Rectangle(1220, 380, 220, 190));
        map.put(Species.HYENA, new Rectangle(1010, 650, 300, 200));
        return map;
    }

    // Highlighting two groups at once makes comparisons easy without forcing the player to
    // pause the world.
    public void setSelectedGroups(AnimalGroup<Animal> groupA, AnimalGroup<Animal> groupB) {
        this.selectedGroupA = groupA;
        this.selectedGroupB = groupB;
        repaint();
    }

    // Selection also adds a small pulse so the chosen animal is easier to spot in a busy
    // crowd.
    public void selectAnimal(Animal animal) {
        this.selectedAnimal = animal;
        if (animal != null) {
            pulseAnimal(animal);
        }
        notifySelectedAnimalChanged();
        repaint();
    }

    // After a hunt or clash both animals pulse for a moment so the eye can catch where the
    // event happened.
    public void pulseEncounter(Animal first, Animal second) {
        pulseAnimal(first);
        pulseAnimal(second);
        repaint();
    }

    private void pulseAnimal(Animal animal) {
        if (animal == null) {
            return;
        }
        Sprite sprite = sprites.get(animal);
        if (sprite != null) {
            sprite.pulseFrames = 28;
        }
    }

    /*
     * This method keeps the sprite list aligned with the current animal groups. It is
     * called after world events and spawns so the arena never drifts away from the real
     * simulation state.
     */
    public void syncAnimals() {
        Map<Animal, Boolean> stillPresent = new IdentityHashMap<>();

        for (AnimalGroup<Animal> group : groups) {
            Rectangle homeZone = spawnAreas.get(group.getSpecies());
            if (homeZone == null) {
                continue;
            }

            for (Animal animal : group.getAnimals()) {
                if (!animal.isAlive()) {
                    continue;
                }
                stillPresent.put(animal, Boolean.TRUE);
                sprites.computeIfAbsent(animal, key -> new Sprite(key, group, homeZone));
            }
        }

        sprites.entrySet().removeIf(entry -> !stillPresent.containsKey(entry.getKey()) || !entry.getKey().isAlive());
    }

    // Each tick updates awareness, movement, and collision driven events in a clear order.
    // That rhythm makes the world easier to reason about and easier to tune later.
    private void tick() {
        syncAnimals();

        if (worldEventCooldownFrames > 0) {
            worldEventCooldownFrames--;
        }

        List<Sprite> snapshot = new ArrayList<>(sprites.values());
        for (Sprite sprite : snapshot) {
            if (!sprite.animal.isAlive()) {
                continue;
            }
            evaluateAwareness(sprite, snapshot);
        }

        for (Sprite sprite : snapshot) {
            if (!sprite.animal.isAlive()) {
                continue;
            }
            updateSprite(sprite, snapshot);
        }

        resolveSpatialInteractions(snapshot);

        if (selectedAnimal != null) {
            notifySelectedAnimalChanged();
        }
    }

    /*
     * Awareness decides what a sprite cares about right now. A predator may lock onto
     * prey, while a vulnerable animal may focus on escape or on keeping space from a
     * heavier threat.
     */
    private void evaluateAwareness(Sprite sprite, List<Sprite> snapshot) {
        sprite.closestPrey = null;
        sprite.closestThreat = null;
        double preyDistance = Double.MAX_VALUE;
        double threatDistance = Double.MAX_VALUE;

        for (Sprite other : snapshot) {
            if (other == sprite || other.group == sprite.group || !other.animal.isAlive()) {
                continue;
            }

            double dx = wrappedDelta(sprite.x, other.x, WORLD_WIDTH);
            double dy = wrappedDelta(sprite.y, other.y, WORLD_HEIGHT);
            double distance = Math.hypot(dx, dy);

            if (distance < CHASE_RADIUS && foodWeb.canEat(sprite.animal, other.animal) && distance < preyDistance) {
                preyDistance = distance;
                sprite.closestPrey = other;
            }

            boolean otherIsThreat = foodWeb.canEat(other.animal, sprite.animal)
                    || (isTank(other.animal.getSpecies()) && isPredator(sprite.animal.getSpecies()));
            if (distance < THREAT_RADIUS && otherIsThreat && distance < threatDistance) {
                threatDistance = distance;
                sprite.closestThreat = other;
            }
        }
    }

    /*
     * Movement blends intent with local steering so animals do not look completely random.
     * Roaming, chasing, fleeing, and spacing each contribute a little to the final
     * direction.
     */
    private void updateSprite(Sprite sprite, List<Sprite> snapshot) {
        if (sprite.wanderCooldown <= 0) {
            double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
            double impulse = 0.04 + ThreadLocalRandom.current().nextDouble(0.08);
            sprite.vx += Math.cos(angle) * impulse;
            sprite.vy += Math.sin(angle) * impulse;
            sprite.wanderCooldown = 10 + ThreadLocalRandom.current().nextInt(24);
        } else {
            sprite.wanderCooldown--;
        }

        applyGroupCohesion(sprite, snapshot);
        applyRoamingTarget(sprite);
        applyInteractionSteering(sprite);
        applySeparation(sprite, snapshot);

        sprite.vx *= 0.97;
        sprite.vy *= 0.97;

        double maxSpeed = 0.75 + sprite.animal.getSpeed() / 12.0;
        if (sprite.closestPrey != null && isPredator(sprite.animal.getSpecies())) {
            maxSpeed += 0.75;
        }
        if (sprite.closestThreat != null && !shouldStandGround(sprite)) {
            maxSpeed += 0.95;
        }
        double velocity = Math.hypot(sprite.vx, sprite.vy);
        if (velocity > maxSpeed && velocity > 0.001) {
            sprite.vx = sprite.vx / velocity * maxSpeed;
            sprite.vy = sprite.vy / velocity * maxSpeed;
        }

        updatePositionWithinBounds(sprite);

        if (sprite.individualCooldownFrames > 0) {
            sprite.individualCooldownFrames--;
        }
        if (sprite.pulseFrames > 0) {
            sprite.pulseFrames--;
        }
    }

    /*
     * Cohesion keeps animals from looking like unrelated particles. Members of the same group
     * lean back toward one another just enough to preserve a loose herd or pack feeling.
     */
    private void applyGroupCohesion(Sprite sprite, List<Sprite> snapshot) {
        double avgDx = 0.0;
        double avgDy = 0.0;
        int count = 0;
        for (Sprite other : snapshot) {
            if (other == sprite || other.group != sprite.group || !other.animal.isAlive()) {
                continue;
            }
            double dx = wrappedDelta(sprite.x, other.x, WORLD_WIDTH);
            double dy = wrappedDelta(sprite.y, other.y, WORLD_HEIGHT);
            double distance = Math.hypot(dx, dy);
            if (distance < 220) {
                avgDx += dx;
                avgDy += dy;
                count++;
            }
        }
        if (count > 0) {
            avgDx /= count;
            avgDy /= count;
            double distance = Math.hypot(avgDx, avgDy);
            if (distance > 42.0) {
                sprite.vx += (avgDx / distance) * 0.030;
                sprite.vy += (avgDy / distance) * 0.030;
            }
        }
    }

    // Roaming gives idle animals something to do so they wander with purpose instead of
    // jittering in place.
    private void applyRoamingTarget(Sprite sprite) {
        if (sprite.roamFrames <= 0 || torusDistance(sprite.x, sprite.y, sprite.targetX, sprite.targetY) < 36.0) {
            sprite.targetX = ThreadLocalRandom.current().nextDouble(0, WORLD_WIDTH);
            sprite.targetY = ThreadLocalRandom.current().nextDouble(0, WORLD_HEIGHT);
            sprite.roamFrames = 80 + ThreadLocalRandom.current().nextInt(160);
        } else {
            sprite.roamFrames--;
        }

        double dx = wrappedDelta(sprite.x, sprite.targetX, WORLD_WIDTH);
        double dy = wrappedDelta(sprite.y, sprite.targetY, WORLD_HEIGHT);
        double distance = Math.hypot(dx, dy);
        if (distance > 0.001) {
            double steer = isPredator(sprite.animal.getSpecies()) ? 0.060 : 0.045;
            if (isTank(sprite.animal.getSpecies())) {
                steer = 0.040;
            }
            sprite.vx += (dx / distance) * steer;
            sprite.vy += (dy / distance) * steer;
        }
    }

    // This is where intent becomes movement. Chasing pushes forward, fleeing pulls away,
    // and tank behavior resists that pressure.
    private void applyInteractionSteering(Sprite sprite) {
        if (sprite.closestThreat != null) {
            double dx = wrappedDelta(sprite.x, sprite.closestThreat.x, WORLD_WIDTH);
            double dy = wrappedDelta(sprite.y, sprite.closestThreat.y, WORLD_HEIGHT);
            double distance = Math.hypot(dx, dy);
            if (distance > 0.001) {
                if (shouldStandGround(sprite)) {
                    sprite.vx += (dx / distance) * 0.23;
                    sprite.vy += (dy / distance) * 0.23;
                } else {
                    sprite.vx -= (dx / distance) * 0.28;
                    sprite.vy -= (dy / distance) * 0.28;
                }
            }
        }

        if (sprite.closestPrey != null && isPredator(sprite.animal.getSpecies())) {
            double dx = wrappedDelta(sprite.x, sprite.closestPrey.x, WORLD_WIDTH);
            double dy = wrappedDelta(sprite.y, sprite.closestPrey.y, WORLD_HEIGHT);
            double distance = Math.hypot(dx, dy);
            if (distance > 0.001) {
                sprite.vx += (dx / distance) * 0.18;
                sprite.vy += (dy / distance) * 0.18;
            }
        }
    }

    // Heavy herbivores are allowed to hold their ground more often because that fits the
    // idea of a dangerous defender.
    private boolean shouldStandGround(Sprite sprite) {
        Species species = sprite.animal.getSpecies();
        if (isTank(species)) {
            return true;
        }
        return species == Species.ZEBRA && sprite.closestThreat != null
                && torusDistance(sprite.x, sprite.y, sprite.closestThreat.x, sprite.closestThreat.y) < 70;
    }

    private void applySeparation(Sprite sprite, List<Sprite> snapshot) {
        for (Sprite other : snapshot) {
            if (other == sprite || !other.animal.isAlive()) {
                continue;
            }
            double dx = -wrappedDelta(sprite.x, other.x, WORLD_WIDTH);
            double dy = -wrappedDelta(sprite.y, other.y, WORLD_HEIGHT);
            double distance = Math.hypot(dx, dy);
            if (distance > 0.001 && distance < 52) {
                double push = (52 - distance) / 52.0;
                sprite.vx += (dx / distance) * push * 0.10;
                sprite.vy += (dy / distance) * push * 0.10;
            }
        }
    }

    /*
     * Once animals overlap closely enough the world decides whether that meeting becomes a
     * trample, a hunt, or a fight. This keeps the automatic interactions tied to actual
     * map position.
     */
    /*
     * Collisions are resolved here after movement settles for the frame. That order keeps the
     * event logic easier to read and stops one contact from being handled twice in the same
     * moment.
     */
    private void resolveSpatialInteractions(List<Sprite> snapshot) {
        if (worldEventCooldownFrames > 0) {
            return;
        }

        for (int i = 0; i < snapshot.size(); i++) {
            Sprite first = snapshot.get(i);
            if (!first.animal.isAlive() || first.individualCooldownFrames > 0) {
                continue;
            }
            for (int j = i + 1; j < snapshot.size(); j++) {
                Sprite second = snapshot.get(j);
                if (!second.animal.isAlive() || second.group == first.group || second.individualCooldownFrames > 0) {
                    continue;
                }

                double distance = torusDistance(first.x, first.y, second.x, second.y);
                if (distance > CONTACT_DISTANCE) {
                    continue;
                }

                if (shouldTrample(first, second)) {
                    afterWorldEncounter(first, second, resolveTrample(first, second));
                    return;
                }
                if (shouldTrample(second, first)) {
                    afterWorldEncounter(first, second, resolveTrample(second, first));
                    return;
                }
                if (foodWeb.canEat(first.animal, second.animal)) {
                    HuntResult result = encounterEngine.hunt(first.group, second.group);
                    afterWorldEncounter(first, second, buildAutoHuntText(first.group, second.group, result));
                    return;
                }
                if (foodWeb.canEat(second.animal, first.animal)) {
                    HuntResult result = encounterEngine.hunt(second.group, first.group);
                    afterWorldEncounter(first, second, buildAutoHuntText(second.group, first.group, result));
                    return;
                }
                if (shouldAutoFight(first, second)) {
                    AnimalGroup<Animal> attackerGroup = pickAttackerGroup(first, second);
                    AnimalGroup<Animal> defenderGroup = attackerGroup == first.group ? second.group : first.group;
                    CombatResult result = encounterEngine.fight(attackerGroup, defenderGroup);
                    afterWorldEncounter(first, second, buildAutoFightText(attackerGroup, defenderGroup, result));
                    return;
                }
            }
        }
    }

    /*
     * Trampling is reserved for the heavy animals that are meant to dominate a lane
     * through force. This gives elephants and rhinos a distinct presence that is different
     * from predator behavior.
     */
    // Trampling is reserved for the heavy animals and only against softer targets so it
    // feels special instead of replacing every other kind of encounter.
    private boolean shouldTrample(Sprite trampler, Sprite target) {
        if (!isTank(trampler.animal.getSpecies()) || isTank(target.animal.getSpecies())) {
            return false;
        }
        if (trampler.group == target.group) {
            return false;
        }
        return trampler.animal.getStrength() + trampler.animal.getDefense() > target.animal.getDefense() + 6;
    }

    private String resolveTrample(Sprite trampler, Sprite target) {
        int damage = 28 + trampler.animal.getStrength() / 3;
        target.animal.takeDamage(damage);
        trampler.group.removeDeadAnimals();
        target.group.removeDeadAnimals();

        StringBuilder sb = new StringBuilder();
        sb.append("Map interaction: ")
                .append(trampler.animal.getName())
                .append(" the ")
                .append(trampler.animal.getSpecies().getDisplayName())
                .append(" trampled ")
                .append(target.animal.getName())
                .append(" the ")
                .append(target.animal.getSpecies().getDisplayName())
                .append(".\n\n")
                .append("Trampler HP: ").append(trampler.animal.getHealth()).append("\n")
                .append("Target HP: ").append(target.animal.getHealth()).append("\n")
                .append(trampler.group.getGroupName()).append(" living: ").append(trampler.group.livingCount()).append("\n")
                .append(target.group.getGroupName()).append(" living: ").append(target.group.livingCount()).append("\n")
                .append("Damage dealt: ").append(damage).append("\n");

        if (!target.animal.isAlive()) {
            sb.append(target.animal.getName()).append(" was knocked out and removed from the arena.");
        } else {
            sb.append(target.animal.getName()).append(" survived the impact.");
        }
        return sb.toString();
    }

    // Once an encounter resolves this helper updates the map feedback and tells the outer
    // panel what happened in plain text.
    private void afterWorldEncounter(Sprite first, Sprite second, String text) {
        first.individualCooldownFrames = 32;
        second.individualCooldownFrames = 32;
        worldEventCooldownFrames = 20;
        syncAnimals();
        pulseEncounter(first.animal, second.animal);
        if (worldEventListener != null) {
            worldEventListener.onWorldEvent(text);
        }
    }

    private boolean shouldAutoFight(Sprite first, Sprite second) {
        Species a = first.animal.getSpecies();
        Species b = second.animal.getSpecies();
        return (isTank(a) && isPredator(b))
                || (isTank(b) && isPredator(a))
                || ((a == Species.ELEPHANT || a == Species.RHINO) && (b == Species.ELEPHANT || b == Species.RHINO));
    }

    private AnimalGroup<Animal> pickAttackerGroup(Sprite first, Sprite second) {
        Species a = first.animal.getSpecies();
        Species b = second.animal.getSpecies();
        if (isTank(a) && isPredator(b)) {
            return first.group;
        }
        if (isTank(b) && isPredator(a)) {
            return second.group;
        }
        int firstInitiative = first.animal.getStrength() + first.animal.getSpeed() + first.animal.getDefense();
        int secondInitiative = second.animal.getStrength() + second.animal.getSpeed() + second.animal.getDefense();
        return firstInitiative >= secondInitiative ? first.group : second.group;
    }

    private boolean isPredator(Species species) {
        return species.getDietType() == DietType.CARNIVORE;
    }

    private boolean isTank(Species species) {
        return species == Species.ELEPHANT || species == Species.RHINO;
    }

    private String buildAutoHuntText(AnimalGroup<Animal> hunterGroup,
                                     AnimalGroup<Animal> preyGroup,
                                     HuntResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Map interaction: ").append(result.getMessage()).append("\n\n");
        if (result.getPredator() != null) {
            sb.append("Hunter: ").append(result.getPredator().getName())
                    .append(" (HP ").append(result.getPredator().getHealth()).append(")\n");
        }
        if (result.getPrey() != null) {
            sb.append("Prey: ").append(result.getPrey().getName())
                    .append(" (HP ").append(result.getPrey().getHealth()).append(")\n");
        }
        sb.append(hunterGroup.getGroupName()).append(" living: ").append(hunterGroup.livingCount()).append("\n");
        sb.append(preyGroup.getGroupName()).append(" living: ").append(preyGroup.livingCount()).append("\n");
        sb.append("Damage to hunter: ").append(result.getDamageToPredator()).append("\n");
        sb.append("Damage to prey: ").append(result.getDamageToPrey()).append("\n");
        sb.append("This happened automatically because the animals met on the map.");
        return sb.toString();
    }

    private String buildAutoFightText(AnimalGroup<Animal> attackerGroup,
                                      AnimalGroup<Animal> defenderGroup,
                                      CombatResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Map interaction: ").append(result.getMessage()).append("\n\n");
        if (result.getAttacker() != null) {
            sb.append("Attacker: ").append(result.getAttacker().getName())
                    .append(" (HP ").append(result.getAttacker().getHealth()).append(")\n");
        }
        if (result.getDefender() != null) {
            sb.append("Defender: ").append(result.getDefender().getName())
                    .append(" (HP ").append(result.getDefender().getHealth()).append(")\n");
        }
        sb.append(attackerGroup.getGroupName()).append(" living: ").append(attackerGroup.livingCount()).append("\n");
        sb.append(defenderGroup.getGroupName()).append(" living: ").append(defenderGroup.livingCount()).append("\n");
        sb.append("Damage to attacker: ").append(result.getDamageToAttacker()).append("\n");
        sb.append("Damage to defender: ").append(result.getDamageToDefender()).append("\n");
        sb.append("This happened automatically because the animals collided on the map.");
        return sb.toString();
    }

    // The name stayed from an earlier wraparound version. The method now just measures plain
    // distance, but keeping one helper still makes later tuning easier.
    private double torusDistance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.hypot(dx, dy);
    }

    // Like the distance helper, this now behaves like a simple delta because the world uses
    // hard boundaries instead of wraparound edges.
    private double wrappedDelta(double from, double to, double limit) {
        return to - from;
    }

    /*
     * Arena movement is kept inside the visible world by reflecting sprites off the edges.
     * That feels cleaner than wraparound for this project and avoids the confusion of
     * icons popping across the map.
     */
    /*
     * Sprites stay inside the arena now, so this method makes them bounce gently off the
     * edges instead of vanishing and reappearing on the opposite side.
     */
    private void updatePositionWithinBounds(Sprite sprite) {
        double nextX = sprite.x + sprite.vx;
        double nextY = sprite.y + sprite.vy;
        double maxX = WORLD_WIDTH - SPRITE_SIZE;
        double maxY = WORLD_HEIGHT - SPRITE_SIZE;

        if (nextX < 0) {
            nextX = 0;
            sprite.vx = Math.abs(sprite.vx) * 0.85;
        } else if (nextX > maxX) {
            nextX = maxX;
            sprite.vx = -Math.abs(sprite.vx) * 0.85;
        }

        if (nextY < 0) {
            nextY = 0;
            sprite.vy = Math.abs(sprite.vy) * 0.85;
        } else if (nextY > maxY) {
            nextY = maxY;
            sprite.vy = -Math.abs(sprite.vy) * 0.85;
        }

        sprite.x = nextX;
        sprite.y = nextY;
    }

    @Override
    // Painting is split into layers so the map stays readable. The grass, zones, animals,
    // and small overlays are drawn in order from background to foreground.
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        paintGrassBackdrop(g2);
        paintSpawnAreas(g2);
        paintAnimals(g2);
        paintHud(g2);

        g2.dispose();
    }

    // The background is simple on purpose because the moving animals and status cues should
    // stay easy to read.
    private void paintGrassBackdrop(Graphics2D g2) {
        g2.setColor(new Color(10, 132, 61));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(255, 255, 255, 16));
        for (int i = 0; i < 90; i++) {
            int x = 20 + i * 21 % Math.max(25, getWidth() - 40);
            int y = 30 + i * 37 % Math.max(25, getHeight() - 60);
            g2.fillOval(x, y, 6, 6);
        }
    }

    // Spawn areas stay visible as landmarks even after animals leave them. They act more
    // like home regions than hard walls.
    private void paintSpawnAreas(Graphics2D g2) {
        Font labelFont = getFont().deriveFont(Font.BOLD, 18f);
        Font countFont = getFont().deriveFont(Font.PLAIN, 13f);

        for (AnimalGroup<Animal> group : groups) {
            Rectangle zone = spawnAreas.get(group.getSpecies());
            if (zone == null) {
                continue;
            }

            boolean selectedA = group == selectedGroupA;
            boolean selectedB = group == selectedGroupB;
            Color outline = new Color(255, 255, 255, 46);
            Color fill = new Color(255, 255, 255, 10);
            if (selectedA && selectedB) {
                outline = new Color(255, 215, 64, 210);
                fill = new Color(255, 215, 64, 46);
            } else if (selectedA) {
                outline = new Color(66, 165, 245, 210);
                fill = new Color(66, 165, 245, 44);
            } else if (selectedB) {
                outline = new Color(255, 112, 67, 210);
                fill = new Color(255, 112, 67, 44);
            }

            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(fill);
            g2.fillRoundRect(zone.x, zone.y, zone.width, zone.height, 32, 32);
            g2.setStroke(new BasicStroke(2.4f));
            g2.setColor(outline);
            g2.drawRoundRect(zone.x, zone.y, zone.width, zone.height, 32, 32);

            g2.setFont(labelFont);
            g2.setColor(Color.WHITE);
            g2.drawString(group.getGroupName(), zone.x + 16, zone.y + 28);
            g2.setFont(countFont);
            g2.drawString("Living: " + group.livingCount(), zone.x + 16, zone.y + 48);
        }
    }

    /*
     * Animals are painted after the map so highlights and labels can sit close to the sprites.
     * Sorting by position gives a steadier draw order and keeps overlapping icons from
     * flickering too much.
     */
    private void paintAnimals(Graphics2D g2) {
        List<Sprite> orderedSprites = new ArrayList<>(sprites.values());
        orderedSprites.sort(Comparator.comparingDouble(sprite -> sprite.y));

        for (Sprite sprite : orderedSprites) {
            boolean selectedA = sprite.group == selectedGroupA;
            boolean selectedB = sprite.group == selectedGroupB;
            boolean selectedSprite = sprite.animal == selectedAnimal;

            for (Point point : getWrappedDrawPositions(sprite.x, sprite.y)) {
                int drawX = point.x;
                int drawY = point.y;

                if (selectedA || selectedB || selectedSprite || sprite.pulseFrames > 0) {
                    Color glowColor = sprite.pulseFrames > 0
                            ? new Color(255, 235, 59, 180)
                            : selectedSprite
                            ? new Color(102, 255, 178, 170)
                            : selectedA && selectedB
                            ? new Color(255, 215, 64, 140)
                            : selectedA
                            ? new Color(66, 165, 245, 140)
                            : new Color(255, 112, 67, 140);
                    g2.setColor(glowColor);
                    g2.fillOval(drawX - 8, drawY - 8, SPRITE_SIZE + 16, SPRITE_SIZE + 16);
                }

                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillOval(drawX + 7, drawY + SPRITE_SIZE - 8, SPRITE_SIZE - 14, 12);

                Image image = scaledImages.get(sprite.animal.getSpecies());
                if (image != null) {
                    g2.drawImage(image, drawX, drawY, SPRITE_SIZE, SPRITE_SIZE, null);
                } else {
                    paintFallbackSprite(g2, sprite.animal, drawX, drawY);
                }

                if (selectedSprite) {
                    paintSelectedAnimalBadge(g2, sprite, drawX, drawY);
                }
            }
        }
    }

    private List<Point> getWrappedDrawPositions(double x, double y) {
        List<Point> positions = new ArrayList<>(1);
        positions.add(new Point((int) Math.round(x), (int) Math.round(y)));
        return positions;
    }

    private void paintFallbackSprite(Graphics2D g2, Animal animal, int x, int y) {
        Color fill;
        switch (animal.getSpecies()) {
            case ELEPHANT:
                fill = new Color(141, 110, 99);
                break;
            case LION:
                fill = new Color(255, 167, 38);
                break;
            case HYENA:
                fill = new Color(189, 189, 189);
                break;
            case RHINO:
                fill = new Color(158, 158, 158);
                break;
            case ZEBRA:
                fill = new Color(245, 245, 245);
                break;
            case ANTELOPE:
            default:
                fill = new Color(121, 85, 72);
                break;
        }

        RoundRectangle2D rounded = new RoundRectangle2D.Double(x, y, SPRITE_SIZE, SPRITE_SIZE, 18, 18);
        g2.setColor(fill);
        g2.fill(rounded);
        g2.setColor(new Color(0, 0, 0, 140));
        g2.draw(rounded);

        String label = animal.getSpecies().getDisplayName().substring(0, Math.min(2, animal.getSpecies().getDisplayName().length())).toUpperCase();
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        g2.setColor(animal.getSpecies() == Species.ZEBRA ? Color.BLACK : Color.WHITE);
        g2.drawString(label, x + 16, y + 33);
    }

    // The small header text gives just enough guidance without covering the active part of
    // the arena.
    private void paintHud(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 230));
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        g2.drawString("Single-click any animal to inspect its health and current status.", 28, 28);
    }

    public String getSelectedAnimalDetails() {
        if (selectedAnimal == null) {
            return "Click an animal icon on the map to inspect its health, stats, and current behavior.";
        }

        Sprite sprite = sprites.get(selectedAnimal);
        if (sprite == null) {
            return selectedAnimal.getName() + " is no longer active in the arena.\n\n"
                    + "Species: " + selectedAnimal.getSpecies().getDisplayName() + "\n"
                    + "Status: Defeated or removed\n"
                    + "Final health: " + selectedAnimal.getHealth() + " / " + selectedAnimal.getMaxHealth();
        }
        return buildAnimalDetails(sprite);
    }

    /*
     * Selection is intentionally direct and happens on a single mouse press. The arena
     * checks which sprite is under the pointer and then updates the inspector through the
     * listener.
     */
    // A single click is enough to inspect an animal, which keeps the interaction quick and
    // friendly while the world keeps moving underneath.
    private void handleMouseClick(MouseEvent e) {
        Sprite clicked = findSpriteAt(e.getX(), e.getY());
        selectedAnimal = clicked == null ? null : clicked.animal;
        if (clicked != null) {
            pulseAnimal(clicked.animal);
        }
        notifySelectedAnimalChanged();
        repaint();
    }

    private Sprite findSpriteAt(int x, int y) {
        List<Sprite> orderedSprites = new ArrayList<>(sprites.values());
        orderedSprites.sort(Comparator.comparingDouble((Sprite sprite) -> sprite.y).reversed());

        for (Sprite sprite : orderedSprites) {
            for (Point point : getWrappedDrawPositions(sprite.x, sprite.y)) {
                Rectangle bounds = new Rectangle(point.x, point.y, SPRITE_SIZE, SPRITE_SIZE);
                if (bounds.contains(x, y)) {
                    return sprite;
                }
            }
        }
        return null;
    }

    // Selection text is built here and pushed outward through the callback so the sidebar
    // always reflects the current pick.
    private void notifySelectedAnimalChanged() {
        if (animalSelectionListener != null) {
            animalSelectionListener.onAnimalSelected(getSelectedAnimalDetails());
        }
    }

    // The detail panel uses this method to turn the selected sprite into human readable
    // text. Keeping that format in one place makes it easy to expand the inspector later.
    private String buildAnimalDetails(Sprite sprite) {
        Animal animal = sprite.animal;
        StringBuilder sb = new StringBuilder();
        sb.append(animal.getName()).append("\n")
                .append("Species: ").append(animal.getSpecies().getDisplayName()).append("\n")
                .append("Group: ").append(sprite.group.getGroupName()).append("\n")
                .append("Status: ").append(animal.isAlive() ? "Alive" : "Defeated").append("\n")
                .append("Behavior: ").append(describeState(sprite)).append("\n")
                .append("Health: ").append(animal.getHealth()).append(" / ").append(animal.getMaxHealth()).append("\n")
                .append("Strength: ").append(animal.getStrength()).append("\n")
                .append("Speed: ").append(animal.getSpeed()).append("\n")
                .append("Defense: ").append(animal.getDefense()).append("\n")
                .append("Diet: ").append(animal.getSpecies().getDietType()).append("\n")
                .append("Role: ").append(animal.getSpecies().getCombatRole()).append("\n")
                .append("Position: (")
                .append((int) Math.round(sprite.x)).append(", ")
                .append((int) Math.round(sprite.y)).append(")");
        return sb.toString();
    }

    private String describeState(Sprite sprite) {
        if (!sprite.animal.isAlive()) {
            return "Defeated";
        }
        if (sprite.closestPrey != null && isPredator(sprite.animal.getSpecies())) {
            return "Hunting " + sprite.closestPrey.animal.getName();
        }
        if (sprite.closestThreat != null) {
            if (shouldStandGround(sprite)) {
                return "Standing ground against " + sprite.closestThreat.animal.getName();
            }
            return "Fleeing from " + sprite.closestThreat.animal.getName();
        }
        return "Roaming";
    }

    private void paintSelectedAnimalBadge(Graphics2D g2, Sprite sprite, int drawX, int drawY) {
        int badgeWidth = 132;
        int badgeX = Math.max(6, Math.min(drawX - 34, WORLD_WIDTH - badgeWidth - 6));
        int badgeY = Math.max(6, drawY - 32);

        g2.setColor(new Color(0, 0, 0, 145));
        g2.fillRoundRect(badgeX, badgeY, badgeWidth, 22, 12, 12);

        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        g2.drawString(sprite.animal.getName(), badgeX + 8, badgeY + 15);

        int barX = Math.max(0, Math.min(drawX, WORLD_WIDTH - SPRITE_SIZE));
        int barY = Math.max(2, drawY - 10);
        int barWidth = SPRITE_SIZE;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(barX, barY, barWidth, 7, 7, 7);

        int hpWidth = (int) Math.round((sprite.animal.getHealth() / (double) sprite.animal.getMaxHealth()) * barWidth);
        g2.setColor(new Color(102, 255, 178));
        g2.fillRoundRect(barX, barY, Math.max(4, hpWidth), 7, 7, 7);
    }

    /*
     * A sprite ties one animal to its visual and movement state on the map. Keeping this
     * helper private lets the arena manage roaming details without leaking them into the
     * rest of the program.
     */
    private static final class Sprite {
        private final Animal animal;
        private final AnimalGroup<Animal> group;
        private final Rectangle spawnArea;
        private double x;
        private double y;
        private double vx;
        private double vy;
        private int wanderCooldown;
        private int pulseFrames;
        private int individualCooldownFrames;
        private int roamFrames;
        private double targetX;
        private double targetY;
        private Sprite closestPrey;
        private Sprite closestThreat;

        /*
         * Each sprite is the moving face of one animal on the map. It stores position and
         * short lived behavior state that the core model does not need to know about.
         */
        private Sprite(Animal animal, AnimalGroup<Animal> group, Rectangle homeZone) {
            this.animal = animal;
            this.group = group;
            this.spawnArea = homeZone;
            this.x = homeZone.x + ThreadLocalRandom.current().nextDouble(Math.max(1, homeZone.width - SPRITE_SIZE));
            this.y = homeZone.y + ThreadLocalRandom.current().nextDouble(Math.max(1, homeZone.height - SPRITE_SIZE));
            this.vx = ThreadLocalRandom.current().nextDouble(-0.8, 0.8);
            this.vy = ThreadLocalRandom.current().nextDouble(-0.8, 0.8);
            this.wanderCooldown = ThreadLocalRandom.current().nextInt(10, 24);
            this.pulseFrames = 0;
            this.individualCooldownFrames = 0;
            this.roamFrames = ThreadLocalRandom.current().nextInt(40, 120);
            this.targetX = ThreadLocalRandom.current().nextDouble(0, WORLD_WIDTH);
            this.targetY = ThreadLocalRandom.current().nextDouble(0, WORLD_HEIGHT);
        }
    }
}
