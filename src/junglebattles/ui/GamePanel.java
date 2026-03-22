package junglebattles.ui;

import junglebattles.model.Animal;
import junglebattles.model.AnimalGroup;
import junglebattles.model.CombatResolver;
import junglebattles.model.DemoData;
import junglebattles.model.EncounterEngine;
import junglebattles.model.FoodWeb;
import junglebattles.model.HuntResolver;
import junglebattles.model.Species;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

// This panel arranges the arena view and the sidebar into one screen. It acts as the bridge
// between the simulation panel and the text based controls and summaries.
public class GamePanel extends JPanel {
    private final List<AnimalGroup<Animal>> groups;
    private final EncounterEngine encounterEngine;
    private final FoodWeb foodWeb;
    private final ArenaPanel arenaPanel;
    private final JComboBox<AnimalGroup<Animal>> groupABox;
    private final JComboBox<AnimalGroup<Animal>> groupBBox;
    private final JComboBox<Species> spawnSpeciesBox;
    private final JTextArea selectedAnimalArea;
    private final JTextArea groupSummaryArea;
    private final JTextArea resultArea;
    private final JScrollPane selectedAnimalScrollPane;
    private final JScrollPane groupSummaryScrollPane;
    private final JScrollPane resultScrollPane;

    // The panel is built once around a shared list of groups. From there it wires the
    // arena callbacks to the sidebar so selection and world events stay in sync.
    public GamePanel(List<AnimalGroup<Animal>> groups) {
        this.groups = groups;
        this.foodWeb = new FoodWeb();
        this.encounterEngine = new EncounterEngine(new HuntResolver(foodWeb), new CombatResolver());

        setLayout(new BorderLayout(14, 14));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(new Color(28, 97, 47));

        JLabel title = new JLabel("Jungle Battles — Roaming World", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        arenaPanel = new ArenaPanel(groups, encounterEngine, this::handleWorldEvent, this::handleAnimalSelection);
        JScrollPane arenaScroll = new JScrollPane(arenaPanel);
        arenaScroll.getViewport().setBackground(new Color(10, 132, 61));
        arenaScroll.setBorder(BorderFactory.createLineBorder(new Color(15, 70, 36), 3));
        arenaScroll.getHorizontalScrollBar().setUnitIncrement(18);
        arenaScroll.getVerticalScrollBar().setUnitIncrement(18);
        add(arenaScroll, BorderLayout.CENTER);

        groupABox = createGroupComboBox();
        groupBBox = createGroupComboBox();
        groupABox.addActionListener(e -> syncSelection());
        groupBBox.addActionListener(e -> syncSelection());

        spawnSpeciesBox = new JComboBox<>(Species.values());
        spawnSpeciesBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Species) {
                    setText(((Species) value).getDisplayName());
                }
                return this;
            }
        });

        selectedAnimalArea = createTextArea("Click an animal icon on the map to inspect its health, stats, and current behavior.");
        groupSummaryArea = createTextArea("");
        resultArea = createTextArea("World interactions are automatic. Lions hunt prey, prey flee, and elephants/rhinos can trample or clash when they collide.\n\nUse the spawn control to add more animals to the arena.");
        selectedAnimalScrollPane = createScrollPane(selectedAnimalArea);
        groupSummaryScrollPane = createScrollPane(groupSummaryArea);
        resultScrollPane = createScrollPane(resultArea);

        add(buildSidebar(), BorderLayout.EAST);

        refreshSummaries();
        syncSelection();
    }

    // These text areas are used like small reading panes, so wrapping and a steady caret
    // matter more than editable behavior.
    private JTextArea createTextArea(String initialText) {
        JTextArea textArea = new JTextArea(10, 28);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(initialText);
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        textArea.setCaretPosition(0);
        return textArea;
    }

    // Scroll panes are set up the same way each time so the sidebar feels consistent from
    // section to section.
    private JScrollPane createScrollPane(JTextArea textArea) {
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        return scrollPane;
    }

    // The sidebar is meant to support the map instead of competing with it. Inspection text
    // and spawning live here, while the main visual drama stays inside the arena.
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 12));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(365, 100));

        sidebar.add(buildControlsPanel(), BorderLayout.NORTH);

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        textStack.add(wrapSection("Selected Animal", selectedAnimalScrollPane, 190));
        textStack.add(wrapSection("Arena Status", groupSummaryScrollPane, 170));
        textStack.add(wrapSection("Last Action", resultScrollPane, 180));

        sidebar.add(textStack, BorderLayout.CENTER);
        return sidebar;
    }

    /*
     * The control card is meant to feel lightweight because most action happens on the map
     * itself. It focuses on spawning, inspection, and rule summaries instead of manual
     * combat buttons.
     */
    private JPanel buildControlsPanel() {
        JPanel card = createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel hint = new JLabel("Choose groups to highlight or compare.");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setFont(new Font("Arial", Font.PLAIN, 13));

        JLabel hint2 = new JLabel("World interactions are automatic. Single-click any animal to inspect it.");
        hint2.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint2.setFont(new Font("Arial", Font.PLAIN, 13));

        JPanel rowA = new JPanel(new BorderLayout(8, 0));
        rowA.setOpaque(false);
        rowA.add(new JLabel("Group A"), BorderLayout.WEST);
        rowA.add(groupABox, BorderLayout.CENTER);

        JPanel rowB = new JPanel(new BorderLayout(8, 0));
        rowB.setOpaque(false);
        rowB.add(new JLabel("Group B"), BorderLayout.WEST);
        rowB.add(groupBBox, BorderLayout.CENTER);

        JPanel spawnRow = new JPanel(new BorderLayout(8, 0));
        spawnRow.setOpaque(false);
        spawnRow.add(new JLabel("Spawn"), BorderLayout.WEST);
        spawnRow.add(spawnSpeciesBox, BorderLayout.CENTER);

        JPanel buttonGrid = new JPanel(new java.awt.GridLayout(0, 1, 0, 8));
        buttonGrid.setOpaque(false);

        JButton spawnButton = new JButton("Spawn Animal");
        spawnButton.addActionListener(e -> spawnSelectedAnimal());
        buttonGrid.add(spawnButton);

        JButton inspectAButton = new JButton("Inspect Group A");
        inspectAButton.addActionListener(e -> inspectSelectedGroup(groupABox));
        buttonGrid.add(inspectAButton);

        JButton inspectBButton = new JButton("Inspect Group B");
        inspectBButton.addActionListener(e -> inspectSelectedGroup(groupBBox));
        buttonGrid.add(inspectBButton);

        JButton foodWebButton = new JButton("Show Food Web");
        foodWebButton.addActionListener(e -> showFoodWeb());
        buttonGrid.add(foodWebButton);

        JButton combatRolesButton = new JButton("Show Combat Roles");
        combatRolesButton.addActionListener(e -> showCombatRules());
        buttonGrid.add(combatRolesButton);

        card.add(hint);
        card.add(Box.createVerticalStrut(8));
        card.add(hint2);
        card.add(Box.createVerticalStrut(12));
        card.add(rowA);
        card.add(Box.createVerticalStrut(8));
        card.add(rowB);
        card.add(Box.createVerticalStrut(12));
        card.add(spawnRow);
        card.add(Box.createVerticalStrut(8));
        card.add(buttonGrid);
        return card;
    }

    // Each text panel gets the same card shell so the right side reads like one unified
    // tool column.
    private JPanel wrapSection(String title, JComponent content, int preferredHeight) {
        JPanel panel = createCardPanel();
        panel.setLayout(new BorderLayout(0, 8));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight));
        panel.setPreferredSize(new Dimension(350, preferredHeight));

        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 15));
        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(new Color(255, 255, 255, 240));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(22, 77, 40), 2),
                new EmptyBorder(12, 12, 12, 12)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    // The group pickers show live counts so the user can see the state of the world before
    // opening any dialog.
    private JComboBox<AnimalGroup<Animal>> createGroupComboBox() {
        JComboBox<AnimalGroup<Animal>> comboBox = new JComboBox<>();
        for (AnimalGroup<Animal> group : groups) {
            comboBox.addItem(group);
        }
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AnimalGroup) {
                    AnimalGroup<?> group = (AnimalGroup<?>) value;
                    setText(group.getGroupName() + " (" + group.livingCount() + ")");
                }
                return this;
            }
        });
        return comboBox;
    }

    /*
     * Spawning creates a new animal that matches the chosen species and sends it into the
     * arena immediately. The new animal is selected on creation so the player can inspect
     * its stats right away.
     */
    private void spawnSelectedAnimal() {
        Species species = (Species) spawnSpeciesBox.getSelectedItem();
        if (species == null) {
            return;
        }
        AnimalGroup<Animal> group = findGroupForSpecies(species);
        if (group == null) {
            setTextPreservingScroll(resultArea, resultScrollPane, "Could not find a group for " + species.getDisplayName() + ".");
            return;
        }

        Animal animal = DemoData.createAnimalForGroup(group);
        group.add(animal);
        arenaPanel.syncAnimals();
        arenaPanel.selectAnimal(animal);
        setTextPreservingScroll(resultArea, resultScrollPane, "Spawned " + animal.getName() + " in " + group.getGroupName() + ".\n\n"
                + "The new animal starts in its home zone, then free-roams across the whole map.");
        refreshSummaries();
        syncSelection();
    }

    // Spawning works by sending the new animal to its matching home group. That keeps the
    // roster and the map in agreement.
    private AnimalGroup<Animal> findGroupForSpecies(Species species) {
        for (AnimalGroup<Animal> group : groups) {
            if (group.getSpecies() == species) {
                return group;
            }
        }
        return null;
    }

    private void syncSelection() {
        arenaPanel.setSelectedGroups(castGroup(groupABox.getSelectedItem()), castGroup(groupBBox.getSelectedItem()));
    }

    private void handleWorldEvent(String text) {
        setTextPreservingScroll(resultArea, resultScrollPane, text);
        refreshSummaries();
        syncSelection();
    }

    private void handleAnimalSelection(String text) {
        setTextPreservingScroll(selectedAnimalArea, selectedAnimalScrollPane, text);
    }

    /*
     * The side panels refresh often while the world is moving, so this helper protects the
     * current scroll position. That way the user can read older text without the panel
     * snapping back on every update.
     */
    private void setTextPreservingScroll(JTextArea area, JScrollPane scrollPane, String newText) {
        String current = area.getText();
        if (current.equals(newText)) {
            return;
        }

        int verticalValue = scrollPane.getVerticalScrollBar().getValue();
        int horizontalValue = scrollPane.getHorizontalScrollBar().getValue();
        area.setText(newText);
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(verticalValue);
            scrollPane.getHorizontalScrollBar().setValue(horizontalValue);
        });
    }

    private void refreshSummaries() {
        arenaPanel.syncAnimals();
        setTextPreservingScroll(selectedAnimalArea, selectedAnimalScrollPane, arenaPanel.getSelectedAnimalDetails());
        setTextPreservingScroll(groupSummaryArea, groupSummaryScrollPane, buildGroupSummaryText());
        groupABox.repaint();
        groupBBox.repaint();
    }

    /*
     * Group summaries are gathered into one block so the status panel can refresh in one
     * shot. This keeps the interface readable even when counts change many times per
     * second.
     */
    private String buildGroupSummaryText() {
        StringBuilder sb = new StringBuilder();
        for (AnimalGroup<Animal> group : groups) {
            sb.append(group.getGroupName())
                    .append("\n")
                    .append("Species: ").append(group.getSpecies().getDisplayName())
                    .append("\n")
                    .append("Living: ").append(group.livingCount()).append(" / ").append(group.size())
                    .append("\n")
                    .append("Diet: ").append(group.getSpecies().getDietType())
                    .append("\n")
                    .append("Role: ").append(group.getSpecies().getCombatRole())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }

    // Inspection opens a simple roster window because sometimes reading names is faster than
    // hunting around the arena.
    private void inspectSelectedGroup(JComboBox<AnimalGroup<Animal>> comboBox) {
        AnimalGroup<Animal> group = castGroup(comboBox.getSelectedItem());
        if (group == null) {
            return;
        }
        JOptionPane.showMessageDialog(this, group.toString(), group.getGroupName(), JOptionPane.INFORMATION_MESSAGE);
    }

    @SuppressWarnings("unchecked")
    private AnimalGroup<Animal> castGroup(Object item) {
        return (AnimalGroup<Animal>) item;
    }

    // The food web dialog is a quick reminder of which species can legally hunt others. It
    // is a player facing view of the same central rules the simulation already uses.
    private void showFoodWeb() {
        StringBuilder sb = new StringBuilder("Food web rules for automatic hunting:\n\n");
        for (Species species : Species.values()) {
            sb.append(species.getDisplayName()).append(" can eat: ");
            if (foodWeb.getPreyFor(species).isEmpty()) {
                sb.append("nothing");
            } else {
                boolean first = true;
                for (Species prey : foodWeb.getPreyFor(species)) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(prey.getDisplayName());
                    first = false;
                }
            }
            sb.append('\n');
        }
        sb.append("\nThese interactions happen automatically when animals meet on the map.");

        JOptionPane.showMessageDialog(this, sb.toString(), "Food Web", JOptionPane.INFORMATION_MESSAGE);
    }

    /*
     * Combat roles explain why large herbivores still matter even when they are not
     * predators. This gives the player a plain language summary of the design behind each
     * species.
     */
    private void showCombatRules() {
        StringBuilder sb = new StringBuilder("Combat roles and trampling behavior:\n\n");
        for (Species species : Species.values()) {
            sb.append(species.getDisplayName())
                    .append(" — ")
                    .append(species.getCombatRole())
                    .append("\n")
                    .append("Stats: STR ")
                    .append(species.getDefaultStrength())
                    .append(", SPD ")
                    .append(species.getDefaultSpeed())
                    .append(", DEF ")
                    .append(species.getDefaultDefense())
                    .append("\n\n");
        }
        sb.append("Elephants and rhinos can also trample non-tank animals on collision.");

        JOptionPane.showMessageDialog(this, sb.toString(), "Combat Roles", JOptionPane.INFORMATION_MESSAGE);
    }
}
