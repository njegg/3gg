package game;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import mars.drawingx.application.DrawingApplication;
import mars.drawingx.application.Options;
import mars.drawingx.application.parameters.WindowState;
import mars.drawingx.drawing.Drawing;
import mars.drawingx.drawing.DrawingUtils;
import mars.drawingx.drawing.View;
import mars.geometry.Transformation;
import mars.geometry.Vector;
import mars.input.InputEvent;
import mars.input.InputState;
import mars.utils.Numeric;


public class Main implements Drawing {

     static int[][] map = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,2,0,0,1,0,2,0,0,0,0,0,1},
            {1,0,0,0,1,0,0,1,0,1,0,1,2,1,1,1},
            {1,0,0,0,1,1,1,1,0,1,0,1,0,0,0,1},
            {1,1,2,1,1,0,0,0,0,1,0,1,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,1,0,1,1,2,1,1},
            {1,2,1,0,0,0,1,1,1,1,1,1,0,0,0,1},
            {1,0,1,0,0,0,1,0,0,0,0,1,0,0,0,1},
            {1,0,1,0,0,0,2,0,2,2,0,1,0,0,0,1},
            {1,0,1,0,0,0,1,0,2,2,0,1,0,0,0,1},
            {1,0,1,0,0,0,1,0,0,0,0,2,0,0,0,1},
            {1,0,1,0,0,0,1,1,1,1,1,1,0,0,0,1},
            {1,0,1,0,0,0,0,0,1,0,0,1,0,0,0,1},
            {1,0,1,0,0,0,0,0,2,0,0,2,0,0,0,1},
            {1,0,2,0,0,0,0,0,1,0,0,1,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
    };

    static int nGrid = map.length;
    static int gridSize = 64;
    Vector mapStart = Vector.vec(-256, -256);

    static class Player {
        Vector p;
        double angle;
        double speed = 2;

        int toRotate;
        boolean canMove;
        boolean forward;
        boolean back;
        boolean canForward;
        boolean canBackwards;

        boolean lookingUp;
        boolean lookingRight;

        boolean signalE;

        public Player(Vector p) {
            this.p = p;
            canMove = true;
        }

        public void update() {
            if (canForward && forward) {
                p = p.add(Vector.polar(speed * frameDiff, angle));
            }
            if (canBackwards && back) {
                p = p.sub(Vector.polar(speed * frameDiff, angle));
            }

            if (toRotate != 0) {
                angle += toRotate * 0.01 * frameDiff;
                if (angle >  1) angle %= 1;
                if (angle <  0) angle += 1;

                lookingUp = angle < 0.5;
                lookingRight = angle < 0.25 || angle > 0.75;
            }
        }
    }


    Player player;
    boolean twoD = false;

    int rayz = 60;
    double dAngle = 0.005;

    long frameCur, framePrev;
    public static double frameDiff;

    @Override
    public void init(View view) {
        framePrev = System.currentTimeMillis();
        player = new Player(Vector.vec(2 * gridSize, 2 * gridSize));
    }

    public void draw(View view) {
        DrawingUtils.clear(view, Color.gray(0.125));

        // more frames = slower (smaller diff), less frames = faster (bigger diff)
        frameCur = System.currentTimeMillis();
        frameDiff = frameCur - framePrev;
        framePrev = frameCur;
        frameDiff = frameDiff * 0.08;

        player.update();
        player.canForward = !collidesWithWallFront();
        player.canBackwards = !collidesWithWallBack();

        view.stateStore();
        view.addTransformation(Transformation.translation(mapStart));

        if (twoD) {
            view.stateStore();

            view.addTransformation(Transformation.translation(player.p.inverse())); // follow player
            view.addTransformation(Transformation.translation(mapStart.inverse()));

            drawMap(view);

            view.stateRestore();
        }

        rejz(view);

        view.stateRestore();

        view.setFill(Color.WHITE);
        view.fillText("PRESS [C] TO TOGGLE PERSPECTIVE", Vector.vec(-300, -300));
    }

    public void drawMap(View view) {
        view.setStroke(Color.GRAY);
        view.setLineWidth(1);

        for (int i = 0; i < nGrid; i++) {
            for (int j = 0; j < nGrid; j++) {
                if (map[i][j] == 1) view.setFill(Color.WHITE);
                else                view.setFill(Color.BLUE);

                if (map[i][j] > 0) {
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
    }

    public void rejz(View view) {
        // calculate and draw rays/walls
        Vector vHit, hHit;

        double angle = player.angle + (dAngle * rayz / 2);
        double dist;

        if (angle >  1) angle %= 1;
        if (angle <  0) angle += 1;

        for (int i = 0; i < rayz; i++) {
            vHit = vHit(angle);
            hHit = hHit(angle);

            double hDist = hHit.distanceTo(player.p);
            double vDist = vHit.distanceTo(player.p);
            dist = Math.min(vDist, hDist);
            Vector ray = hDist < vDist ? hHit : vHit;

            if (twoD) {
                view.stateStore();

                view.addTransformation(Transformation.translation(player.p.inverse()));
                view.addTransformation(Transformation.translation(mapStart.inverse()));

                view.setStroke(Color.ORANGE);
                view.strokeLine(player.p, ray);

                view.stateRestore();
            } else {
                // angle between player and ray
                double PRangle = angle - player.angle;
                if (PRangle >  1) PRangle %= 1;
                if (PRangle <  0) PRangle += 1;

                // side rays are longer -> walls left and right are smaller
                // this scales them to be the same as the distance from
                // player to wall (middle ray)
                // side rays still a bit weird but looks way better
                double scale = Numeric.cosT(PRangle);

                double wallH = 512 * 15 / (dist * scale);
                double x = i*10-40; // x pos of wall

                Vector wallBottom = Vector.vec(x, -wallH + 300);
                Vector wallTop = Vector.vec(x, wallH + 300);

                Vector im = toMapCoords(ray);
                int mapX = (int) im.x;
                int mapY = (int) im.y;

                mapX -= (!lookingRight(angle) && vDist > hDist) ? 1 : 0;
                mapY -= (!lookingUp(angle)    && vDist < hDist) ? 1 : 0;

                double hue = 0;
                if (inMap(mapX, mapY) && map[mapY][mapX] == 2) {
                    hue = 240;
                }

                view.setLineWidth(10);
                view.setStroke(Color.hsb(hue, 1, dist == vDist ? 1 : 0.7));
                view.strokeLine(wallBottom, wallTop);

                view.setStroke(Color.gray(0.1));
                view.strokeLine(wallBottom, Vector.vec(x, -50));

                view.setStroke(Color.hsb(260, 1, 0.2));
                view.strokeLine(wallTop, Vector.vec(x, 600));
            }

            // next ray
            angle -= dAngle;
            if (angle >  1) angle %= 1;
            if (angle <  0) angle += 1;
        }
    }

    public boolean lookingUp(double angle) {
        return angle < 0.5;
    }

    public boolean lookingRight(double angle) {
        return angle < 0.25 || angle > 0.75;
    }

    private static Vector toMapCoords(Vector v) {
        return Vector.vec((int) (v.x / gridSize), (int) (v.y / gridSize));
    }

    // mixed up horisontal and vertical (again) and lazy to fix
    private Vector hHit(double angle) {
        int mapX, mapY, depth;
        double xOffset, yOffset, rayX, rayY;

        double tan = Numeric.tanT(angle);
        double mod64 = Numeric.mod(player.p.x, gridSize);

        boolean right = false;
        if (lookingRight(angle)) { // looking right
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

            if (mapY >= 0 && mapY < nGrid && mapX >= 0 && mapX < nGrid && map[mapY][mapX] > 0) {
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
        if (lookingUp(angle)) {       // looking up
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

            if (mapY >= 0 && mapY < nGrid && mapX >= 0 && mapX < nGrid && map[mapY][mapX] > 0) {
                break;
            } else {
                rayY += yOffset;
                rayX += xOffset;
                depth++;
            }
        }

        return Vector.vec(rayX + player.p.x, player.p.y + rayY);
    }

    public boolean collidesWithWallFront() {
        double collideLength = 20;
        int mapX, mapY;

        Vector collider = player.p.add(Vector.polar(collideLength, player.angle));

        mapX = (int) (collider.x / gridSize);
        mapY = (int) (collider.y / gridSize);
        boolean collides = collidesWithWall(mapX, mapY);

        if (inMap(mapX, mapY) && map[mapY][mapX] == 2 && player.signalE) { // open door
            map[mapY][mapX] = 0;
        }

        return collides;
    }

    public boolean collidesWithWallBack() {
        double collideLength = 20;
        int mapX, mapY;

        Vector collide = player.p.sub(Vector.polar(collideLength, player.angle));

        mapX = (int) (collide.x / gridSize);
        mapY = (int) (collide.y / gridSize);
        return collidesWithWall(mapX, mapY);
    }

    private boolean collidesWithWall(int mapX, int mapY) {
        return inMap(mapX, mapY) && map[mapY][mapX] > 0;
    }

    private boolean inMap(int mapX, int mapY) {
        return mapY >= 0 && mapY < nGrid && mapX >= 0 && mapX < nGrid;
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
        if (event.isKeyPress(KeyCode.W)) {
            player.forward = true;
        }

        if (event.isKeyRelease(KeyCode.W)) {
            player.forward = false;
        }

        if (event.isKey(KeyCode.S)) {
            player.back = state.keyPressed(KeyCode.S);
        }

        if (event.isKey(KeyCode.A)) {
            player.toRotate = state.keyPressed(KeyCode.A) ? 1 : 0;
        }

        if (event.isKey(KeyCode.D)) {
            player.toRotate = state.keyPressed(KeyCode.D) ? -1 : 0;
        }

        if (event.isKeyPress(KeyCode.C)) {
            twoD = !twoD;
        }

        player.signalE = event.isKeyPress(KeyCode.E);
    }
}
