# Jungle Battles

Jungle Battles is a Java Swing simulation where animals roam a shared world, react to one another, and trigger automatic encounters. Predators chase prey, prey try to survive, and heavy animals like elephants and rhinos can dominate space through direct clashes and trample behavior.

This project is designed as a visual simulation rather than a turn based game. The arena stays active while animals move on their own, and the sidebar lets you inspect groups and individual animals while the world keeps running.

## Features

- Large roaming arena with real time movement
- Automatic world interactions instead of manual attack buttons
- Single click animal inspection
- Health and status display for individual animals
- Group level inspection and arena summaries
- Spawn control for adding new animals during runtime
- Predator and prey behavior
- Combat resolution for direct clashes
- Trample behavior for elephants and rhinos
- Image based animal icons loaded from the resources folder

## Current animal behavior

Different animals fill different roles in the simulation.

- Lions act as predators and pressure prey animals
- Hyenas are aggressive roaming hunters
- Antelope and zebras are survival oriented prey animals
- Elephants are durable tanks that can crush attackers
- Rhinos are bruisers that can win collisions and trample smaller animals

World interactions are automatic. When animals get close enough, the simulation decides whether that encounter becomes a hunt, a clash, or a trample event.


## Why this project is useful

This project is useful because it brings together object oriented design, event driven UI programming, simulation logic, and game style state management in one application. Instead of only printing results to the terminal, it shows how data models and rules can drive visible behavior in a live system.

It is also a strong practice project for structuring a larger Java program. The code separates model logic from UI logic, handles automatic interactions between many moving objects, supports runtime spawning, and gives the user a way to inspect what is happening in real time. That makes it a good example of building software that is both interactive and organized.


## Project structure

```text
src/
└── junglebattles/
    ├── model/
    │   ├── Animal.java
    │   ├── AnimalGroup.java
    │   ├── CombatResolver.java
    │   ├── CombatResult.java
    │   ├── DemoData.java
    │   ├── DietType.java
    │   ├── EncounterEngine.java
    │   ├── FoodWeb.java
    │   ├── HuntResolver.java
    │   ├── HuntResult.java
    │   └── Species.java
    └── ui/
        ├── ArenaPanel.java
        ├── GamePanel.java
        ├── JungleBattlesGame.java
        └── ResourceLoader.java

resources/
└── images/
    ├── Antelope.png
    ├── Elephant.png
    ├── Hyena.png
    ├── Lion.png
    ├── Rhino.png
    └── Zebra.png

README.md
Makefile
.gitignore
```

## How the code is organized

The project is split into two main parts.

The `model` package contains the simulation rules and data objects. This includes animals, groups, species information, predator and prey rules, and the logic that resolves hunts, clashes, and trample events.

The `ui` package contains the Swing interface. This includes the main window, the scrolling arena, the right side control panel, the click based inspection behavior, and image loading.

## Requirements

- Java 17 or newer is recommended
- `make` is optional if you want to use the Makefile
- PNG images should stay inside `resources/images`

## Build and run

### With Makefile

```bash
make
make run
```

### Without Makefile

```bash
mkdir -p out
find src -name "*.java" -print0 | xargs -0 javac -d out
java -cp out:resources junglebattles.ui.JungleBattlesGame
```

## Cleaning the build

```bash
make clean
```

## Rebuilding from scratch

```bash
make rebuild
make run
```

## Controls

- Single click an animal to inspect its health and current status
- Use the group selectors in the sidebar to compare or inspect groups
- Use the spawn control to add another animal into the simulation
- Scroll around the arena to follow movement and interactions

## Future improvements

Some good next upgrades for this project would be

- smoother steering and pathfinding
- herd formation behavior
- richer predator pack tactics
- better spawn balancing
- pause and speed controls
- save and load for simulation states
- more animals and biome types

## Author

Arun Sidhu
