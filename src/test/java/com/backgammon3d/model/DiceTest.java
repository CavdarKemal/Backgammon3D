package com.backgammon3d.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Dice class.
 */
@DisplayName("Dice Tests")
class DiceTest {

    @Nested
    @DisplayName("Roll Tests")
    class RollTests {

        @RepeatedTest(100)
        @DisplayName("Roll produces values 1-6")
        void rollProducesValidValues() {
            int[] roll = Dice.roll();

            assertEquals(2, roll.length, "Roll should produce 2 dice");
            assertTrue(roll[0] >= 1 && roll[0] <= 6, "Die 1 should be 1-6");
            assertTrue(roll[1] >= 1 && roll[1] <= 6, "Die 2 should be 1-6");
        }

        @Test
        @DisplayName("Specific roll returns exact values")
        void specificRoll() {
            int[] roll = Dice.specific(3, 5);

            assertEquals(3, roll[0]);
            assertEquals(5, roll[1]);
        }
    }

    @Nested
    @DisplayName("Doubles Tests")
    class DoublesTests {

        @Test
        @DisplayName("isDoubles returns true for doubles")
        void isDoublesTrue() {
            assertTrue(Dice.isDoubles(3, 3));
            assertTrue(Dice.isDoubles(6, 6));
        }

        @Test
        @DisplayName("isDoubles returns false for non-doubles")
        void isDoublesFalse() {
            assertFalse(Dice.isDoubles(3, 5));
            assertFalse(Dice.isDoubles(1, 6));
        }

        @Test
        @DisplayName("isDoubles with array works")
        void isDoublesArray() {
            assertTrue(Dice.isDoubles(new int[]{4, 4}));
            assertFalse(Dice.isDoubles(new int[]{4, 5}));
        }
    }

    @Nested
    @DisplayName("getMovesFromRoll Tests")
    class GetMovesFromRollTests {

        @Test
        @DisplayName("Non-doubles returns 2 moves")
        void nonDoublesReturnsTwoMoves() {
            int[] moves = Dice.getMovesFromRoll(3, 5);

            assertEquals(2, moves.length);
            assertEquals(3, moves[0]);
            assertEquals(5, moves[1]);
        }

        @Test
        @DisplayName("Doubles returns 4 moves")
        void doublesReturnsFourMoves() {
            int[] moves = Dice.getMovesFromRoll(4, 4);

            assertEquals(4, moves.length);
            assertEquals(4, moves[0]);
            assertEquals(4, moves[1]);
            assertEquals(4, moves[2]);
            assertEquals(4, moves[3]);
        }

        @Test
        @DisplayName("All dice values return correct moves for doubles")
        void allDoublesValues() {
            for (int d = 1; d <= 6; d++) {
                int[] moves = Dice.getMovesFromRoll(d, d);
                assertEquals(4, moves.length, "Doubles of " + d + " should give 4 moves");
                for (int move : moves) {
                    assertEquals(d, move, "Each move should be " + d);
                }
            }
        }
    }

    @Nested
    @DisplayName("Sum Tests")
    class SumTests {

        @Test
        @DisplayName("getSum returns correct sum")
        void getSum() {
            assertEquals(7, Dice.getSum(3, 4));
            assertEquals(12, Dice.getSum(6, 6));
            assertEquals(2, Dice.getSum(1, 1));
        }
    }
}
