package com.backgammon3d;

/**
 * Launcher class to work around JavaFX runtime issues.
 * This class does not extend Application, avoiding module system problems.
 */
public class Launcher {

    public static void main(String[] args) {
        Main.main(args);
    }
}
