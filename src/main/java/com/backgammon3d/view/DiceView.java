package com.backgammon3d.view;

import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.Random;

/**
 * 3D representation of a single die with pips (dots).
 */
public class DiceView extends Group {

    private static final double SIZE = 30;
    private static final double PIP_RADIUS = 3;
    private static final double PIP_OFFSET = SIZE / 4;

    private static final Color DIE_COLOR = Color.IVORY;
    private static final Color PIP_COLOR = Color.rgb(30, 30, 30);

    private final Box dieBox;
    private final Group pipsGroup;
    private int currentValue;
    private final Random random = new Random();

    public DiceView() {
        this(1);
    }

    public DiceView(int initialValue) {
        this.currentValue = initialValue;

        // Create die cube
        dieBox = new Box(SIZE, SIZE, SIZE);
        PhongMaterial dieMaterial = new PhongMaterial();
        dieMaterial.setDiffuseColor(DIE_COLOR);
        dieMaterial.setSpecularColor(Color.WHITE);
        dieMaterial.setSpecularPower(30);
        dieBox.setMaterial(dieMaterial);

        getChildren().add(dieBox);

        // Pips group
        pipsGroup = new Group();
        getChildren().add(pipsGroup);

        showValue(initialValue);
    }

    /**
     * Shows a specific value (1-6) on the die.
     */
    public void showValue(int value) {
        this.currentValue = value;
        pipsGroup.getChildren().clear();

        double halfSize = SIZE / 2 + 0.5; // Slightly outside the cube

        // Create pips based on value
        switch (value) {
            case 1:
                addPip(0, -halfSize, 0); // Center top
                break;
            case 2:
                addPip(-PIP_OFFSET, -halfSize, -PIP_OFFSET);
                addPip(PIP_OFFSET, -halfSize, PIP_OFFSET);
                break;
            case 3:
                addPip(0, -halfSize, 0);
                addPip(-PIP_OFFSET, -halfSize, -PIP_OFFSET);
                addPip(PIP_OFFSET, -halfSize, PIP_OFFSET);
                break;
            case 4:
                addPip(-PIP_OFFSET, -halfSize, -PIP_OFFSET);
                addPip(PIP_OFFSET, -halfSize, -PIP_OFFSET);
                addPip(-PIP_OFFSET, -halfSize, PIP_OFFSET);
                addPip(PIP_OFFSET, -halfSize, PIP_OFFSET);
                break;
            case 5:
                addPip(0, -halfSize, 0);
                addPip(-PIP_OFFSET, -halfSize, -PIP_OFFSET);
                addPip(PIP_OFFSET, -halfSize, -PIP_OFFSET);
                addPip(-PIP_OFFSET, -halfSize, PIP_OFFSET);
                addPip(PIP_OFFSET, -halfSize, PIP_OFFSET);
                break;
            case 6:
                addPip(-PIP_OFFSET, -halfSize, -PIP_OFFSET);
                addPip(PIP_OFFSET, -halfSize, -PIP_OFFSET);
                addPip(-PIP_OFFSET, -halfSize, 0);
                addPip(PIP_OFFSET, -halfSize, 0);
                addPip(-PIP_OFFSET, -halfSize, PIP_OFFSET);
                addPip(PIP_OFFSET, -halfSize, PIP_OFFSET);
                break;
        }
    }

    private void addPip(double x, double y, double z) {
        Sphere pip = new Sphere(PIP_RADIUS);
        PhongMaterial pipMaterial = new PhongMaterial();
        pipMaterial.setDiffuseColor(PIP_COLOR);
        pip.setMaterial(pipMaterial);
        pip.setTranslateX(x);
        pip.setTranslateY(y);
        pip.setTranslateZ(z);
        pipsGroup.getChildren().add(pip);
    }

    /**
     * Animates a dice roll and shows the final value.
     */
    public void animateRoll(int finalValue, Runnable onComplete) {
        // Create rotation animations
        RotateTransition rx = new RotateTransition(Duration.millis(100), this);
        rx.setAxis(new Point3D(1, 0, 0));
        rx.setByAngle(360);
        rx.setCycleCount(3);

        RotateTransition ry = new RotateTransition(Duration.millis(120), this);
        ry.setAxis(new Point3D(0, 1, 0));
        ry.setByAngle(360);
        ry.setCycleCount(2);

        SequentialTransition animation = new SequentialTransition(rx, ry);
        animation.setOnFinished(e -> {
            // Reset rotation
            getTransforms().clear();
            showValue(finalValue);
            if (onComplete != null) {
                onComplete.run();
            }
        });

        // Show random values during animation
        animation.play();
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public static double getDieSize() {
        return SIZE;
    }
}
