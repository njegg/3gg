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
import mars.drawingx.gadgets.annotations.GadgetDoubleLogarithmic;
import mars.drawingx.gadgets.annotations.GadgetInteger;
import mars.geometry.Transformation;
import mars.geometry.Vector;
import mars.input.InputEvent;
import mars.input.InputState;
import mars.utils.Numeric;

/*
 * TODO
 *  fix:
 *    fps
 *    consistent screen
 *  textures
 *    doors
 *  walking animation (sin/cos screen offset?)
 *  particles:
 *    shooting
 *    enemies
 *  floor/roof texture
 * */

public class Main implements Drawing {

    int[][] map = {
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 1, 1, 1, 0, 1},
            {1, 0, 0, 1, 0, 0, 0, 1},
            {1, 0, 0, 1, 1, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
    };
    int nGrid = 8;
    int gridSize = 64;
    int mapSize = 512;

    Player player = new Player(Vector.vec(2 * gridSize, 2 * gridSize));
    double speed = 2;

    boolean twoD = false;

    @GadgetInteger
    int rayz = 60;

    @GadgetDoubleLogarithmic(min = 0.0001, max = 0.01)
    double dAngle = 0.005;

    int limit = 0;
    public void draw(View view) {
        DrawingUtils.clear(view, Color.gray(0.125));

        player.update();

        if (twoD) {
            drawMap(view);
        } else {
            view.setFill(Color.RED);
            view.setStroke(Color.RED);
            view.setLineWidth(5);

            view.strokeLine(player.p, player.p.add(Vector.vec(10,  0).rotate(player.angle)));
            view.fillCircleCentered(player.p, 5);
        }
        rejz(view);

    }


    public void drawMap(View view) {
        view.setFill(Color.WHITE);
        view.setStroke(Color.GRAY);
        view.setLineWidth(1);

        view.stateStore();

        view.setTransformation(Transformation.translation(Vector.vec(-mapSize/2, -mapSize/2)));

        for (int i = 0; i < nGrid; i++) {
            for (int j = 0; j < nGrid; j++) {
                if (map[i][j] == 1) {
                    view.fillRect(
                            Vector.vec(j * gridSize, i * gridSize),
                            Vector.vec(gridSize, gridSize)
                    );
                }

                view.strokeRect(
                        Vector.vec(j * gridSize, i * gridSize),
                        Vector.vec(gridSize, gridSize)
                );
                view.setFill(Color.RED);
                view.fillText(String.format("%d,%d", j, i), Vector.vec(j * gridSize, i * gridSize));
                view.setFill(Color.WHITE);
            }
        }

        view.stateRestore();
    }

    public void rejz(View view) {
        view.stateStore();

        view.setStroke(Color.CORNFLOWERBLUE);
        view.setTransformation(Transformation.translation(Vector.vec(-mapSize/2, -mapSize/2)));

        Vector vHit, hHit;
//
//        int rajz = 100;
//        double dAngle = 0.001;

        double angle = player.angle - (dAngle * rayz / 2);

        if (angle >  1) angle %= 1;
        if (angle <  0) angle += 1;

        for (int i = 0; i < rayz; i++) {
            vHit = vHit(angle);
            hHit = hHit(angle);

            double hDist = hHit.distanceTo(player.p);
            double vDist = vHit.distanceTo(player.p);
            double dist = Math.min(vDist, hDist);

            if (twoD) {
                Vector ray = hDist < vDist ? hHit : vHit;
                view.setStroke(Color.ORANGE);
                view.strokeLine(player.p, ray);
            } else {
                double wallH = mapSize * 15 / dist;
                double x = i*10-50; // x pos of wall

                Vector wallBottom = Vector.vec(x, -wallH + 300);
                Vector wallTop = Vector.vec(x, wallH + 300);

                view.setLineWidth(10);
                view.setStroke(Color.hsb(0, 1, dist == vDist ? 1 : 0.7));
                view.strokeLine(wallBottom, wallTop);

                view.setStroke(Color.gray(0.1));
                view.strokeLine(wallBottom, Vector.vec(x, -50));

                view.setStroke(Color.hsb(260, 1, 0.2));
                view.strokeLine(wallTop, Vector.vec(x, 600));
            }

            angle += dAngle;
            if (angle >  1) angle %= 1;
            if (angle <  0) angle += 1;
        }
    }

    private Vector hHit(double angle) {
        int mapX, mapY, depth;
        double xOffset, yOffset, rayX, rayY;

        double tan = Numeric.tanT(angle);
        double mod64 = Numeric.mod(player.p.x, gridSize);

        boolean right = false;
        if (angle < 0.25 || angle > 0.75) { // looking right
            right = true;
            rayX = gridSize - mod64;
            xOffset = gridSize;
        } else {
            rayX = -mod64;
            xOffset = -gridSize;
        }
        rayY = rayX * tan;
        yOffset = xOffset * tan;

        depth = 0;
        if (angle == 0.25 || angle == 0.75) depth = 8;

        while (depth < 8) {
            mapX = (int) ((rayX + player.p.x) / gridSize) - (right ? 0 : 1);
            mapY = (int) ((rayY + player.p.y) / gridSize);

            if (mapY >= 0 && mapY < nGrid && mapX >= 0 && mapX < nGrid && map[mapY][mapX] == 1) {
                break;
            } else {
                rayY += yOffset;
                rayX += xOffset;
                depth++;
            }
        }

        return Vector.vec(rayX + player.p.x, player.p.y + rayY);
    }

    private Vector vHit(double angle) {
        int mapX, mapY, depth;
        double xOffset, yOffset, rayX, rayY;

        double mod64 = Numeric.mod(player.p.y, gridSize);
        double ctan = 1.0 / Numeric.tanT(angle);;

        boolean up = false;
        if (angle < 0.5) {       // looking up
            up = true;
            rayY = gridSize - mod64;
            yOffset = gridSize;
        } else {
            rayY = - mod64;
            yOffset = - gridSize;
        }
        rayX = rayY * ctan;
        xOffset = yOffset * ctan;

        depth = 0;
        if (angle == 0 || angle == 0.5) depth = 8;

        while (depth < 8) {
            mapX = (int) ((rayX + player.p.x) / gridSize);
            mapY = (int) ((rayY + player.p.y) / gridSize) - (up ? 0 : 1);

            if (mapY >= 0 && mapY < nGrid && mapX >= 0 && mapX < nGrid && map[mapY][mapX] == 1) {
                break;
            } else {
                rayY += yOffset;
                rayX += xOffset;
                depth++;
            }
        }

        return Vector.vec(rayX + player.p.x, player.p.y + rayY);
    }


    public static void main(String[] args) {
        Options options = new Options();

        options.constructGui = true;
        options.hideMouseCursor = true;
        options.drawingSize = new Vector(600, 600);
        options.resizable = true;
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
//        if (event.isKeyPress(KeyCode.A)) {
            player.toRotate = state.keyPressed(KeyCode.A) ? 1 : 0;
//            player.rotate(true);
        }

        if (event.isKey(KeyCode.D)) {
//        if (event.isKeyPress(KeyCode.D)) {
            player.toRotate = state.keyPressed(KeyCode.D) ? -1 : 0;
//            player.rotate(false);
        }

        if (event.isKeyPress(KeyCode.C)) {
            twoD = !twoD;
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
        angle += toRotate * 0.003;
        if (angle >  1) angle %= 1;
        if (angle <  0) angle += 1;
    }
}
