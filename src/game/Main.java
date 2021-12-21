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

    Player player = new Player(Vector.vec(2 * gridSize, 2 * gridSize));

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
        drawRejz(view);

        view.setFill(Color.RED);
        view.setStroke(Color.RED);
        view.setLineWidth(5);

        view.strokeLine(player.p, player.p.add(Vector.vec(10,  0).rotate(player.angle)));
        view.fillCircleCentered(player.p, 5);
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

    public void drawRejz(View view) {
        view.setStroke(Color.CORNFLOWERBLUE);

        view.stateStore();

        view.setTransformation(Transformation.translation(Vector.vec(-mapSize/2, -mapSize/2)));

        Vector vHit = new Vector(2*mapSize, 2*mapSize);
        Vector hHit = new Vector(2*mapSize, 2*mapSize);

        int mapX = 0, mapY = 0, depth;
        double x, y, xOffset, yOffset, rayX, rayY;

        double tan = Numeric.tanT(player.angle);
        double ctan = 1 / tan;

        double mody64 = Numeric.mod(player.p.y, gridSize);

        for (int i = 0; i < 1; i++) {
            if (player.angle < 0.5) { // looking up
                rayY = gridSize - mody64;
                yOffset = gridSize;
            } else { // looking down
                rayY = - mody64;
                yOffset = - gridSize;
            }
            rayX = rayY * ctan;

            xOffset = yOffset * ctan;


            depth = 0;
            if (player.angle == 0 || player.angle == 0.5) depth = 8;

            while (depth < 8) {
                mapX = (int) ((rayX + player.p.x) / gridSize);
                mapY = (int) ((rayY + player.p.y) / gridSize); //  - (player.angle < 0.5 ? 0 : 1);

                System.out.println(rayX + "   " + rayY);

                if (mapY >= 0 && mapY < nGrid && mapX >= 0 && mapX < nGrid && map[mapY][mapX] == 1) {
                    break;
                } else {
                    rayY += yOffset;
                    rayX += xOffset;
                    depth++;
                }
            }

            if (depth < 9) {
                view.setFill(Color.CORNFLOWERBLUE);
                vHit = Vector.vec(rayX + player.p.x, player.p.y + rayY);
//                view.fillCircleCentered(vHit, 5);
            }

            /*  */

            mody64 = Numeric.mod(player.p.x, gridSize);

            if (player.angle < 0.25 || player.angle > 0.75) { // looking up
                rayX = gridSize - mody64;
                xOffset = gridSize;
            } else { // looking down
                rayX = - mody64;
                xOffset = - gridSize;
            }
            rayY = rayX * tan;

            yOffset = xOffset * tan;


            depth = 0;
            if (player.angle == 0.25 || player.angle == 0.75) depth = 8;

            while (depth < 8) {
                mapX = (int) ((rayX + player.p.x) / gridSize);
                mapY = (int) ((rayY + player.p.y) / gridSize); //  - (player.angle < 0.5 ? 0 : 1);

                if (mapY >= 0 && mapY < nGrid && mapX >= 0 && mapX < nGrid && map[mapY][mapX] == 1) {
                    break;
                } else {
                    rayY += yOffset;
                    rayX += xOffset;
                    depth++;
                }
            }

            if (depth < 9) {
                view.setFill(Color.CORNFLOWERBLUE);
                hHit = Vector.vec(rayX + player.p.x, player.p.y + rayY);
//                view.fillCircleCentered(hHit, 5);
            }

            double hDist = hHit.distanceTo(player.p);
            double vDist = vHit.distanceTo(player.p);

            Vector ray = hDist < vDist ? hHit : vHit;

            view.setStroke(Color.RED);
            view.setFill(Color.CORNFLOWERBLUE);
            view.strokeLine(player.p, ray);

        }
    }

//    public void drawRejzOld(View view) {
//        view.setStroke(Color.CORNFLOWERBLUE);
//
//        view.stateStore();
//
//        view.setTransformation(Transformation.translation(Vector.vec(-mapSize/2, -mapSize/2)));
//
//        Vector vHit, hHit;
//
//        int mapX = 0, mapY = 0, depth;
//        double x, y, xOffset, yOffset, rayX, rayY;
//
//        // TODO use normal system, but translate view after rendering
//
//        double tan = Numeric.tanT(player.angle);
//        double ctan = 1 / tan;
//
//        for (int i = 0; i < 1; i++) {
//
//            /* vertical rays */
//
//            boolean up;
//            if (player.angle < 0.5) { // looking up
//                up = true;
//                y = 64 - Numeric.mod(player.p.y , 64);
//            } else { // looking down
//                up = false;
//                y = -Numeric.mod(player.p.y, 64);
//                mapOffsetY -= gridSize;
//            }
//
//            x = y * ctan;
//
//            depth = 0;
//            if  (player.angle == 0 || player.angle == 0.5) depth = 8;
//
//            yOffset = 64 * (up ? 1 : -1);
//
//            rayX = x;
//            rayY = y;
//            while (depth < 8) {
//                mapX = (int)((rayX + mapOffsetX) / gridSize);
//                mapY = (int)((rayY + mapOffsetY) / gridSize);
//
//                if (mapY >= 0 && mapY < nGrid && mapX >= 0 && mapX < nGrid) {
//                    if (map[mapY][mapX] == 1) {
//                        break;
//                    }
//                }
//
//                rayY += yOffset;
//                rayX += rayY * ctan;
//
//                depth++;
//            }
//
//            vHit = Vector.vec(player.p.x + rayX, player.p.y + rayY);
//
//
//            /* horizontal rays */
//
//            boolean right;
//
//            if (player.angle < 0.25 || player.angle > 0.75) { // looking right
//                right = true;
//                x = 64 - Numeric.mod(player.p.x , 64);
//            } else {
//                right = false;
//                x = -1.0 * Numeric.mod(player.p.x, 64);
//                mapOffsetX -= gridSize;
//            }
//
//            y = x * tan;
//
//            depth = 0;
//            if  (player.angle == 0.25 || player.angle == 0.75) depth = 8;
//
//            xOffset = 64 * (right ? 1 : -1);
//
//            rayX = x;
//            rayY = y;
//            while (depth < 8) {
//                mapX = (int)((rayX + mapOffsetX) / 64);
//                mapY = (int)((rayY + mapOffsetY) / 64);
//
//                if (mapY >= 0 && mapY < nGrid && mapX >= 0 && mapX < nGrid) {
//                    if (map[mapY][mapX] == 1) {
//                        break;
//                    }
//                }
//
//                rayX += xOffset;
//                rayY += tan * rayX;
//
//                depth++;
//
////            }
//
//            hHit = Vector.vec(player.p.x + rayX, player.p.y + rayY);
//
//            double distH = player.p.distanceTo(hHit);
//            double distV = player.p.distanceTo(vHit);
//
//            Vector closestHit = distH < distV ? hHit : vHit;
//
////            view.strokeLine(player.p, vHit);
////            view.setStroke(Color.RED);
////            view.strokeLine(player.p, hHit);
//
//            view.strokeLine(player.p, closestHit);
//
//        }
//
//        view.stateRestore();
//    }


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

//        if (event.isKey(KeyCode.A)) {
        if (event.isKeyPress(KeyCode.A)) {
//            player.toRotate = state.keyPressed(KeyCode.A) ? 1 : 0;
            player.rotate(true);
        }

//        if (event.isKey(KeyCode.D)) {
        if (event.isKeyPress(KeyCode.D)) {
//            player.toRotate = state.keyPressed(KeyCode.D) ? -1 : 0;
            player.rotate(false);
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
//        angle += toRotate * 0.002;
    }

    public void rotate(boolean left) {
        angle += 0.04 * (left ? 1 : -1);
        if (angle >= 1) angle = 0;
        if (angle <  0) angle = 1;
    }

}
