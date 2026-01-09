# Backgammon3D - TD-Gammon KI

Ein 3D-Backgammon-Spiel in JavaFX mit einer KI, die durch **Temporal Difference Learning** (TD-Gammon) trainiert wird.

![Backgammon3D Screenshot](bild1.png)

## Projektübersicht

Dieses Projekt implementiert:
- **3D-Spielbrett** mit JavaFX 3D-Grafik
- **Vollständige Backgammon-Regeln** (Würfeln, Ziehen, Schlagen, Auswürfeln)
- **TD-Gammon KI** basierend auf Tesauros bahnbrechender Arbeit von 1995
- **Self-Play Training** zum Verbessern der KI
- **Drag & Drop** Steuerung für intuitive Bedienung

## Technologie-Stack

| Komponente | Version | Zweck |
|------------|---------|-------|
| Java | 17+ | Hauptsprache |
| JavaFX | 21.0.1 | 3D-Rendering & UI |
| Maven | 3.8+ | Build-System |
| DeepLearning4J | 1.0.0-M2.1 | Neuronales Netz |

## Installation & Start

```bash
# Repository klonen
git clone https://github.com/CavdarKemal/Backgammon3D.git
cd Backgammon3D

# Kompilieren und starten
mvn clean javafx:run
```

## Spielanleitung

### Gegen die KI spielen
1. Wähle **"Weiß: Mensch"** und **"Schwarz: KI (TD-Gammon)"**
2. Klicke **"Neues Spiel"**
3. Klicke **"Würfeln"**
4. **Ziehe** einen weißen Stein auf ein grün markiertes Zielfeld
5. Nach allen Zügen wechselt der Spieler automatisch

### KI trainieren
1. Klicke **"KI trainieren"** in der Toolbar
2. Wähle Anzahl der Spiele (empfohlen: 10.000+)
3. Klicke **"Training starten"**
4. Nach dem Training: **"Modell speichern"**

Das Modell wird beim nächsten Start automatisch geladen.

---

## TD-Gammon KI - Architektur

### Überblick

```
┌─────────────────────────────────────────────────────────────┐
│                    TD-GAMMON SYSTEM                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   GameState ──► BoardEncoder ──► TDNetwork ──► Bewertung   │
│       │              │               │             │        │
│   Spielbrett    198 Features    Neuronales    P(Gewinn)    │
│   24 Punkte     Tesauro-        Netz          [0, 1]       │
│   Bar, BearOff  Encoding        198→80→1                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### BoardEncoder (198 Features)

Das Encoding basiert auf Tesauros Paper und kodiert:

| Features | Beschreibung |
|----------|--------------|
| 1-96 | Eigene Steine (4 Units × 24 Punkte) |
| 97-192 | Gegner-Steine (4 Units × 24 Punkte) |
| 193-194 | Steine auf der Bar |
| 195-196 | Ausgewürfelte Steine |
| 197-198 | Spieler am Zug (One-Hot) |

**4-Unit Encoding pro Punkt:**
```
0 Steine: (0, 0, 0, 0)
1 Stein:  (1, 0, 0, 0)  ← Blot (kann geschlagen werden)
2 Steine: (1, 1, 0, 0)  ← Blockade
3 Steine: (1, 1, 1, 0)
n Steine: (1, 1, 1, (n-3)/2)
```

### TDNetwork (Neuronales Netz)

```
Eingabe (198)
     │
     ▼
┌─────────────┐
│ Hidden (80) │  Sigmoid-Aktivierung
└─────────────┘
     │
     ▼
┌─────────────┐
│ Ausgabe (1) │  Sigmoid → P(Gewinn) ∈ [0,1]
└─────────────┘

Parameter: 16.001 trainierbare Gewichte
```

### TD(λ) Learning

**Temporal Difference Learning** kombiniert:
- **Monte Carlo**: Lernt aus Spielausgängen
- **Dynamic Programming**: Bootstrapped von Nachfolgezuständen

**Update-Regel:**
```
V(s) ← V(s) + α × [V(s') - V(s)]
```

**Hyperparameter:**
- α (Lernrate) = 0.1
- λ (Eligibility Traces) = 0.7

### TDPlayer (Zugauswahl)

```
Für jeden möglichen Zug:
    1. Simuliere den Zug auf einer Kopie des Spielstands
    2. Kodiere den resultierenden Zustand (BoardEncoder)
    3. Bewerte mit dem neuronalen Netz (TDNetwork)
    4. Wähle den Zug mit der höchsten Gewinnwahrscheinlichkeit
```

### Self-Play Training

```
Für jedes Trainingsspiel:
    1. Initialisiere Spielbrett
    2. Wiederhole bis Spielende:
       a. Würfle
       b. Wähle zufälligen gültigen Zug (Exploration)
       c. Speichere Spielzustand
    3. Nach Spielende:
       - TD(λ) Update für alle gespeicherten Zustände
       - Reward: 1.0 für Gewinner, 0.0 für Verlierer
```

---

## Projektstruktur

```
Backgammon3D/
├── pom.xml                          # Maven Build-Konfiguration
├── README.md                        # Diese Datei
├── td-gammon-model.zip              # Trainiertes Modell (optional)
│
└── src/main/java/com/backgammon3d/
    ├── Launcher.java                # Entry Point
    ├── Main.java                    # JavaFX Application
    │
    ├── model/                       # Spiellogik
    │   ├── GameState.java           # Spielzustand (Brett, Bar, BearOff)
    │   ├── Move.java                # Einzelner Zug
    │   ├── MoveGenerator.java       # Gültige Züge generieren
    │   ├── BackgammonRules.java     # Regelvalidierung
    │   └── Dice.java                # Würfel-Logik
    │
    ├── ai/                          # KI-Spieler
    │   ├── Player.java              # Interface
    │   ├── HumanPlayer.java         # Mensch (UI-gesteuert)
    │   ├── RandomPlayer.java        # Zufalls-KI (Baseline)
    │   └── TDPlayer.java            # TD-Gammon KI
    │
    ├── neural/                      # TD-Learning
    │   ├── BoardEncoder.java        # State → 198 Features
    │   ├── TDNetwork.java           # Neuronales Netz (DL4J)
    │   └── TDTrainer.java           # Self-Play Training
    │
    └── view/                        # 3D-Darstellung
        ├── BoardView.java           # 3D-Spielbrett (SubScene)
        ├── PointView.java           # Dreiecke auf dem Brett
        ├── CheckerView.java         # Spielsteine (Zylinder)
        └── DiceView.java            # 3D-Würfel
```

---

## Referenzen

- **TD-Gammon Paper**: Tesauro, G. (1995). "Temporal Difference Learning and TD-Gammon". Communications of the ACM.
- **Reinforcement Learning**: Sutton, R. S., & Barto, A. G. (2018). "Reinforcement Learning: An Introduction"
- **DeepLearning4J**: https://deeplearning4j.konduit.ai/

---

## Lizenz

MIT License - Frei zur Verwendung und Modifikation.

---

## Autor

Erstellt mit [Claude Code](https://claude.ai/claude-code) - Anthropics KI-Assistent für Softwareentwicklung.
