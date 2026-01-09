package com.backgammon3d.view;

import com.backgammon3d.model.GameState;
import com.backgammon3d.model.MoveGenerator;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * 3D view of the Backgammon board.
 */
public class BoardView extends SubScene {

    // Board dimensions
    private static final double BOARD_WIDTH = 580;
    private static final double BOARD_HEIGHT = 320;
    private static final double BOARD_DEPTH = 15;
    private static final double BAR_WIDTH = 25;

    // Colors
    private static final Color BOARD_COLOR = Color.rgb(222, 184, 135);    // Burlywood
    private static final Color BOARD_EDGE_COLOR = Color.rgb(139, 90, 43); // Saddle brown
    private static final Color BEAR_OFF_COLOR = Color.rgb(80, 60, 40);

    private final Group root;
    private final Group boardGroup;
    private final PointView[] pointViews;
    private final DiceView[] diceViews;
    private Box barBox;
    private final PerspectiveCamera camera;

    private GameState gameState;
    private int selectedPoint = -1;
    private int[] availableDice;

    // Drag & Drop state
    private CheckerView draggedChecker;
    private int dragSourcePoint = -1;
    private double dragStartX, dragStartY;
    private double dragOffsetX, dragOffsetY, dragOffsetZ;
    private Group dragGroup;

    // Callbacks
    private BiConsumer<Integer, Boolean> onCheckerSelected;
    private BiConsumer<Integer, Integer> onMoveExecuted; // (fromPoint, toPoint)

    public BoardView(double width, double height, GameState gameState) {
        super(new Group(), width, height, true, SceneAntialiasing.BALANCED);

        this.root = (Group) getRoot();
        this.gameState = gameState;
        this.pointViews = new PointView[24];
        this.diceViews = new DiceView[2];
        this.availableDice = new int[0];

        // Set background
        setFill(Color.rgb(40, 40, 40));

        // Create board group
        boardGroup = new Group();
        root.getChildren().add(boardGroup);

        // Setup camera - mostly top-down with slight tilt for 3D effect
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        camera.setFieldOfView(50);
        camera.setTranslateX(0);
        camera.setTranslateY(-600);
        camera.setTranslateZ(0);
        camera.getTransforms().add(new Rotate(-85, Rotate.X_AXIS)); // Almost top-down
        setCamera(camera);

        // Setup lighting
        setupLighting();

        // Build board
        createBoard();
        createPoints();
        createBar();
        createDice();
        createBearOffAreas();

        // Create drag group (for dragged checker)
        dragGroup = new Group();
        root.getChildren().add(dragGroup);

        // Setup mouse interaction
        setupMouseHandlers();

        // Initial update
        updateBoard();
    }

    private void setupLighting() {
        AmbientLight ambient = new AmbientLight(Color.rgb(120, 120, 120));
        root.getChildren().add(ambient);

        PointLight light1 = new PointLight(Color.rgb(255, 255, 255));
        light1.setTranslateX(0);
        light1.setTranslateY(-500);
        light1.setTranslateZ(0);
        root.getChildren().add(light1);

        PointLight light2 = new PointLight(Color.rgb(180, 180, 180));
        light2.setTranslateX(-300);
        light2.setTranslateY(-400);
        light2.setTranslateZ(200);
        root.getChildren().add(light2);
    }

    private void createBoard() {
        // Main board surface
        Box boardSurface = new Box(BOARD_WIDTH, BOARD_DEPTH, BOARD_HEIGHT);
        PhongMaterial boardMaterial = new PhongMaterial();
        boardMaterial.setDiffuseColor(BOARD_COLOR);
        boardMaterial.setSpecularColor(Color.WHITE);
        boardMaterial.setSpecularPower(5);
        boardSurface.setMaterial(boardMaterial);
        boardGroup.getChildren().add(boardSurface);

        // Board edges
        double edgeThickness = 15;
        PhongMaterial edgeMaterial = new PhongMaterial();
        edgeMaterial.setDiffuseColor(BOARD_EDGE_COLOR);

        // Left edge
        Box leftEdge = new Box(edgeThickness, BOARD_DEPTH + 10, BOARD_HEIGHT + edgeThickness * 2);
        leftEdge.setMaterial(edgeMaterial);
        leftEdge.setTranslateX(-BOARD_WIDTH / 2 - edgeThickness / 2);
        boardGroup.getChildren().add(leftEdge);

        // Right edge
        Box rightEdge = new Box(edgeThickness, BOARD_DEPTH + 10, BOARD_HEIGHT + edgeThickness * 2);
        rightEdge.setMaterial(edgeMaterial);
        rightEdge.setTranslateX(BOARD_WIDTH / 2 + edgeThickness / 2);
        boardGroup.getChildren().add(rightEdge);

        // Top edge
        Box topEdge = new Box(BOARD_WIDTH, BOARD_DEPTH + 10, edgeThickness);
        topEdge.setMaterial(edgeMaterial);
        topEdge.setTranslateZ(-BOARD_HEIGHT / 2 - edgeThickness / 2);
        boardGroup.getChildren().add(topEdge);

        // Bottom edge
        Box bottomEdge = new Box(BOARD_WIDTH, BOARD_DEPTH + 10, edgeThickness);
        bottomEdge.setMaterial(edgeMaterial);
        bottomEdge.setTranslateZ(BOARD_HEIGHT / 2 + edgeThickness / 2);
        boardGroup.getChildren().add(bottomEdge);
    }

    private void createPoints() {
        double pointWidth = PointView.getPointWidth();
        double pointLength = PointView.getPointLength();
        double startX = -BOARD_WIDTH / 2 + pointWidth / 2 + 20;
        double gapForBar = BAR_WIDTH + 20;

        // Points 0-11 (bottom row, left to right from white's view)
        // Points 12-23 (top row, right to left from white's view)

        for (int i = 0; i < 24; i++) {
            boolean isTopRow = i >= 12;
            int displayIndex = isTopRow ? (23 - i) : i;

            PointView point = new PointView(i, isTopRow);

            // Calculate X position
            double x;
            if (displayIndex < 6) {
                x = startX + displayIndex * (pointWidth + 5);
            } else {
                x = startX + displayIndex * (pointWidth + 5) + gapForBar;
            }

            // Z position (top or bottom)
            double z = isTopRow ? -BOARD_HEIGHT / 2 + pointLength / 2 + 20
                               : BOARD_HEIGHT / 2 - pointLength / 2 - 20;

            // Y position (on top of board)
            point.setTranslateX(x);
            point.setTranslateY(-BOARD_DEPTH / 2 - 2);
            point.setTranslateZ(z);

            pointViews[i] = point;
            boardGroup.getChildren().add(point);
        }
    }

    private void createBar() {
        barBox = new Box(BAR_WIDTH, BOARD_DEPTH + 5, BOARD_HEIGHT - 40);
        PhongMaterial barMaterial = new PhongMaterial();
        barMaterial.setDiffuseColor(BOARD_EDGE_COLOR);
        barBox.setMaterial(barMaterial);
        barBox.setTranslateY(-2);
        boardGroup.getChildren().add(barBox);
    }

    private void createDice() {
        diceViews[0] = new DiceView(1);
        diceViews[0].setTranslateX(-50);
        diceViews[0].setTranslateY(-BOARD_DEPTH / 2 - DiceView.getDieSize() / 2 - 5);
        diceViews[0].setTranslateZ(0);

        diceViews[1] = new DiceView(1);
        diceViews[1].setTranslateX(50);
        diceViews[1].setTranslateY(-BOARD_DEPTH / 2 - DiceView.getDieSize() / 2 - 5);
        diceViews[1].setTranslateZ(0);

        boardGroup.getChildren().addAll(diceViews[0], diceViews[1]);
    }

    private void createBearOffAreas() {
        // White bear off (right side)
        Box whiteBearOff = new Box(40, BOARD_DEPTH, 80);
        PhongMaterial whiteMaterial = new PhongMaterial();
        whiteMaterial.setDiffuseColor(BEAR_OFF_COLOR);
        whiteBearOff.setMaterial(whiteMaterial);
        whiteBearOff.setTranslateX(BOARD_WIDTH / 2 + 50);
        whiteBearOff.setTranslateZ(BOARD_HEIGHT / 4);
        boardGroup.getChildren().add(whiteBearOff);

        // Black bear off (right side, top)
        Box blackBearOff = new Box(40, BOARD_DEPTH, 80);
        blackBearOff.setMaterial(whiteMaterial);
        blackBearOff.setTranslateX(BOARD_WIDTH / 2 + 50);
        blackBearOff.setTranslateZ(-BOARD_HEIGHT / 4);
        boardGroup.getChildren().add(blackBearOff);
    }

    private void setupMouseHandlers() {
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;

        PickResult pickResult = event.getPickResult();
        Node intersectedNode = pickResult.getIntersectedNode();

        if (intersectedNode == null) {
            clearSelection();
            return;
        }

        // Find which point was clicked and start drag if it has a checker
        for (int i = 0; i < 24; i++) {
            if (isChildOf(intersectedNode, pointViews[i])) {
                int checkerCount = gameState.getPoint(i);
                boolean hasWhiteChecker = checkerCount > 0;
                boolean hasBlackChecker = checkerCount < 0;

                if (hasWhiteChecker || hasBlackChecker) {
                    startDrag(i, hasWhiteChecker, event);
                }
                return;
            }
        }

        clearSelection();
    }

    private void startDrag(int pointIndex, boolean isWhite, MouseEvent event) {
        // Clear any previous selection
        clearSelection();

        dragSourcePoint = pointIndex;
        selectedPoint = pointIndex;

        // Highlight valid targets
        if (onCheckerSelected != null) {
            onCheckerSelected.accept(pointIndex, isWhite);
        }

        // Get the top checker from the point
        CheckerView topChecker = pointViews[pointIndex].getTopChecker();
        if (topChecker == null) return;

        // Create a new checker for dragging
        draggedChecker = new CheckerView(isWhite);
        draggedChecker.setDragging(true);

        // Store initial mouse position
        dragStartX = event.getSceneX();
        dragStartY = event.getSceneY();

        // Calculate initial 3D position from the top checker
        Point3D localPos = topChecker.localToScene(Point3D.ZERO);
        dragOffsetX = pointViews[pointIndex].getTranslateX();
        dragOffsetY = -BOARD_DEPTH / 2 - CheckerView.getCheckerHeight() - 15;
        dragOffsetZ = pointViews[pointIndex].getTranslateZ();

        if (pointViews[pointIndex].isTopRow()) {
            dragOffsetZ += -PointView.getPointLength() / 2 + 20 +
                    (pointViews[pointIndex].getCheckers().size() - 1) * (CheckerView.getCheckerHeight() + 3);
        } else {
            dragOffsetZ += PointView.getPointLength() / 2 - 20 -
                    (pointViews[pointIndex].getCheckers().size() - 1) * (CheckerView.getCheckerHeight() + 3);
        }

        draggedChecker.setTranslateX(dragOffsetX);
        draggedChecker.setTranslateY(dragOffsetY);
        draggedChecker.setTranslateZ(dragOffsetZ);

        dragGroup.getChildren().add(draggedChecker);

        // Highlight the source checker
        topChecker.setSelected(true);

        // Show valid targets
        if (availableDice != null && availableDice.length > 0) {
            highlightValidTargets(pointIndex, availableDice, isWhite);
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (draggedChecker == null) return;

        // Calculate mouse delta
        double deltaX = event.getSceneX() - dragStartX;
        double deltaY = event.getSceneY() - dragStartY;

        // Convert 2D mouse movement to 3D (approximate)
        // Since camera is almost top-down (-85°), X maps to X and Y maps mostly to Z
        double scale = 1.2;
        draggedChecker.setTranslateX(dragOffsetX + deltaX * scale);
        draggedChecker.setTranslateZ(dragOffsetZ - deltaY * scale);
    }

    private void handleMouseReleased(MouseEvent event) {
        if (draggedChecker == null) {
            return;
        }

        // Find target point under the dropped checker
        int targetPoint = findTargetPoint(draggedChecker.getTranslateX(), draggedChecker.getTranslateZ());

        // Save source before clearing
        int sourcePoint = dragSourcePoint;

        // Check if it's a valid target
        if (targetPoint >= 0 && targetPoint < 24 && pointViews[targetPoint].isHighlighted()) {
            // Clean up drag state first
            endDrag();

            // Execute the move
            if (onMoveExecuted != null) {
                onMoveExecuted.accept(sourcePoint, targetPoint);
            }
        } else {
            // Invalid target - just clean up
            endDrag();
        }
    }

    private int findTargetPoint(double x, double z) {
        // Find the point closest to the given 3D coordinates
        double minDist = Double.MAX_VALUE;
        int closestPoint = -1;

        for (int i = 0; i < 24; i++) {
            double px = pointViews[i].getTranslateX();
            double pz = pointViews[i].getTranslateZ();

            double dist = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));

            // Check if within reasonable range of the point
            if (dist < 50 && dist < minDist) {
                minDist = dist;
                closestPoint = i;
            }
        }

        return closestPoint;
    }

    private void endDrag() {
        // Remove dragged checker
        dragGroup.getChildren().clear();
        draggedChecker = null;
        dragSourcePoint = -1;

        // Clear selection and highlights
        clearSelection();
    }

    private boolean isChildOf(Node node, Node parent) {
        Node current = node;
        while (current != null) {
            if (current == parent) return true;
            current = current.getParent();
        }
        return false;
    }

    public void clearSelection() {
        if (selectedPoint >= 0) {
            pointViews[selectedPoint].setCheckersSelected(false);
        }
        selectedPoint = -1;
        clearHighlights();
    }

    private void clearHighlights() {
        for (PointView point : pointViews) {
            point.setHighlighted(false);
        }
    }

    /**
     * Updates the board display from the current game state.
     */
    public void updateBoard() {
        int[] points = gameState.getPoints();

        for (int i = 0; i < 24; i++) {
            int value = points[i];
            if (value > 0) {
                pointViews[i].setCheckers(value, true);
            } else if (value < 0) {
                pointViews[i].setCheckers(-value, false);
            } else {
                pointViews[i].clearCheckers();
            }
        }
    }

    /**
     * Sets the game state and updates the display.
     */
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        clearSelection();
        updateBoard();
    }

    /**
     * Highlights valid target points for a selected checker.
     */
    public void highlightValidTargets(int fromPoint, int[] dice, boolean isWhite) {
        clearHighlights();

        if (dice == null || dice.length == 0) return;

        List<Integer> targets = MoveGenerator.getValidTargets(gameState, fromPoint, dice, isWhite);

        for (int target : targets) {
            if (target >= 0 && target < 24) {
                pointViews[target].setHighlighted(true);
            }
            // TODO: Handle bear off highlighting
        }
    }

    /**
     * Sets the available dice for move calculation.
     */
    public void setAvailableMoves(int[] dice) {
        this.availableDice = dice;
        if (dice.length >= 2) {
            diceViews[0].showValue(dice[0]);
            diceViews[1].showValue(dice[1]);
        }
    }

    /**
     * Executes a move animation (placeholder).
     */
    public void executeMove(int targetPoint) {
        // TODO: Animate checker movement
        updateBoard();
    }

    // Callback setters
    public void setOnCheckerSelected(BiConsumer<Integer, Boolean> callback) {
        this.onCheckerSelected = callback;
    }

    public void setOnMoveExecuted(BiConsumer<Integer, Integer> callback) {
        this.onMoveExecuted = callback;
    }
}
