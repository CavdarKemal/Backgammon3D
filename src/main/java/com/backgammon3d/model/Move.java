package com.backgammon3d.model;

/**
 * Represents a single move in Backgammon.
 *
 * Special values:
 * - from = -1: Moving from the bar
 * - to = -1: Bearing off
 */
public class Move {

    public static final int BAR = -1;
    public static final int BEAR_OFF = -1;

    private final int from;
    private final int to;
    private final int dieValue;
    private final boolean isHit;

    /**
     * Creates a move.
     *
     * @param from Source point (0-23) or BAR (-1)
     * @param to Target point (0-23) or BEAR_OFF (-1)
     * @param dieValue The die value used for this move
     * @param isHit True if this move hits an opponent's blot
     */
    public Move(int from, int to, int dieValue, boolean isHit) {
        this.from = from;
        this.to = to;
        this.dieValue = dieValue;
        this.isHit = isHit;
    }

    /**
     * Creates a simple move without hit.
     */
    public Move(int from, int to, int dieValue) {
        this(from, to, dieValue, false);
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public int getDieValue() {
        return dieValue;
    }

    public boolean isHit() {
        return isHit;
    }

    public boolean isFromBar() {
        return from == BAR;
    }

    public boolean isBearOff() {
        return to == BEAR_OFF;
    }

    /**
     * Creates a bar entry move.
     */
    public static Move fromBar(int to, int dieValue, boolean isHit) {
        return new Move(BAR, to, dieValue, isHit);
    }

    /**
     * Creates a bear off move.
     */
    public static Move bearOff(int from, int dieValue) {
        return new Move(from, BEAR_OFF, dieValue, false);
    }

    @Override
    public String toString() {
        String fromStr = from == BAR ? "Bar" : String.valueOf(from + 1);
        String toStr = to == BEAR_OFF ? "Off" : String.valueOf(to + 1);
        String hitStr = isHit ? "*" : "";
        return fromStr + "/" + toStr + hitStr + " (" + dieValue + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Move move = (Move) obj;
        return from == move.from && to == move.to && dieValue == move.dieValue;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * from + to) + dieValue;
    }
}
