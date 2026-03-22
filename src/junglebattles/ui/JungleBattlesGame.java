package junglebattles.ui;

import junglebattles.model.Animal;
import junglebattles.model.AnimalGroup;
import junglebattles.model.DemoData;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.List;

// This is the small entry point for the Swing application. It opens the menu first and then
// hands the real simulation work to the main game panel.
public class JungleBattlesGame {
    /*
     * Swing setup starts on the event dispatch thread so the interface stays responsive.
     * The main method stays short because all screen building happens in helper methods
     * below.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(JungleBattlesGame::showMainMenu);
    }

    /*
     * The menu is intentionally simple so the project opens straight into the interesting
     * part of the program. It gives the player one clear action and leaves room for future
     * menu items later.
     */
    private static void showMainMenu() {
        JFrame frame = new JFrame("Jungle Battles Game");
        frame.setSize(560, 380);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setJMenuBar(buildMenuBar(frame));

        JButton startGameButton = new JButton("Open Roaming Arena");
        startGameButton.setBorder(new LineBorder(Color.BLACK, 2, true));
        startGameButton.setBackground(new Color(144, 238, 144));
        startGameButton.setForeground(Color.BLACK);
        startGameButton.setFocusPainted(false);
        startGameButton.setOpaque(true);
        startGameButton.setPreferredSize(new Dimension(240, 58));
        startGameButton.addActionListener(e -> openGameScreen());

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(244, 248, 239));
        centerPanel.add(startGameButton);

        frame.add(centerPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    /*
     * The menu bar is intentionally modest because the project is centered on the live arena.
     * It still gives the window a familiar desktop feel and leaves a natural spot for future
     * features later on.
     */
    private static JMenuBar buildMenuBar(JFrame parentFrame) {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenu helpMenu = new JMenu("Help");

        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> JOptionPane.showMessageDialog(
                parentFrame,
                "Open is not wired up yet. Add save/load later in a dedicated persistence class.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE));

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> JOptionPane.showMessageDialog(
                parentFrame,
                "Save is not wired up yet. This design keeps persistence separate from the UI.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE));

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                parentFrame,
                "Jungle Battles Game\nLarge roaming arena with separate hunting and combat systems.",
                "About",
                JOptionPane.INFORMATION_MESSAGE));

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    // Opening the arena creates a fresh world each time from the demo data. That makes
    // repeated testing easy and avoids carrying stale state across sessions.
    private static void openGameScreen() {
        JFrame gameFrame = new JFrame("Jungle Battles - Roaming Savanna");
        gameFrame.setSize(1480, 940);
        gameFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        gameFrame.setLocationRelativeTo(null);

        List<AnimalGroup<Animal>> groups = DemoData.createDefaultGroups();
        gameFrame.add(new GamePanel(groups), BorderLayout.CENTER);
        gameFrame.setVisible(true);
    }
}
