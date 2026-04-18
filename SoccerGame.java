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
import java.io.File;
import javafx.geometry.Pos;

public class SoccerGame extends Application {
    private static final double WIN_WIDTH = 800;
    private static final double WIN_HEIGHT = 600;
    
    private final double PLAYER_WIDTH = 80;
    private final double PLAYER_HEIGHT = 60;
    private final double BALL_WIDTH = 25;
    private final double BALL_HEIGHT = 25;
    private final double GOAL_WIDTH = 60; 
    private final double GOAL_HEIGHT = 130; 

    private double playerX, playerY;
    private double ballX, ballY, ballSpeedX, ballSpeedY;
    private final double ballFriction = 0.985; 
    
    private int playerScore = 0;
    private int opponentScore = 0;
    
    private double opponentX, opponentY;
    private double opponentSpeed = 150; 
    private Image fieldImage, playerImage, ballImage, opponentImage;
    private String difficulty = "Rookie"; 
    private boolean isPaused = false;
    private double spawnTimer = 0;

    @Override
    public void start(Stage primaryStage) {
        loadImages();
        primaryStage.setTitle("Soccer Video Game");
        
        VBox menu = new VBox(25);
        menu.setStyle("-fx-background-color: #87CEEB; -fx-padding: 50;");
        menu.setAlignment(Pos.CENTER);

        Label title = new Label("Soccer Video Game");
        title.setStyle("-fx-text-fill: #FF8C00; -fx-font-family: 'Arial'; -fx-font-size: 50; -fx-font-weight: bold;");

        Button rookieBtn = createMenuButton("Rookie", "Rookie", primaryStage);
        Button proBtn = createMenuButton("Professional", "Professional", primaryStage);
        Button legendBtn = createMenuButton("Legendary", "Legendary", primaryStage);
        
        Label hint = new Label("F: Full Screen | P: Pause | R: Reset to Menu");
        hint.setStyle("-fx-text-fill: #FFFFFF; -fx-text-alignment: center; -fx-padding: 20; -fx-font-weight: bold;");
        
        menu.getChildren().addAll(title, rookieBtn, proBtn, legendBtn, hint);
        Scene menuScene = new Scene(menu, WIN_WIDTH, WIN_HEIGHT);
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    private void startGame(Stage primaryStage) {
        StackPane root = new StackPane();
        Canvas canvas = new Canvas(primaryStage.getWidth(), primaryStage.getHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        canvas.widthProperty().bind(primaryStage.widthProperty());
        canvas.heightProperty().bind(primaryStage.heightProperty());
        root.getChildren().add(canvas);
        
        Scene scene = new Scene(root, primaryStage.getWidth(), primaryStage.getHeight());
        scene.setOnMouseMoved(this::handleMouseMoved);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.P) isPaused = !isPaused;
            if (e.getCode() == KeyCode.R) restartGame(primaryStage);
            if (e.getCode() == KeyCode.F) primaryStage.setFullScreen(!primaryStage.isFullScreen());
        });
        
        primaryStage.setScene(scene);

        if (difficulty.equals("Rookie")) opponentSpeed = 160;
        else if (difficulty.equals("Professional")) opponentSpeed = 280;
        else opponentSpeed = 450;

        resetBall(primaryStage.getWidth(), primaryStage.getHeight());

        new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (!isPaused) {
                    if (lastUpdate == 0) lastUpdate = now;
                    double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                    lastUpdate = now;
                    update(deltaTime, canvas.getWidth(), canvas.getHeight());
                    draw(gc, canvas.getWidth(), canvas.getHeight());
                } else {
                    lastUpdate = 0; 
                }
            }
        }.start();
    }

    private void update(double deltaTime, double w, double h) {
        if (spawnTimer > 0) {
            spawnTimer -= deltaTime;
            return; 
        }

        ballX += ballSpeedX * deltaTime;
        ballY += ballSpeedY * deltaTime;
        ballSpeedX *= ballFriction;
        ballSpeedY *= ballFriction;

        // Bounce off Top/Bottom walls
        if (ballY < 0 || ballY > h - BALL_HEIGHT) {
            ballSpeedY *= -0.8;
            ballY = Math.max(0, Math.min(h - BALL_HEIGHT, ballY));
        }

        // --- UPDATED BOUNCE LOGIC (No Goal Kicks) ---
        double goalTop = h / 2 - GOAL_HEIGHT / 2;
        double goalBottom = h / 2 + GOAL_HEIGHT / 2;

        // Left End Line
        if (ballX < 0) {
            if (ballY > goalTop && ballY < goalBottom) {
                playerScore++; // Goal!
                resetBall(w, h);
            } else {
                ballSpeedX *= -0.8; // Bounce back if it misses the goal
                ballX = 0;
            }
        } 
        // Right End Line
        else if (ballX > w - BALL_WIDTH) {
            if (ballY > goalTop && ballY < goalBottom) {
                opponentScore++; // Goal!
                resetBall(w, h);
            } else {
                ballSpeedX *= -0.8; // Bounce back if it misses the goal
                ballX = w - BALL_WIDTH;
            }
        }

        if (checkHit(playerX, playerY)) applyHit(playerX, playerY);
        if (checkHit(opponentX, opponentY)) applyHit(opponentX, opponentY);

        // AI Logic: Attacking Right Goal
        double targetX, targetY;
        if (ballX > w * 0.1) { 
            targetX = ballX - PLAYER_WIDTH; 
            targetY = ballY - PLAYER_HEIGHT / 2;
        } else {
            targetX = w * 0.05; 
            targetY = h / 2 - PLAYER_HEIGHT / 2;
        }

        if (Math.abs(opponentX - targetX) > 5) 
            opponentX += (opponentX < targetX ? 1 : -1) * (opponentSpeed * 0.7) * deltaTime;
        if (Math.abs(opponentY - targetY) > 5) 
            opponentY += (opponentY < targetY ? 1 : -1) * opponentSpeed * deltaTime;

        opponentX = Math.max(0, Math.min(w * 0.65, opponentX));
    }

    private void resetBall(double w, double h) {
        ballX = w / 2 - BALL_WIDTH / 2;
        ballY = h / 2 - BALL_HEIGHT / 2;
        ballSpeedX = 0; ballSpeedY = 0;
        
        opponentX = w * 0.15; 
        opponentY = h / 2 - PLAYER_HEIGHT / 2;
        
        playerX = w * 0.85 - PLAYER_WIDTH; 
        playerY = h / 2 - PLAYER_HEIGHT / 2;
        
        spawnTimer = 1.0; 
    }

    private void draw(GraphicsContext gc, double w, double h) {
        gc.clearRect(0, 0, w, h);
        if (fieldImage != null) gc.drawImage(fieldImage, 0, 0, w, h);
        else { gc.setFill(Color.DARKGREEN); gc.fillRect(0,0,w,h); }

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(4);
        double goalTop = h/2 - GOAL_HEIGHT/2;
        gc.strokeRect(0, goalTop, GOAL_WIDTH, GOAL_HEIGHT);
        gc.strokeRect(w - GOAL_WIDTH, goalTop, GOAL_WIDTH, GOAL_HEIGHT);

        if (playerImage != null) gc.drawImage(playerImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        if (opponentImage != null) gc.drawImage(opponentImage, opponentX, opponentY, PLAYER_WIDTH, PLAYER_HEIGHT);
        if (ballImage != null) gc.drawImage(ballImage, ballX, ballY, BALL_WIDTH, BALL_HEIGHT);

        gc.setFill(Color.ORANGE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 24));
        gc.fillText("Opponent: " + opponentScore, 80, 50); 
        gc.fillText("Player: " + playerScore, w - 200, 50);
        
        if (isPaused) {
            gc.setFill(Color.web("rgba(0,0,0,0.5)"));
            gc.fillRect(0,0,w,h);
            gc.setFill(Color.ORANGE);
            gc.fillText("PAUSED", w/2 - 50, h/2);
        }
    }

    private void handleMouseMoved(MouseEvent event) {
        playerX = event.getX() - PLAYER_WIDTH / 2;
        playerY = event.getY() - PLAYER_HEIGHT / 2;
    }

    private boolean checkHit(double x, double y) {
        return Math.abs(x + PLAYER_WIDTH/2 - (ballX + BALL_WIDTH/2)) < (PLAYER_WIDTH + BALL_WIDTH)/2
            && Math.abs(y + PLAYER_HEIGHT/2 - (ballY + BALL_HEIGHT/2)) < (PLAYER_HEIGHT + BALL_HEIGHT)/2;
    }

    private void applyHit(double px, double py) {
        double dx = (ballX + BALL_WIDTH/2) - (px + PLAYER_WIDTH/2);
        double dy = (ballY + BALL_HEIGHT/2) - (py + PLAYER_HEIGHT/2);
        ballSpeedX = dx * 15; 
        ballSpeedY = dy * 15;
    }

    private void loadImages() {
        try {
            fieldImage = new Image(new File("new soccer field.png").toURI().toURL().toString());
            playerImage = new Image(new File("new player.png").toURI().toURL().toString());
            ballImage = new Image(new File("new ball.png").toURI().toURL().toString());
            opponentImage = new Image(new File("new opponent.png").toURI().toURL().toString());
        } catch (Exception e) {
            System.out.println("Resource images not found.");
        }
    }

    private Button createMenuButton(String text, String diff, Stage stage) {
        Button btn = new Button(text);
        btn.setPrefWidth(250);
        btn.setStyle("-fx-font-size: 18; -fx-base: #ffffff; -fx-font-weight: bold; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            difficulty = diff;
            startGame(stage);
        });
        return btn;
    }

    private void restartGame(Stage primaryStage) {
        playerScore = 0; opponentScore = 0;
        isPaused = false;
        start(primaryStage); 
    }

    public static void main(String[] args) { launch(args); }
}
