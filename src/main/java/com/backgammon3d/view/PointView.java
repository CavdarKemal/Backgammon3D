package com.backgammon3d.view;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

/**
 * 3D representation of a single point (triangle) on the Backgammon board.
 * Uses stacked boxes to create a triangle-like shape.
 */
public class PointView extends Group {

    private static final double POINT_WIDTH = 36;
    private static final double POINT_LENGTH = 120;
    private static final double POINT_HEIGHT = 3;
    private static final int TRIANGLE_SEGMENTS = 8;

    // Besserer Kontrast: Creme und Dunkelbraun
    private static final Color LIGHT_POINT_COLOR = Color.rgb(245, 222, 179);  // Helles Beige/Creme
    private static final Color DARK_POINT_COLOR = Color.rgb(139, 69, 19);     // Sattelbraun
    private static final Color HIGHLIGHT_COLOR = Color.LIMEGREEN;
    private static final Color AI_TARGET_COLOR = Color.rgb(255, 100, 255);    // Magenta für KI-Zielfeld

    private final int pointIndex;
    private final boolean isTopRow;
    private final boolean isDarkPoint;
    private final List<CheckerView> checkers;
    private final Group checkerGroup;
    private final Group triangleGroup;
    private final List<Box> triangleBoxes;
    private PhongMaterial material;
    private boolean isHighlighted;

    public PointView(int pointIndex, boolean isTopRow) {
        this.pointIndex = pointIndex;
        this.isTopRow = isTopRow;
        this.isDarkPoint = (pointIndex % 2 == 0);
        this.checkers = new ArrayList<>();
        this.isHighlighted = false;
        this.triangleBoxes = new ArrayList<>();

        // Create material
        material = new PhongMaterial();
        material.setDiffuseColor(isDarkPoint ? DARK_POINT_COLOR : LIGHT_POINT_COLOR);
        material.setSpecularColor(Color.WHITE);
        material.setSpecularPower(15);

        // Create triangle from segments
        triangleGroup = new Group();
        createTriangleFromBoxes();
        getChildren().add(triangleGroup);

        // Group for checkers
        checkerGroup = new Group();
        getChildren().add(checkerGroup);
    }

    /**
     * Creates a triangle shape using multiple box segments that taper toward the tip.
     */
    private void createTriangleFromBoxes() {
        double segmentLength = POINT_LENGTH / TRIANGLE_SEGMENTS;

        for (int i = 0; i < TRIANGLE_SEGMENTS; i++) {
            // Width tapers from full at base to narrow at tip
            double widthFactor = 1.0 - (double) i / TRIANGLE_SEGMENTS;
            double segmentWidth = POINT_WIDTH * widthFactor;

            if (segmentWidth < 3) segmentWidth = 3; // Minimum width

            Box segment = new Box(segmentWidth, POINT_HEIGHT, segmentLength);
            segment.setMaterial(material);

            // Position along the length
            double zPos;
            if (isTopRow) {
                zPos = -POINT_LENGTH / 2 + segmentLength / 2 + i * segmentLength;
            } else {
                zPos = POINT_LENGTH / 2 - segmentLength / 2 - i * segmentLength;
            }

            segment.setTranslateZ(zPos);
            segment.setTranslateY(-POINT_HEIGHT / 2);

            triangleBoxes.add(segment);
            triangleGroup.getChildren().add(segment);
        }
    }

    /**
     * Updates the checkers displayed on this point.
     */
    public void setCheckers(int count, boolean isWhite) {
        checkerGroup.getChildren().clear();
        checkers.clear();

        double checkerHeight = CheckerView.getCheckerHeight();
        double startZ = isTopRow ? -POINT_LENGTH / 2 + 20 : POINT_LENGTH / 2 - 20;
        double direction = isTopRow ? 1 : -1;

        int displayCount = Math.min(count, 5);

        for (int i = 0; i < displayCount; i++) {
            CheckerView checker = new CheckerView(isWhite);

            // Stack checkers along the point toward the tip
            double zOffset = startZ + direction * i * (checkerHeight + 3);
            checker.setTranslateZ(zOffset);
            checker.setTranslateY(-POINT_HEIGHT - checkerHeight / 2 - 2);

            checkers.add(checker);
            checkerGroup.getChildren().add(checker);
        }

        // If more than 5, stack them higher
        if (count > 5) {
            for (int i = 5; i < count; i++) {
                CheckerView checker = new CheckerView(isWhite);
                int baseIndex = i % 5;
                int layer = i / 5;
                double zOffset = startZ + direction * baseIndex * (checkerHeight + 3);
                checker.setTranslateZ(zOffset);
                checker.setTranslateY(-POINT_HEIGHT - checkerHeight / 2 - 2 - layer * (checkerHeight + 1));
                checkers.add(checker);
                checkerGroup.getChildren().add(checker);
            }
        }
    }

    /**
     * Clears all checkers from this point.
     */
    public void clearCheckers() {
        checkerGroup.getChildren().clear();
        checkers.clear();
    }

    /**
     * Highlights this point as a valid move target.
     */
    public void setHighlighted(boolean highlighted) {
        this.isHighlighted = highlighted;
        Color color = highlighted ? HIGHLIGHT_COLOR : (isDarkPoint ? DARK_POINT_COLOR : LIGHT_POINT_COLOR);
        material.setDiffuseColor(color);
    }

    /**
     * Markiert dieses Feld als KI-Zug-Ziel (Magenta).
     */
    public void setAiTargetHighlight(boolean highlighted) {
        Color color = highlighted ? AI_TARGET_COLOR : (isDarkPoint ? DARK_POINT_COLOR : LIGHT_POINT_COLOR);
        material.setDiffuseColor(color);
    }

    /**
     * Markiert den obersten Stein als KI-Zug.
     */
    public void setTopCheckerAiMove(boolean aiMove) {
        CheckerView top = getTopChecker();
        if (top != null) {
            top.setAiMove(aiMove);
        }
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public int getPointIndex() {
        return pointIndex;
    }

    public boolean isTopRow() {
        return isTopRow;
    }

    public List<CheckerView> getCheckers() {
        return checkers;
    }

    public void setCheckersSelected(boolean selected) {
        for (CheckerView checker : checkers) {
            checker.setSelected(selected);
        }
    }

    public CheckerView getTopChecker() {
        if (checkers.isEmpty()) {
            return null;
        }
        return checkers.get(checkers.size() - 1);
    }

    public static double getPointWidth() {
        return POINT_WIDTH;
    }

    public static double getPointLength() {
        return POINT_LENGTH;
    }
}
