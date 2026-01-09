package com.backgammon3d.model;

/**
 * Validates moves according to Backgammon rules.
 */
public class BackgammonRules {

    /**
     * Checks if a move is valid.
     */
    public static boolean isValidMove(GameState state, Move move, boolean isWhite) {
        // Must have checker on bar first
        if (isWhite && state.getWhiteBar() > 0 && !move.isFromBar()) {
            return false;
        }
        if (!isWhite && state.getBlackBar() > 0 && !move.isFromBar()) {
            return false;
        }

        // Validate source
        if (!move.isFromBar()) {
            if (!state.isOwnedBy(move.getFrom(), isWhite)) {
                return false;
            }
        }

        // Validate destination
        if (move.isBearOff()) {
            return canBearOff(state, move, isWhite);
        } else {
            return canMoveTo(state, move.getTo(), isWhite);
        }
    }

    /**
     * Checks if a checker can move to a specific point.
     */
    public static boolean canMoveTo(GameState state, int point, boolean isWhite) {
        if (point < 0 || point >= GameState.NUM_POINTS) {
            return false;
        }
        // Can't move to a blocked point (2+ opponent checkers)
        return !state.isBlocked(point, isWhite);
    }

    /**
     * Checks if bear off is valid.
     */
    public static boolean canBearOff(GameState state, Move move, boolean isWhite) {
        if (!state.canBearOff(isWhite)) {
            return false;
        }

        int from = move.getFrom();
        int dieValue = move.getDieValue();

        if (isWhite) {
            // White bears off from points 0-5 (home board)
            // Exact roll or higher if no checkers behind
            int distanceToOff = from + 1;

            if (dieValue == distanceToOff) {
                return true;
            }

            if (dieValue > distanceToOff) {
                // Can only bear off with higher die if no checkers on higher points
                for (int i = from + 1; i <= 5; i++) {
                    if (state.getPoint(i) > 0) {
                        return false;
                    }
                }
                return true;
            }

            return false;
        } else {
            // Black bears off from points 18-23 (home board)
            int distanceToOff = 24 - from;

            if (dieValue == distanceToOff) {
                return true;
            }

            if (dieValue > distanceToOff) {
                // Can only bear off with higher die if no checkers on higher points
                for (int i = from - 1; i >= 18; i--) {
                    if (state.getPoint(i) < 0) {
                        return false;
                    }
                }
                return true;
            }

            return false;
        }
    }

    /**
     * Calculates the target point for a move.
     *
     * @param from Source point or -1 for bar
     * @param dieValue Die value
     * @param isWhite True if white player
     * @return Target point (0-23) or -1 for bear off, or -2 if invalid
     */
    public static int calculateTarget(int from, int dieValue, boolean isWhite) {
        int target;

        if (from == Move.BAR) {
            // Enter from bar
            if (isWhite) {
                // White enters on points 23-18 (opponent's home board)
                target = 24 - dieValue;
            } else {
                // Black enters on points 0-5 (opponent's home board)
                target = dieValue - 1;
            }
        } else {
            if (isWhite) {
                // White moves from high to low (towards point 0)
                target = from - dieValue;
            } else {
                // Black moves from low to high (towards point 23)
                target = from + dieValue;
            }
        }

        // Check for bear off
        if (isWhite && target < 0) {
            return Move.BEAR_OFF;
        }
        if (!isWhite && target >= 24) {
            return Move.BEAR_OFF;
        }

        // Invalid target
        if (target < 0 || target >= 24) {
            return -2;
        }

        return target;
    }

    /**
     * Checks if the current player must use the higher die value.
     * (If only one move is possible, must use the higher die)
     */
    public static boolean mustUseHigherDie(GameState state, int[] dice, boolean isWhite) {
        // This is only relevant when the player cannot use both dice
        // and must choose one
        // Implementation simplified - full version would check all possibilities
        return false;
    }

    /**
     * Checks if a hit will occur at the target point.
     */
    public static boolean willHit(GameState state, int target, boolean isWhite) {
        return state.hasBlot(target, isWhite);
    }
}
