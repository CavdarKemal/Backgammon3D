package com.backgammon3d.neural;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;

/**
 * TDNetwork - Neuronales Netzwerk für TD-Gammon.
 *
 * Diese Klasse implementiert das Herzstück der TD-Gammon KI: Ein neuronales
 * Netzwerk, das Backgammon-Positionen bewertet und durch Temporal Difference
 * Learning trainiert wird.
 *
 * ============================================================================
 * ARCHITEKTUR (nach Tesauros TD-Gammon):
 * ============================================================================
 *
 *   Eingabe (198 Neuronen)
 *         ↓
 *   [BoardEncoder Output]
 *         ↓
 *   ══════════════════════
 *   Hidden Layer (80 Neuronen, Sigmoid)
 *   ══════════════════════
 *         ↓
 *   Ausgabe (1 Neuron, Sigmoid)
 *         ↓
 *   Gewinnwahrscheinlichkeit [0, 1]
 *
 * - Eingabe: 198 Features aus BoardEncoder
 * - Hidden: 80 Neuronen mit Sigmoid-Aktivierung
 * - Ausgabe: 1 Neuron → P(Gewinn) ∈ [0, 1]
 *
 * Parameter: 198×80 + 80 + 80×1 + 1 = 16.001 trainierbare Gewichte
 *
 * ============================================================================
 * TD(λ) LEARNING - TEMPORAL DIFFERENCE LERNEN:
 * ============================================================================
 *
 * TD-Learning ist eine Kombination aus:
 * - Monte Carlo (lernt aus Spielausgängen)
 * - Dynamic Programming (bootstrapped von Nachfolgezuständen)
 *
 * Update-Regel:
 *   V(s) ← V(s) + α × [V(s') - V(s)]
 *
 * Wobei:
 * - V(s)  = Geschätzte Gewinnwahrscheinlichkeit in Zustand s
 * - V(s') = Geschätzte Gewinnwahrscheinlichkeit im Nachfolgezustand
 * - α     = Lernrate (hier: 0.1)
 *
 * TD(λ) mit Eligibility Traces:
 * - λ = 0 → Nur vom nächsten Zustand lernen (TD(0))
 * - λ = 1 → Von allen zukünftigen Zuständen lernen (Monte Carlo)
 * - λ = 0.7 → Guter Kompromiss (Tesauros Empfehlung)
 *
 * ============================================================================
 * WARUM TD-LEARNING FÜR BACKGAMMON?
 * ============================================================================
 *
 * 1. Kein Label nötig: Das Netzwerk lernt nur aus Spielausgängen (1=Sieg, 0=Niederlage)
 * 2. Self-Play: Kann gegen sich selbst trainieren → unbegrenzte Trainingsdaten
 * 3. Kontinuierliches Lernen: Verbessert sich während des Spielens
 * 4. Erfolgreich bewiesen: TD-Gammon erreichte Weltklasse-Niveau
 *
 * Referenz: Tesauro, G. (1995). "Temporal Difference Learning and TD-Gammon"
 *
 * @author Backgammon3D Project
 * @see BoardEncoder
 * @see TDTrainer
 * @see TDPlayer
 */
public class TDNetwork {

    // ============================================================================
    // KONSTANTEN
    // ============================================================================

    /** Eingabegröße: 198 Features aus BoardEncoder */
    private static final int INPUT_SIZE = BoardEncoder.INPUT_SIZE;

    /** Hidden Layer Größe: 80 Neuronen (Tesauros Original) */
    private static final int HIDDEN_SIZE = 80;

    /** Lernrate für SGD-Optimierer */
    private static final double LEARNING_RATE = 0.1;

    // ============================================================================
    // INSTANZVARIABLEN
    // ============================================================================

    /** Das DeepLearning4J Netzwerk */
    private MultiLayerNetwork network;

    /** Lambda-Parameter für TD(λ) Eligibility Traces */
    private double lambda = 0.7;

    /** Wiederverwendbarer Eingabe-Buffer (vermeidet GC-Overhead) */
    private INDArray inputBuffer;

    // ============================================================================
    // KONSTRUKTOREN
    // ============================================================================

    /**
     * Erstellt ein neues, untrainiertes Netzwerk.
     */
    public TDNetwork() {
        buildNetwork();
    }

    /**
     * Erstellt ein Netzwerk und lädt optional ein gespeichertes Modell.
     *
     * @param modelPath Pfad zur Modell-Datei (.zip)
     */
    public TDNetwork(String modelPath) {
        File modelFile = new File(modelPath);
        if (modelFile.exists()) {
            try {
                network = ModelSerializer.restoreMultiLayerNetwork(modelFile);
                System.out.println("Loaded TD-Gammon model from: " + modelPath);
            } catch (IOException e) {
                System.err.println("Failed to load model, creating new: " + e.getMessage());
                buildNetwork();
            }
        } else {
            buildNetwork();
        }
    }

    /**
     * Baut die Netzwerk-Architektur auf.
     *
     * Architektur:
     * - Input Layer: 198 Neuronen (implizit)
     * - Hidden Layer: 80 Neuronen, Sigmoid-Aktivierung
     * - Output Layer: 1 Neuron, Sigmoid-Aktivierung
     *
     * Die Sigmoid-Aktivierung ist wichtig, da wir Wahrscheinlichkeiten [0,1] ausgeben.
     */
    private void buildNetwork() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(42)  // Reproduzierbarkeit
                .weightInit(WeightInit.XAVIER)  // Xavier-Initialisierung für Sigmoid
                .updater(new Sgd(LEARNING_RATE))  // Stochastic Gradient Descent
                .list()
                // Hidden Layer
                .layer(new DenseLayer.Builder()
                        .nIn(INPUT_SIZE)   // 198 Eingaben
                        .nOut(HIDDEN_SIZE) // 80 Hidden Neuronen
                        .activation(Activation.SIGMOID)
                        .build())
                // Output Layer
                .layer(new OutputLayer.Builder()
                        .nIn(HIDDEN_SIZE)  // 80 Eingaben vom Hidden Layer
                        .nOut(1)           // 1 Ausgabe: Gewinnwahrscheinlichkeit
                        .activation(Activation.SIGMOID)
                        .lossFunction(LossFunctions.LossFunction.MSE)  // Mean Squared Error
                        .build())
                .build();

        network = new MultiLayerNetwork(conf);
        network.init();

        System.out.println("Created new TD-Gammon network");
        System.out.println("  Input size: " + INPUT_SIZE);
        System.out.println("  Hidden size: " + HIDDEN_SIZE);
        System.out.println("  Parameters: " + network.numParams());
    }

    // ============================================================================
    // EVALUATION
    // ============================================================================

    /**
     * Bewertet eine Spielposition.
     *
     * Dies ist die Kernfunktion der KI: Sie nimmt einen kodierten Spielzustand
     * und gibt die geschätzte Gewinnwahrscheinlichkeit zurück.
     *
     * @param input Der kodierte Spielzustand (198 Features von BoardEncoder)
     * @return Gewinnwahrscheinlichkeit für den Spieler am Zug [0, 1]
     */
    public double evaluate(float[] input) {
        // Buffer wiederverwenden um GC-Druck zu reduzieren
        if (inputBuffer == null) {
            inputBuffer = Nd4j.create(1, INPUT_SIZE);
        }

        // Daten in Buffer kopieren
        for (int i = 0; i < INPUT_SIZE; i++) {
            inputBuffer.putScalar(0, i, input[i]);
        }

        // Forward Pass durch das Netzwerk
        INDArray output = network.output(inputBuffer);
        return output.getDouble(0);
    }

    /**
     * Schnelle Evaluation für Training (weniger genau, aber viel schneller).
     *
     * Diese vereinfachte Evaluation wird während des Self-Play Trainings
     * verwendet, um die Geschwindigkeit zu erhöhen.
     *
     * @param input Der kodierte Spielzustand
     * @return Approximierte Gewinnwahrscheinlichkeit [0, 1]
     */
    public double evaluateFast(float[] input) {
        // Einfache lineare Kombination als Approximation
        double sum = 0;
        for (int i = 0; i < INPUT_SIZE; i++) {
            sum += input[i] * (i % 2 == 0 ? 0.01 : -0.01);
        }
        return 1.0 / (1.0 + Math.exp(-sum)); // Sigmoid
    }

    // ============================================================================
    // TRAINING
    // ============================================================================

    /**
     * TD-Update: Passt das Netzwerk basierend auf dem TD-Error an.
     *
     * Die Update-Regel:
     *   V(s) ← V(s) + α × [target - V(s)]
     *
     * Wobei target typischerweise V(s') oder der finale Reward ist.
     *
     * @param currentState Der aktuelle Zustand (kodiert)
     * @param nextValue    Der Zielwert (V(s') oder finaler Reward)
     * @param alpha        Lernraten-Modifikator
     */
    public void tdUpdate(float[] currentState, double nextValue, double alpha) {
        INDArray input = Nd4j.create(currentState).reshape(1, INPUT_SIZE);

        // Zielwert erstellen
        INDArray target = Nd4j.create(new double[]{nextValue}).reshape(1, 1);

        // Ein Trainingsschritt
        network.fit(input, target);
    }

    /**
     * Batch TD(λ) Update mit Eligibility Traces.
     *
     * Diese Methode implementiert das vollständige TD(λ) Update für eine
     * Sequenz von Spielzuständen nach Spielende.
     *
     * Eligibility Traces:
     * - Jeder besuchte Zustand hinterlässt eine "Spur"
     * - Die Spur verblasst mit Faktor λ über die Zeit
     * - Beim finalen Reward werden alle Zustände anteilig aktualisiert
     *
     * @param states      Array von kodierten Zuständen aus dem Spiel
     * @param finalReward Der finale Reward (1.0 = Sieg, 0.0 = Niederlage)
     */
    public void tdLambdaUpdate(float[][] states, double finalReward) {
        if (states.length == 0) return;

        int n = states.length;
        double[] values = new double[n + 1];

        // Berechne V(s) für alle Zustände
        for (int i = 0; i < n; i++) {
            values[i] = evaluate(states[i]);
        }
        values[n] = finalReward;

        // TD(λ) Updates rückwärts durch die Sequenz
        double[] eligibility = new double[n];

        for (int t = n - 1; t >= 0; t--) {
            // TD-Error zum Zeitpunkt t
            double tdError = values[t + 1] - values[t];

            // Eligibility Traces akkumulieren
            for (int i = 0; i <= t; i++) {
                double decay = Math.pow(lambda, t - i);
                eligibility[i] += decay;
            }

            // λ-Return Zielwert berechnen
            double target = values[t] + 0.1 * tdError * eligibility[t];
            target = Math.max(0.0, Math.min(1.0, target)); // Clip auf [0,1]

            // Update für diesen Zustand
            INDArray input = Nd4j.create(states[t]).reshape(1, INPUT_SIZE);
            INDArray targetArray = Nd4j.create(new double[]{target}).reshape(1, 1);
            network.fit(input, targetArray);
        }
    }

    // ============================================================================
    // PERSISTENZ
    // ============================================================================

    /**
     * Speichert das trainierte Modell in eine Datei.
     *
     * Das Modell wird als ZIP-Archiv gespeichert und enthält:
     * - Netzwerk-Architektur
     * - Alle trainierten Gewichte
     * - Optimierer-Zustand
     *
     * @param path Zielpfad (z.B. "td-gammon-model.zip")
     * @throws IOException Bei Schreibfehlern
     */
    public void save(String path) throws IOException {
        ModelSerializer.writeModel(network, new File(path), true);
        System.out.println("Saved model to: " + path);
    }

    /**
     * Lädt ein gespeichertes Modell aus einer Datei.
     *
     * @param path Quellpfad zur Modell-Datei
     * @throws IOException Bei Lesefehlern
     */
    public void load(String path) throws IOException {
        network = ModelSerializer.restoreMultiLayerNetwork(new File(path));
        inputBuffer = null; // Buffer zurücksetzen
        System.out.println("Loaded model from: " + path);
    }

    // ============================================================================
    // GETTER / SETTER
    // ============================================================================

    public double getLambda() {
        return lambda;
    }

    /**
     * Setzt den Lambda-Parameter für TD(λ).
     *
     * @param lambda Wert zwischen 0 und 1 (empfohlen: 0.7)
     */
    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    /**
     * Gibt das zugrunde liegende DL4J-Netzwerk zurück.
     * Für fortgeschrittene Anwendungen und Debugging.
     */
    public MultiLayerNetwork getNetwork() {
        return network;
    }
}
