package com.backgammon3d.view;

import javafx.animation.ScaleTransition;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.util.Duration;

/**
 * 3D representation of a single Backgammon checker (game piece).
 */
public class CheckerView extends Group {

    private static final double RADIUS = 18;
    private static final double HEIGHT = 8;

    private static final Color WHITE_COLOR = Color.WHITE;
    private static final Color WHITE_HIGHLIGHT = Color.rgb(220, 220, 255);
    private static final Color BLACK_COLOR = Color.rgb(30, 30, 30);
    private static final Color BLACK_HIGHLIGHT = Color.rgb(60, 60, 60);
    private static final Color SELECTED_COLOR = Color.CYAN;
    private static final Color DRAGGING_COLOR = Color.GOLD;

    private final Cylinder cylinder;
    private final PhongMaterial material;
    private final boolean isWhite;
    private boolean isSelected;
    private boolean isHighlighted;
    private boolean isDragging;
    private boolean isDraggable;

    public CheckerView(boolean isWhite) {
        this.isWhite = isWhite;
        this.isSelected = false;
        this.isHighlighted = false;
        this.isDragging = false;
        this.isDraggable = false;

        // Create checker cylinder
        cylinder = new Cylinder(RADIUS, HEIGHT);

        // Create material
        material = new PhongMaterial();
        material.setDiffuseColor(isWhite ? WHITE_COLOR : BLACK_COLOR);
        material.setSpecularColor(Color.WHITE);
        material.setSpecularPower(30);
        cylinder.setMaterial(material);

        // Rotate to lay flat
        cylinder.setRotate(90);
        cylinder.setRotationAxis(javafx.geometry.Point3D.ZERO.add(1, 0, 0));

        getChildren().add(cylinder);
    }

    public boolean isWhiteChecker() {
        return isWhite;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        updateAppearance();

        if (selected) {
            playSelectionAnimation();
        }
    }

    public void setHighlighted(boolean highlighted) {
        this.isHighlighted = highlighted;
        updateAppearance();
    }

    private void updateAppearance() {
        Color color;
        if (isDragging) {
            color = DRAGGING_COLOR;
        } else if (isSelected) {
            color = SELECTED_COLOR;
        } else if (isHighlighted) {
            color = isWhite ? WHITE_HIGHLIGHT : BLACK_HIGHLIGHT;
        } else {
            color = isWhite ? WHITE_COLOR : BLACK_COLOR;
        }
        material.setDiffuseColor(color);
    }

    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
        updateAppearance();
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void setDraggable(boolean draggable) {
        this.isDraggable = draggable;
    }

    public boolean isDraggable() {
        return isDraggable;
    }

    private void playSelectionAnimation() {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), this);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setFromZ(1.0);
        st.setToX(1.1);
        st.setToY(1.1);
        st.setToZ(1.1);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }

    public static double getCheckerRadius() {
        return RADIUS;
    }

    public static double getCheckerHeight() {
        return HEIGHT;
    }
}
