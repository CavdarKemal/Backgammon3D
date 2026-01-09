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

    // Weiß = Cyan/Türkis für bessere Sichtbarkeit
    private static final Color WHITE_COLOR = Color.rgb(0, 200, 220);        // Cyan/Türkis
    private static final Color WHITE_HIGHLIGHT = Color.rgb(100, 230, 240);  // Heller Cyan
    private static final Color BLACK_COLOR = Color.rgb(60, 30, 30);         // Dunkles Rot-Braun
    private static final Color BLACK_HIGHLIGHT = Color.rgb(100, 50, 50);    // Heller Rot-Braun
    private static final Color SELECTED_COLOR = Color.GOLD;                 // Gold für Auswahl
    private static final Color DRAGGING_COLOR = Color.ORANGE;               // Orange beim Ziehen
    private static final Color AI_MOVE_COLOR = Color.MAGENTA;               // Magenta für KI-Züge

    private final Cylinder cylinder;
    private final PhongMaterial material;
    private final boolean isWhite;
    private boolean isSelected;
    private boolean isHighlighted;
    private boolean isDragging;
    private boolean isDraggable;
    private boolean isAiMove;

    public CheckerView(boolean isWhite) {
        this.isWhite = isWhite;
        this.isSelected = false;
        this.isHighlighted = false;
        this.isDragging = false;
        this.isDraggable = false;
        this.isAiMove = false;

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
        } else if (isAiMove) {
            color = AI_MOVE_COLOR;  // Magenta für KI-Züge - gut sichtbar!
        } else if (isSelected) {
            color = SELECTED_COLOR;
        } else if (isHighlighted) {
            color = isWhite ? WHITE_HIGHLIGHT : BLACK_HIGHLIGHT;
        } else {
            color = isWhite ? WHITE_COLOR : BLACK_COLOR;
        }
        material.setDiffuseColor(color);
    }

    /**
     * Markiert diesen Stein als KI-Zug (Magenta Highlight).
     * Wird nach kurzer Zeit automatisch zurückgesetzt.
     */
    public void setAiMove(boolean aiMove) {
        this.isAiMove = aiMove;
        updateAppearance();
    }

    public boolean isAiMove() {
        return isAiMove;
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
