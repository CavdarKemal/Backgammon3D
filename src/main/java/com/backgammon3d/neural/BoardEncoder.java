package com.backgammon3d.neural;

import com.backgammon3d.model.GameState;

/**
 * BoardEncoder - Konvertiert Backgammon-Spielzustände in neuronale Netzwerk-Eingaben.
 *
 * Diese Klasse implementiert Tesauros berühmtes 198-Feature-Encoding aus dem
 * TD-Gammon Paper (1995). Dieses Encoding hat sich als sehr effektiv für
 * Backgammon-KI erwiesen und wird auch heute noch verwendet.
 *
 * ============================================================================
 * ENCODING-SCHEMA (198 Features total):
 * ============================================================================
 *
 * 1. SPIELER-STEINE (96 Features):
 *    - 24 Punkte × 4 Units pro Punkt = 96 Features
 *    - Die 4 Units kodieren die Anzahl der Steine:
 *      * 0 Steine: (0, 0, 0, 0)
 *      * 1 Stein:  (1, 0, 0, 0)
 *      * 2 Steine: (1, 1, 0, 0)
 *      * 3 Steine: (1, 1, 1, 0)
 *      * n Steine (n ≥ 3): (1, 1, 1, (n-3)/2)
 *
 *    Dieses Schema ermöglicht dem Netzwerk:
 *    - Blots (einzelne Steine) zu erkennen → wichtig für Schlagen
 *    - Blockaden (2+ Steine) zu erkennen → wichtig für Strategie
 *    - Große Stapel zu bewerten → (n-3)/2 gibt proportionale Info
 *
 * 2. GEGNER-STEINE (96 Features):
 *    - Identisches Schema wie Spieler-Steine
 *    - Aus Sicht des aktuellen Spielers gespiegelt
 *
 * 3. BAR (2 Features):
 *    - 1 Feature für Spieler-Steine auf der Bar (normalisiert durch /2)
 *    - 1 Feature für Gegner-Steine auf der Bar
 *    - Steine auf der Bar müssen zuerst wieder eingesetzt werden
 *
 * 4. BEAR-OFF (2 Features):
 *    - 1 Feature für ausgewürfelte Spieler-Steine (normalisiert durch /15)
 *    - 1 Feature für ausgewürfelte Gegner-Steine
 *    - Ziel: Alle 15 Steine auswürfeln
 *
 * 5. SPIELER AM ZUG (2 Features):
 *    - One-Hot-Encoding: (1,0) wenn Spieler dran, (0,1) wenn Gegner dran
 *    - Wichtig für die Bewertung der Position
 *
 * ============================================================================
 * WARUM DIESES ENCODING?
 * ============================================================================
 *
 * - Translationsinvarianz: Das Netzwerk lernt Muster unabhängig von der
 *   absoluten Position auf dem Brett
 * - Informationsreich: Alle strategisch relevanten Informationen sind enthalten
 * - Kompakt: Nur 198 Werte statt 24×30 = 720 für naive Darstellung
 * - Normalisiert: Alle Werte im Bereich [0,1] für stabiles Training
 *
 * Referenz: Tesauro, G. (1995). "Temporal Difference Learning and TD-Gammon"
 *
 * @author Backgammon3D Project
 * @see TDNetwork
 * @see TDPlayer
 */
public class BoardEncoder {

    /**
     * Größe des Eingabevektors für das neuronale Netzwerk.
     * 198 = 96 (Spieler) + 96 (Gegner) + 2 (Bar) + 2 (BearOff) + 2 (Spieler am Zug)
     */
    public static final int INPUT_SIZE = 198;

    /**
     * Kodiert einen Spielzustand aus der Perspektive eines Spielers.
     *
     * Die Perspektive ist wichtig: Das Netzwerk lernt immer aus Sicht des
     * "aktuellen Spielers". Für Weiß werden die Punkte 0-23 normal gelesen,
     * für Schwarz werden sie gespiegelt (23-0).
     *
     * @param state Der zu kodierende Spielzustand
     * @param asWhite true = Kodierung aus Weiß-Perspektive, false = aus Schwarz-Perspektive
     * @return float-Array der Größe 198 mit Werten im Bereich [0,1]
     */
    public static float[] encode(GameState state, boolean asWhite) {
        float[] input = new float[INPUT_SIZE];
        int idx = 0;

        // ===== TEIL 1: Eigene Steine (96 Features) =====
        // Iteriere über alle 24 Punkte aus Spieler-Perspektive
        for (int i = 0; i < 24; i++) {
            // Punkt-Index abhängig von der Perspektive
            int point = asWhite ? i : (23 - i);
            int count = state.getPoint(point);

            // Positive Werte = Weiß, Negative = Schwarz
            // Wir wollen nur die eigenen Steine zählen
            int playerCount = asWhite ? Math.max(0, count) : Math.max(0, -count);

            // Kodiere mit 4-Unit-Schema
            idx = encodeCheckerCount(input, idx, playerCount);
        }

        // ===== TEIL 2: Gegner-Steine (96 Features) =====
        for (int i = 0; i < 24; i++) {
            int point = asWhite ? i : (23 - i);
            int count = state.getPoint(point);

            // Gegner-Steine haben das umgekehrte Vorzeichen
            int opponentCount = asWhite ? Math.max(0, -count) : Math.max(0, count);

            idx = encodeCheckerCount(input, idx, opponentCount);
        }

        // ===== TEIL 3: Bar (2 Features) =====
        // Steine auf der Bar normalisiert (max ~7-8 realistisch, /2 für Skalierung)
        int playerBar = asWhite ? state.getWhiteBar() : state.getBlackBar();
        input[idx++] = playerBar / 2.0f;

        int opponentBar = asWhite ? state.getBlackBar() : state.getWhiteBar();
        input[idx++] = opponentBar / 2.0f;

        // ===== TEIL 4: Bear-Off (2 Features) =====
        // Ausgewürfelte Steine normalisiert (max 15, /15 für [0,1])
        int playerOff = asWhite ? state.getWhiteBearOff() : state.getBlackBearOff();
        input[idx++] = playerOff / 15.0f;

        int opponentOff = asWhite ? state.getBlackBearOff() : state.getWhiteBearOff();
        input[idx++] = opponentOff / 15.0f;

        // ===== TEIL 5: Spieler am Zug (2 Features) =====
        // One-Hot-Encoding für den aktuellen Spieler
        boolean isPlayerTurn = asWhite ? state.isWhiteTurn() : !state.isWhiteTurn();
        input[idx++] = isPlayerTurn ? 1.0f : 0.0f;
        input[idx++] = isPlayerTurn ? 0.0f : 1.0f;

        return input;
    }

    /**
     * Kodiert die Steinanzahl mit Tesauros 4-Unit-Schema.
     *
     * Schema:
     * - Unit 0: 1 wenn mindestens 1 Stein
     * - Unit 1: 1 wenn mindestens 2 Steine
     * - Unit 2: 1 wenn mindestens 3 Steine
     * - Unit 3: (Anzahl - 3) / 2 wenn mehr als 3 Steine
     *
     * Beispiele:
     * - 0 Steine → (0, 0, 0, 0)
     * - 1 Stein  → (1, 0, 0, 0) ← Blot! Kann geschlagen werden
     * - 2 Steine → (1, 1, 0, 0) ← Blockade
     * - 5 Steine → (1, 1, 1, 1) ← Starke Position
     *
     * @param input Das Ziel-Array
     * @param startIdx Startindex im Array
     * @param count Anzahl der Steine (≥ 0)
     * @return Neuer Index (startIdx + 4)
     */
    private static int encodeCheckerCount(float[] input, int startIdx, int count) {
        if (count >= 1) input[startIdx] = 1.0f;     // Mindestens 1 Stein
        if (count >= 2) input[startIdx + 1] = 1.0f; // Mindestens 2 Steine
        if (count >= 3) input[startIdx + 2] = 1.0f; // Mindestens 3 Steine
        if (count > 3) input[startIdx + 3] = (count - 3) / 2.0f; // Überschuss

        return startIdx + 4;
    }

    /**
     * Convenience-Methode: Kodiert aus Sicht des aktuellen Spielers.
     *
     * @param state Der Spielzustand
     * @return Kodierung aus Sicht des Spielers, der am Zug ist
     */
    public static float[] encode(GameState state) {
        return encode(state, state.isWhiteTurn());
    }
}
