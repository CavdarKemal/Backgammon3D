package com.backgammon3d.model;

import java.util.Random;

/**
 * Dice utilities for Backgammon.
 */
public class Dice {

    private static final Random random = new Random();

    /**
     * Roll two dice.
     * @return Array of two dice values [1-6, 1-6]
     */
    public static int[] roll() {
        return new int[] {
            random.nextInt(6) + 1,
            random.nextInt(6) + 1
        };
    }

    /**
     * Roll two dice with a specific seed (for testing).
     */
    public static int[] roll(long seed) {
        Random seeded = new Random(seed);
        return new int[] {
            seeded.nextInt(6) + 1,
            seeded.nextInt(6) + 1
        };
    }

    /**
     * Get the available moves from a dice roll.
     * For doubles (Pasch), returns 4 identical values.
     *
     * @param d1 First die value
     * @param d2 Second die value
     * @return Array of move values (2 or 4 elements)
     */
    public static int[] getMovesFromRoll(int d1, int d2) {
        if (d1 == d2) {
            // Doubles: 4 moves
            return new int[] { d1, d1, d1, d1 };
        } else {
            return new int[] { d1, d2 };
        }
    }

    /**
     * Check if the roll is doubles.
     */
    public static boolean isDoubles(int d1, int d2) {
        return d1 == d2;
    }

    /**
     * Check if the roll is doubles.
     */
    public static boolean isDoubles(int[] dice) {
        return dice.length >= 2 && dice[0] == dice[1];
    }

    /**
     * Get the sum of two dice.
     */
    public static int getSum(int d1, int d2) {
        return d1 + d2;
    }

    /**
     * Get a specific roll (for testing).
     */
    public static int[] specific(int d1, int d2) {
        return new int[] { d1, d2 };
    }
}
