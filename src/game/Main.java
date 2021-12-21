package game;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import mars.drawingx.application.DrawingApplication;
import mars.drawingx.application.Options;
import mars.drawingx.application.parameters.WindowState;
import mars.drawingx.drawing.Drawing;
import mars.drawingx.drawing.DrawingUtils;
import mars.drawingx.drawing.View;
import mars.drawingx.gadgets.annotations.GadgetDouble;
import mars.geometry.Transformation;
import mars.geometry.Vector;
import mars.input.InputEvent;
import mars.input.InputState;
import mars.utils.Numeric;

import javax.swing.*;


public class Main implements Drawing {

    int[][] map = {
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 1, 1, 0, 0, 1},
            {1, 0, 0, 1, 1, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
    };
    int nGrid = 8;
    int gridSize = 64;
    int mapSize = 512;

    Player player = new Player(Vector.vec(-2*gridSize, 0));

    @GadgetDouble(max = 100)
    double speed = 1;

    int limit = 0;

    public void draw(View view) {
        if (limit++ > 150) {
            limit = 0;
//            System.out.printf("%.2f\n", player.angle * 360);
        }

        DrawingUtils.clear(view, Color.gray(0.125));

        drawMap(view);

        player.update();

        view.setFill(Color.RED);
        view.setStroke(Color.RED);
        view.setLineWidth(5);

        view.strokeLine(player.p, player.p.add(Vector.vec(10,  0).rotate(player.angle)));
        view.fillCircleCentered(player.p, 5);
        view.fillCircleCentered(player.closest64y(), 5);
    }


    public void drawMap(View view) {
        Vector start = Vector.vec(-mapSize/2, -mapSize/2);
        view.setFill(Color.WHITE);
        view.setStroke(Color.GRAY);
        view.setLineWidth(1);


        for (int i = 0; i < nGrid; i++) {
            for (int j = 0; j < nGrid; j++) {
                if (map[i][j] == 1) {
                    view.fillRect(
                            start.add(Vector.vec(j * gridSize, i * gridSize)),
                            Vector.vec(gridSize, gridSize)
                    );
                }

                view.strokeRect(
                        start.add(Vector.vec(j * gridSize, i * gridSize)),
                        Vector.vec(gridSize, gridSize)
                );
            }
        }
    }

    public void drawRejz() {
        
        double rayAngle = player.angle;

        for (int i = 0; i < 1; i++) {
        }
    }


    public static void main(String[] args) {
        Options options = new Options();

        options.constructGui = false;
        options.hideMouseCursor = true;
        options.drawingSize = new Vector(600, 600);
        options.resizable = false;
        options.initialWindowState = WindowState.NORMAL;

        DrawingApplication.launch(options);
    }

    @Override
    public void receiveEvent(View view, InputEvent event, InputState state, Vector pointerWorld, Vector pointerViewBase) {
        if (event.isKey(KeyCode.W)) {
            int isPressed = state.keyPressed(KeyCode.W) ? 1 : 0;
            player.v = Vector.vec(speed * isPressed, 0).rotate(player.angle);
        }

        if (event.isKey(KeyCode.S)) {
            int isPressed = state.keyPressed(KeyCode.S) ? -1 : 0;
            player.v = Vector.vec(speed * isPressed, 0).rotate(player.angle);
        }

        if (event.isKey(KeyCode.A)) {
            player.toRotate = state.keyPressed(KeyCode.A) ? 1 : 0;
        }

        if (event.isKey(KeyCode.D)) {
            player.toRotate = state.keyPressed(KeyCode.D) ? -1 : 0;
        }
    }
}

class Player {
    Vector p;
    Vector v;
    double angle;
    int toRotate;

    public Player(Vector p) {
        this.p = p;
        v = Vector.ZERO;
        angle = 0;
    }

    public void update() {
        p = p.add(v);
        angle += toRotate * 0.005;
        if (angle >= 1) angle = 0;
        if (angle <  0) angle = 1;
    }

    public Vector closest64y() {
        double y;
        if (angle < 0.5) {
            // looking up
            y = 64 - Numeric.mod(p.y , 64);
        } else {
            // looking down
            y = -Numeric.mod(p.y, 64);
        }

        double sin = Math.sin(angle*2*Math.PI);
        double cos = Math.cos(angle*2*Math.PI);
        double x = cos * (y / sin);


        return Vector.vec(p.x + x, y + p.y);
    }
}
