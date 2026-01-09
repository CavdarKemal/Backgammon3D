package com.backgammon3d.model;

import java.util.Arrays;

/**
 * Represents the complete state of a Backgammon game.
 *
 * Board layout (from White's perspective):
 * Points 0-5: White's home board
 * Points 6-11: White's outer board
 * Points 12-17: Black's outer board
 * Points 18-23: Black's home board
 *
 * Positive values = White checkers
 * Negative values = Black checkers
 */
public class GameState {

    public static final int NUM_POINTS = 24;
    public static final int CHECKERS_PER_PLAYER = 15;

    // Board representation: positive = white, negative = black
    private int[] points;

    // Bar (hit checkers)
    private int whiteBar;
    private int blackBar;

    // Bear off (removed checkers)
    private int whiteBearOff;
    private int blackBearOff;

    // Current player: true = white, false = black
    private boolean whiteTurn;

    /**
     * Creates a new game with standard starting position.
     */
    public GameState() {
        points = new int[NUM_POINTS];
        initializeStandardPosition();
        whiteTurn = true;
    }

    /**
     * Creates a copy of an existing game state.
     */
    public GameState(GameState other) {
        this.points = Arrays.copyOf(other.points, NUM_POINTS);
        this.whiteBar = other.whiteBar;
        this.blackBar = other.blackBar;
        this.whiteBearOff = other.whiteBearOff;
        this.blackBearOff = other.blackBearOff;
        this.whiteTurn = other.whiteTurn;
    }

    /**
     * Standard Backgammon starting position.
     */
    private void initializeStandardPosition() {
        Arrays.fill(points, 0);

        // White checkers (positive)
        points[0] = 2;    // 2 on point 1 (index 0)
        points[11] = 5;   // 5 on point 12
        points[16] = 3;   // 3 on point 17
        points[18] = 5;   // 5 on point 19

        // Black checkers (negative)
        points[23] = -2;  // 2 on point 24
        points[12] = -5;  // 5 on point 13
        points[7] = -3;   // 3 on point 8
        points[5] = -5;   // 5 on point 6

        whiteBar = 0;
        blackBar = 0;
        whiteBearOff = 0;
        blackBearOff = 0;
    }

    // Getters
    public int[] getPoints() {
        return points;
    }

    public int getPoint(int index) {
        if (index < 0 || index >= NUM_POINTS) return 0;
        return points[index];
    }

    public int getWhiteBar() {
        return whiteBar;
    }

    public int getBlackBar() {
        return blackBar;
    }

    public int getWhiteBearOff() {
        return whiteBearOff;
    }

    public int getBlackBearOff() {
        return blackBearOff;
    }

    public boolean isWhiteTurn() {
        return whiteTurn;
    }

    // Setters
    public void setPoint(int index, int value) {
        if (index >= 0 && index < NUM_POINTS) {
            points[index] = value;
        }
    }

    public void setWhiteBar(int value) {
        this.whiteBar = value;
    }

    public void setBlackBar(int value) {
        this.blackBar = value;
    }

    public void setWhiteBearOff(int value) {
        this.whiteBearOff = value;
    }

    public void setBlackBearOff(int value) {
        this.blackBearOff = value;
    }

    public void setWhiteTurn(boolean whiteTurn) {
        this.whiteTurn = whiteTurn;
    }

    public void switchTurn() {
        this.whiteTurn = !this.whiteTurn;
    }

    /**
     * Returns the number of checkers for a player on a specific point.
     * Always returns a positive number.
     */
    public int getCheckerCount(int point, boolean isWhite) {
        int value = points[point];
        if (isWhite) {
            return value > 0 ? value : 0;
        } else {
            return value < 0 ? -value : 0;
        }
    }

    /**
     * Checks if a point is owned by a player (has at least one checker).
     */
    public boolean isOwnedBy(int point, boolean isWhite) {
        return isWhite ? points[point] > 0 : points[point] < 0;
    }

    /**
     * Checks if a point is blocked (has 2+ opponent checkers).
     */
    public boolean isBlocked(int point, boolean forWhite) {
        if (forWhite) {
            return points[point] <= -2;
        } else {
            return points[point] >= 2;
        }
    }

    /**
     * Checks if a point has a blot (single opponent checker).
     */
    public boolean hasBlot(int point, boolean forWhite) {
        if (forWhite) {
            return points[point] == -1;
        } else {
            return points[point] == 1;
        }
    }

    /**
     * Checks if all checkers are in home board (can bear off).
     */
    public boolean canBearOff(boolean isWhite) {
        if (isWhite) {
            if (whiteBar > 0) return false;
            // All white checkers must be in points 0-5
            for (int i = 6; i < NUM_POINTS; i++) {
                if (points[i] > 0) return false;
            }
            return true;
        } else {
            if (blackBar > 0) return false;
            // All black checkers must be in points 18-23
            for (int i = 0; i < 18; i++) {
                if (points[i] < 0) return false;
            }
            return true;
        }
    }

    /**
     * Checks if the game is over.
     */
    public boolean isGameOver() {
        return whiteBearOff == CHECKERS_PER_PLAYER || blackBearOff == CHECKERS_PER_PLAYER;
    }

    /**
     * Returns the winner (only valid if game is over).
     * @return true for white, false for black
     */
    public boolean getWinner() {
        return whiteBearOff == CHECKERS_PER_PLAYER;
    }

    /**
     * Applies a move to this game state.
     */
    public void applyMove(Move move) {
        boolean isWhite = whiteTurn;

        // Remove checker from source
        if (move.isFromBar()) {
            if (isWhite) {
                whiteBar--;
            } else {
                blackBar--;
            }
        } else {
            if (isWhite) {
                points[move.getFrom()]--;
            } else {
                points[move.getFrom()]++;
            }
        }

        // Handle bear off
        if (move.isBearOff()) {
            if (isWhite) {
                whiteBearOff++;
            } else {
                blackBearOff++;
            }
            return;
        }

        // Check for hit
        if (move.isHit()) {
            if (isWhite) {
                points[move.getTo()] = 0;
                blackBar++;
            } else {
                points[move.getTo()] = 0;
                whiteBar++;
            }
        }

        // Add checker to destination
        if (isWhite) {
            points[move.getTo()]++;
        } else {
            points[move.getTo()]--;
        }
    }

    /**
     * Creates a deep copy of this game state.
     */
    public GameState copy() {
        return new GameState(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Turn: ").append(whiteTurn ? "White" : "Black").append("\n");
        sb.append("Bar - White: ").append(whiteBar).append(", Black: ").append(blackBar).append("\n");
        sb.append("BearOff - White: ").append(whiteBearOff).append(", Black: ").append(blackBearOff).append("\n");
        sb.append("Board:\n");
        for (int i = 0; i < NUM_POINTS; i++) {
            if (points[i] != 0) {
                sb.append("  Point ").append(i + 1).append(": ").append(points[i]).append("\n");
            }
        }
        return sb.toString();
    }
}
