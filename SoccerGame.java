package application;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class SoccerGame extends Application {
    private static final double SCENE_WIDTH = 800;
    private static final double SCENE_HEIGHT = 600;
    private static final double PLAYER_WIDTH = SCENE_WIDTH * 0.1;
    private static final double PLAYER_HEIGHT = SCENE_HEIGHT * 0.1;
    private static final double BALL_WIDTH = SCENE_WIDTH * 0.03;
    private static final double BALL_HEIGHT = SCENE_HEIGHT * 0.03;
    private static final double GOAL_WIDTH = SCENE_WIDTH * 0.0625; // 50
    private static final double GOAL_HEIGHT = SCENE_HEIGHT * 0.1667; // 100

    private double playerX = 100;
    private double playerY = 100;
    private double ballX = 300;
    private double ballY = 200;
    private double ballSpeedX = 0;
    private double ballSpeedY = 0;
    private final double ballFriction = 0.987; // Decreased friction for smoother glide
    private int playerScore = 0;
    private int opponentScore = 0;
    private double goalX = 750;
    private double goalY = 250;
    private final double initialHitSpeedMultiplier = 60.0; // Lower speed for initial hits
    private final double movingHitSpeedMultiplier = 2; // Multiplier for moving hits
    private final double speedThreshold = 0.5; // Lower threshold for considering the ball as slow

    private Image fieldImage;
    private Image playerImage;
    private Image ballImage;
    private Image opponentImage;

    private double opponentX = 600;
    private double opponentY = 300;
    private double opponentSpeed = 3; // Default value, will be adjusted based on difficulty

    private String difficulty = "Rookie"; // Default difficulty
    private boolean isPaused = false;

    @Override
    public void start(Stage primaryStage) {
        try {
            fieldImage = new Image(new FileInputStream("new soccer field.png"), SCENE_WIDTH, SCENE_HEIGHT, false, true);
            playerImage = new Image(new FileInputStream("new player.png"), PLAYER_WIDTH, PLAYER_HEIGHT, false, true);
            ballImage = new Image(new FileInputStream("new ball.png"), BALL_WIDTH, BALL_HEIGHT, false, true);
            opponentImage = new Image(new FileInputStream("new opponent.png"), PLAYER_WIDTH, PLAYER_HEIGHT, false, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        primaryStage.setTitle("Enhanced Soccer Game");

        // Difficulty Selection Screen
        VBox menu = new VBox(20);
        menu.setStyle("-fx-background-color: lightgray; -fx-padding: 20;");
        menu.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        menu.setPrefSize(SCENE_WIDTH, SCENE_HEIGHT);

        Label title = new Label("Select Difficulty");
        title.setStyle("-fx-font-family: 'Times New Roman'; -fx-font-size: 24; -fx-padding: 20;");
        title.setAlignment(javafx.geometry.Pos.CENTER);

        Button rookieButton = new Button("Rookie Mode");
        rookieButton.setStyle("-fx-font-family: 'Times New Roman'; -fx-font-size: 16; -fx-padding: 10;");
        rookieButton.setOnAction(e -> {
            difficulty = "Rookie";
            startGame(primaryStage);
        });

        Button professionalButton = new Button("Professional Mode");
        professionalButton.setStyle("-fx-font-family: 'Times New Roman'; -fx-font-size: 16; -fx-padding: 10;");
        professionalButton.setOnAction(e -> {
            difficulty = "Professional";
            startGame(primaryStage);
        });

        Button legendaryButton = new Button("Legendary Mode");
        legendaryButton.setStyle("-fx-font-family: 'Times New Roman'; -fx-font-size: 16; -fx-padding: 10;");
        legendaryButton.setOnAction(e -> {
            difficulty = "Legendary";
            startGame(primaryStage);
        });

        menu.getChildren().addAll(title, rookieButton, professionalButton, legendaryButton);

        StackPane menuPane = new StackPane(menu);
        menuPane.setPrefSize(SCENE_WIDTH, SCENE_HEIGHT);

        Scene menuScene = new Scene(menuPane, SCENE_WIDTH, SCENE_HEIGHT);
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    private void startGame(Stage primaryStage) {
        Canvas canvas = new Canvas(SCENE_WIDTH, SCENE_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        StackPane root = new StackPane();
        root.getChildren().add(canvas);
        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);

        scene.setOnMouseMoved(this::handleMouseMoved);

        primaryStage.setTitle("Enhanced Soccer Game");
        primaryStage.setScene(scene);

        // Adjust opponent speed based on difficulty
        switch (difficulty) {
            case "Rookie":
                opponentSpeed = 75;
                break;
            case "Professional":
                opponentSpeed = 150;
                break;
            case "Legendary":
                opponentSpeed = 230;
                break;
        }

        // Add pause and restart functionality
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.P) {
                togglePause();
            } else if (e.getCode() == KeyCode.R) {
                restartGame(primaryStage);
            }
        });

        // Pause game when minimized or window loses focus
        primaryStage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                pauseGame();
            } else {
                resumeGame();
            }
        });

        primaryStage.setOnHiding(e -> pauseGame());
        primaryStage.setOnShowing(e -> resumeGame());

        // Main game loop
        new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (!isPaused) {
                    double deltaTime = (now - lastUpdate) / 1_000_000_000.0; // Convert nanoseconds to seconds
                    lastUpdate = now;
                    update(deltaTime);
                    draw(gc);
                }
            }
        }.start();
    }

    private void handleMouseMoved(MouseEvent event) {
        playerX = event.getX() - PLAYER_WIDTH / 2;
        playerY = event.getY() - PLAYER_HEIGHT / 2;
    }

    private void update(double deltaTime) {
        // Update ball position with current speed
        ballX += ballSpeedX * deltaTime;
        ballY += ballSpeedY * deltaTime;

        // Apply friction to the ball speed
        ballSpeedX *= ballFriction;
        ballSpeedY *= ballFriction;

        // Bounce ball off walls
        if (ballX < 0 || ballX > SCENE_WIDTH - BALL_WIDTH) {
            ballSpeedX = -ballSpeedX;
            ballX = Math.max(0, Math.min(SCENE_WIDTH - BALL_WIDTH, ballX));
        }
        if (ballY < 0 || ballY > SCENE_HEIGHT - BALL_HEIGHT) {
            ballSpeedY = -ballSpeedY;
            ballY = Math.max(0, Math.min(SCENE_HEIGHT - BALL_HEIGHT, ballY));
        }

        // Check for player collision with the ball
        if (Math.abs(playerX + PLAYER_WIDTH / 2 - (ballX + BALL_WIDTH / 2)) < (PLAYER_WIDTH + BALL_WIDTH) / 2
            && Math.abs(playerY + PLAYER_HEIGHT / 2 - (ballY + BALL_HEIGHT / 2)) < (PLAYER_HEIGHT + BALL_HEIGHT) / 2) {

            // Calculate collision normal
            double dx = (ballX + BALL_WIDTH / 2) - (playerX + PLAYER_WIDTH / 2);
            double dy = (ballY + BALL_HEIGHT / 2) - (playerY + PLAYER_HEIGHT / 2);
            double length = Math.sqrt(dx * dx + dy * dy);

            if (length != 0) {
                dx /= length;
                dy /= length;
            } else {
                dx = 1;
                dy = 0;
            }

            // Adjust ball position to avoid sticking
            double overlap = (PLAYER_WIDTH + BALL_WIDTH) / 2 - length;
            ballX += dx * overlap;
            ballY += dy * overlap;

            // Calculate the reflection vector
            double dotProduct = (ballSpeedX * dx + ballSpeedY * dy);
            ballSpeedX = ballSpeedX - 2 * dotProduct * dx;
            ballSpeedY = ballSpeedY - 2 * dotProduct * dy;

            // If the ball is moving very slowly, give it a higher initial speed
            if (Math.abs(ballSpeedX) < speedThreshold && Math.abs(ballSpeedY) < speedThreshold) {
                ballSpeedX = dx * initialHitSpeedMultiplier;
                ballSpeedY = dy * initialHitSpeedMultiplier;
            } else {
                // Increase ball speed upon collision with the player
                double speedIncrease = 2;
                ballSpeedX *= speedIncrease;
                ballSpeedY *= speedIncrease;
            }
        }

        // Simple AI for opponent
        double aiSpeed = opponentSpeed * deltaTime;
        double targetX = ballX + BALL_WIDTH / 2 - PLAYER_WIDTH / 2;
        double targetY = ballY + BALL_HEIGHT / 2 - PLAYER_HEIGHT / 2;

        if (opponentX < targetX) {
            opponentX += aiSpeed;
        } else {
            opponentX -= aiSpeed;
        }
        if (opponentY < targetY) {
            opponentY += aiSpeed;
        } else {
            opponentY -= aiSpeed;
        }

        // Prevent opponent from going out of bounds
        opponentX = Math.max(0, Math.min(SCENE_WIDTH - PLAYER_WIDTH, opponentX));
        opponentY = Math.max(0, Math.min(SCENE_HEIGHT - PLAYER_HEIGHT, opponentY));

        // Check for opponent collision with the ball
        if (Math.abs(opponentX + PLAYER_WIDTH / 2 - (ballX + BALL_WIDTH / 2)) < (PLAYER_WIDTH + BALL_WIDTH) / 2
            && Math.abs(opponentY + PLAYER_HEIGHT / 2 - (ballY + BALL_HEIGHT / 2)) < (PLAYER_HEIGHT + BALL_HEIGHT) / 2) {
            if (Math.abs(ballSpeedX) < speedThreshold && Math.abs(ballSpeedY) < speedThreshold) {
                // Ball is almost stationary
                ballSpeedX = (ballX + BALL_WIDTH / 2 - opponentX - PLAYER_WIDTH / 2) * initialHitSpeedMultiplier;
                ballSpeedY = (ballY + BALL_HEIGHT / 2 - opponentY - PLAYER_HEIGHT / 2) * initialHitSpeedMultiplier;
            } else {
                // Ball is moving
                double normalX = (ballX + BALL_WIDTH / 2 - opponentX - PLAYER_WIDTH / 2) / ((PLAYER_WIDTH + BALL_WIDTH) / 2);
                double normalY = (ballY + BALL_HEIGHT / 2 - opponentY - PLAYER_HEIGHT / 2) / ((PLAYER_WIDTH + BALL_WIDTH) / 2);
                
                // Calculate the reflection vector
                double dotProduct = (ballSpeedX * normalX + ballSpeedY * normalY);
                ballSpeedX = ballSpeedX - 2 * dotProduct * normalX;
                ballSpeedY = ballSpeedY - 2 * dotProduct * normalY;

                // Increase speed when the ball is moving
                ballSpeedX *= movingHitSpeedMultiplier;
                ballSpeedY *= movingHitSpeedMultiplier;
            }
        }

        // Check for goal
        if (ballX > goalX && ballX < goalX + GOAL_WIDTH && ballY > goalY && ballY < goalY + GOAL_HEIGHT) {
            playerScore++;
            resetBall();
        } else if (ballX < GOAL_WIDTH && ballY > goalY && ballY < goalY + GOAL_HEIGHT) {
            opponentScore++;
            resetBall();
        }
    }

    private void draw(GraphicsContext gc) {
        gc.drawImage(fieldImage, 0, 0);
        gc.drawImage(playerImage, playerX, playerY);
        gc.drawImage(ballImage, ballX, ballY);
        gc.drawImage(opponentImage, opponentX, opponentY);

        // Draw goal area
        gc.setStroke(Color.RED);
        gc.strokeRect(goalX, goalY, GOAL_WIDTH, GOAL_HEIGHT);
        gc.strokeRect(0, goalY, GOAL_WIDTH, GOAL_HEIGHT);

        // Draw score
        gc.setFill(Color.WHITE);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setFont(javafx.scene.text.Font.font("Times New Roman", 24));
        gc.fillText("Player Score: " + playerScore, SCENE_WIDTH / 4, 30);
        gc.fillText("Opponent Score: " + opponentScore, SCENE_WIDTH * 3 / 4, 30);
    }

    private void resetBall() {
        ballX = SCENE_WIDTH / 2 - BALL_WIDTH / 2;
        ballY = SCENE_HEIGHT / 2 - BALL_HEIGHT / 2;
        ballSpeedX = 0;
        ballSpeedY = 0;
    }

    private void togglePause() {
        isPaused = !isPaused;
    }

    private void pauseGame() {
        isPaused = true;
    }

    private void resumeGame() {
        isPaused = false;
    }

    private void restartGame(Stage primaryStage) {
        isPaused = false;
        start(primaryStage); // Restart the game and go back to the difficulty menu
    }

    public static void main(String[] args) {
        launch(args);
    }
}
