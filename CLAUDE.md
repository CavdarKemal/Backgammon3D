# Backgammon3D - Projektkontext für Claude

## Projektübersicht
3D-Backgammon-Spiel in JavaFX mit TD-Gammon KI (Temporal Difference Learning).

## Technologie-Stack
- **Java 21** (Wichtig: Java 25 ist NICHT kompatibel mit DeepLearning4J!)
- **JavaFX 21.0.1** - 3D-Rendering & UI
- **DeepLearning4J 1.0.0-M2.1** - Neuronales Netz für TD-Learning
- **Maven** - Build-System
- **JUnit 5** - Tests

## Projektstruktur

```
src/main/java/com/backgammon3d/
├── Launcher.java              # Entry Point (für JavaFX Module)
├── Main.java                  # JavaFX Application, Game Controller
│
├── model/                     # Spiellogik (GETESTET - 217 Tests bestanden)
│   ├── GameState.java         # Board-Zustand, Spieler, Bar, BearOff
│   ├── Move.java              # Einzelner Zug (from, to, dieValue, hit)
│   ├── MoveGenerator.java     # Alle gültigen Züge generieren
│   ├── BackgammonRules.java   # Regelvalidierung, calculateTarget()
│   └── Dice.java              # Würfel-Logik (inkl. Pasch → 4 Züge)
│
├── ai/                        # KI-Strategien
│   ├── Player.java            # Interface für Spieler
│   ├── HumanPlayer.java       # Wartet auf UI-Input
│   ├── RandomPlayer.java      # Zufällige Züge (Baseline)
│   └── TDPlayer.java          # TD-Gammon Implementierung
│
├── neural/                    # TD-Learning Komponenten
│   ├── BoardEncoder.java      # GameState → float[198] (Tesauro Encoding)
│   ├── TDNetwork.java         # Neuronales Netz (198→80→1)
│   ├── TDTrainer.java         # Self-Play Training
│   └── ModelPersistence.java  # Speichern/Laden des Modells
│
└── view/                      # 3D-Darstellung
    ├── BoardView.java         # SubScene mit 3D-Board, Kamerasteuerung
    ├── PointView.java         # Einzelner Punkt (Dreieck)
    ├── CheckerView.java       # Spielstein (Zylinder, Cyan/Braun)
    ├── DiceView.java          # 3D-Würfel mit Animation
    └── BarView.java           # Gefangene Steine
```

## Board-Repräsentation (WICHTIG)

```
Points 0-23 (Array-Index):
- Positiv = Weiße Steine
- Negativ = Schwarze Steine

Weiß (Cyan): bewegt sich von HOHEN zu NIEDRIGEN Indizes (→ Point 0 → BearOff)
Schwarz:     bewegt sich von NIEDRIGEN zu HOHEN Indizes (→ Point 23 → BearOff)

Bar Entry:
- Weiß: tritt bei Points 18-23 ein (die 1 → index 23, die 6 → index 18)
- Schwarz: tritt bei Points 0-5 ein (die 1 → index 0, die 6 → index 5)

Startposition:
- Weiß: points[0]=2, points[11]=5, points[16]=3, points[18]=5
- Schwarz: points[23]=-2, points[12]=-5, points[7]=-3, points[5]=-5
```

## Aktueller Stand

### Funktioniert:
- ✅ 3D-Board-Rendering mit Kamerasteuerung (Drehen, Zoomen, Verschieben)
- ✅ Spiellogik (217 Unit-Tests bestanden)
- ✅ TD-Gammon KI Training (Self-Play)
- ✅ KI-Geschwindigkeits-Slider
- ✅ Abbruch-Button während des Spiels
- ✅ Control-Sperre während des Spiels
- ✅ KI-Zug-Visualisierung (Magenta Highlight)
- ✅ Farben: Weiß=Cyan, Schwarz=Dunkelbraun

### AKTUELLES PROBLEM (zu untersuchen):
**"Die gewürfelten Steine werden falsch gespielt"**

Die Model-Tests bestehen alle, also liegt das Problem in der UI-Schicht:
- Möglicherweise in `Main.java` - wie Würfel an MoveGenerator übergeben werden
- Möglicherweise in `BoardView.java` - wie Züge visualisiert werden
- Möglicherweise Synchronisation zwischen Model und View

**Nächster Schritt:** User fragen, WAS GENAU falsch ist:
- Falsche Würfelwerte verwendet?
- Falsche Zugrichtung?
- Falscher Stein bewegt?
- Nicht alle Würfel verwendet?

## Wichtige Methoden

### BackgammonRules.calculateTarget()
```java
// Weiß: target = from - dieValue (oder BEAR_OFF wenn < 0)
// Schwarz: target = from + dieValue (oder BEAR_OFF wenn >= 24)
// Bar Entry Weiß: target = 24 - dieValue
// Bar Entry Schwarz: target = dieValue - 1
```

### Dice.getMovesFromRoll()
```java
// Normal: [d1, d2]
// Pasch: [d1, d1, d1, d1]
```

## Befehle

```bash
# Bauen und Starten
mvn clean javafx:run

# Tests ausführen
mvn test

# Training starten (im Programm: Train-Button)
```

## Git Repository
https://github.com/CavdarKemal/Backgammon3D

## Letzte Commits
- `9c934fe` - Umfassende Unit-Tests für Spiellogik (217 Tests)
- `703f7ac` - Update Java target version to 21
- `676bfed` - Fix ND4J dependency resolution für Windows
- `7a5fc6f` - Aktualisiertes TD-Gammon Trainingsmodell
- `1640aac` - KI-Geschwindigkeits-Slider und Control-Sperre
- `7789a15` - 3D-Kamerasteuerung: Drehen, Zoomen, Verschieben

## Bekannte Einschränkungen
- DeepLearning4J ist nicht kompatibel mit Java 25 (nur bis Java 21)
- nd4j-native-platform hat Modul-Probleme, daher nd4j-native mit Windows-Classifier

## Für nächste Session
1. User fragen: **Was genau ist das falsche Verhalten?**
2. Main.java untersuchen - insbesondere:
   - `executeAIMovesWithDelay()`
   - `handleHumanMove()`
   - Wie `currentDice` verwendet wird
3. BoardView.java prüfen - wie Züge auf dem Board angezeigt werden
