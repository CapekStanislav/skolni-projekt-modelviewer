package modelviewer;

import global.AbstractRenderer;
import global.GLCamera;
import lwjglutils.OBJLoader;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.Vec3D;

import java.util.ArrayList;
import java.util.List;

import static global.GluUtils.gluPerspective;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Shows rendering of a skybox in a scene
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2020-01-20
 */
public class Renderer extends AbstractRenderer {
    private float dx, dy, ox, oy;
    private float zenit, azimut;

    private float trans, deltaTrans = 0;

    private float uhel = 0;

    private boolean per = true, move = false;
    private GLCamera camera;
    private List<OBJLoader.Model> models = new ArrayList<>();
    private boolean isLight0 = true;
    private boolean isLight1 = true;
    private boolean isLight2 = true;
    private boolean isTextured = true;
    private int actual = 0;
    private boolean mouseLocked = false;

    long fps;
    long oldmils;

    public Renderer() {
        super();
        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    // We will detect this in our rendering loop
                    glfwSetWindowShouldClose(window, true);
                if (action == GLFW_RELEASE) {
                    trans = 0;
                    deltaTrans = 0;
                }

                if (action == GLFW_PRESS) {
                    switch (key) {
                        case GLFW_KEY_P:
                            per = !per;
                            break;
                        case GLFW_KEY_M:
                            move = !move;
                            break;
                        case GLFW_KEY_W:
                        case GLFW_KEY_S:
                        case GLFW_KEY_A:
                        case GLFW_KEY_D:
                            deltaTrans = 0.005f;
                            break;

                        case GLFW_KEY_1:
                            isLight0 = !isLight0;
                            break;
                        case GLFW_KEY_2:
                            isLight1 = !isLight1;
                            break;
                        case GLFW_KEY_3:
                            isLight2 = !isLight2;
                            break;
                        case GLFW_KEY_T:
                            isTextured = !isTextured;
                            break;
                        case GLFW_KEY_SPACE:
                            actual = ++actual % models.size();
                            break;

                    }
                }
                switch (key) {
                    case GLFW_KEY_W:
                        camera.forward(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_S:
                        camera.backward(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_A:
                        camera.left(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_D:
                        camera.right(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;
                }
            }
        };

        glfwMouseButtonCallback = new GLFWMouseButtonCallback() {

            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button == GLFW_MOUSE_BUTTON_1 && !mouseLocked) {
                    final int xpos = width / 2;
                    final int ypos = height / 2;
                    glfwSetCursorPos(window, xpos, ypos);
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                    ox = xpos;
                    oy = ypos;
                    mouseLocked = !mouseLocked;
                } else if (button == GLFW_MOUSE_BUTTON_2 && mouseLocked) {
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    mouseLocked = !mouseLocked;
                }
            }

        };

        glfwCursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                if (mouseLocked) {
                    dx = (float) x - ox;
                    dy = (float) y - oy;
                    ox = (float) x;
                    oy = (float) y;
                    zenit -= dy / width * 180;
                    if (zenit > 90)
                        zenit = 90;
                    if (zenit <= -90)
                        zenit = -90;
                    azimut += dx / height * 180;
                    azimut = azimut % 360;
                    camera.setAzimuth(Math.toRadians(azimut));
                    camera.setZenith(Math.toRadians(zenit));
                    dx = 0;
                    dy = 0;
                }
            }
        };

        glfwScrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double dx, double dy) {
                //do nothing
            }
        };
    }

    @Override
    public void init() {
        super.init();
        glClearColor(0.25f, 0.25f, 0.25f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);
        glFrontFace(GL_CW);
        glPolygonMode(GL_FRONT, GL_FILL);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Loading external models
        System.out.println("Loading objects...");
        loadModels();
        System.out.println("#" + models.size() + " objects loaded");

        // nastavení textur
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        camera = new GLCamera();
        camera.setPosition(new Vec3D(8));
        azimut = -43;
        zenit = -33;
        camera.setAzimuth(Math.toRadians(azimut));
        camera.setZenith(Math.toRadians(zenit));
        camera.setFirstPerson(true);

        settingLightsAndMaterials();
    }

    private void loadModels() {
        // loading models with custom OBJLoader
        final OBJLoader.Model cubeTex = OBJLoader.loadModel("models/cubeTex/cubeTex.obj");
        final OBJLoader.Model monkey = OBJLoader.loadModel("models/monkey/monkey.obj");
        final OBJLoader.Model station = OBJLoader.loadModel("models/station/station.obj");
        final OBJLoader.Model rex = OBJLoader.loadModel("models/rex/rex.obj");
        final OBJLoader.Model reaper1 = OBJLoader.loadModel("models/reaper/reaper_1.obj");
        final OBJLoader.Model reaper2 = OBJLoader.loadModel("models/reaper/reaper_2.obj");
        models.add(cubeTex);
        models.add(monkey);
        models.add(station);
        models.add(rex);

        // joining two models
        reaper2.addPart(reaper1);
        models.add(reaper2);
    }

    private void settingLightsAndMaterials() {
        // key light
        setLight(GL_LIGHT0, 255, 255, 225, 0.1f, 0.95f, 0.1f);
        // back light
        setLight(GL_LIGHT1, 185, 191, 215, 0.05f, 0.50f, 0.01f);
        // fill light
        setLight(GL_LIGHT2, 255, 255, 255, 0f, 0.25f, 0.01f);


    }

    /**
     * @param light
     * @param r     red
     * @param g     green
     * @param b     blue
     * @param ai    portion of ambient
     * @param di    portion of diffuse
     * @param si    portion of specular
     */
    private void setLight(int light, int r, int g, int b, float ai, float di, float si) {
        float p = 1 / (float) 255;
        float rf = r * p;
        float gf = g * p;
        float bf = b * p;

        // light source setting - diffuse component
        float[] light_dif = new float[]{rf * di, gf * di, bf * di, 1};
        // light source setting - ambient component
        float[] light_amb = new float[]{rf * ai, gf * ai, bf * ai, 1};
        // light source setting - specular component
        float[] light_spec = new float[]{rf * si, gf * si, bf * si, 1};

        glLightfv(light, GL_AMBIENT, light_amb);
        glLightfv(light, GL_DIFFUSE, light_dif);
        glLightfv(light, GL_SPECULAR, light_spec);

    }

    private void drawAxis() {
        glBegin(GL_LINES);
        glColor3f(1, 0, 0);
        glVertex3f(0, 0, 0);
        glVertex3f(5, 0, 0);

        glColor3f(0, 1, 0);
        glVertex3f(0, 0, 0);
        glVertex3f(0, 5, 0);

        glColor3f(0, 0, 1);
        glVertex3f(0, 0, 0);
        glVertex3f(0, 0, 5);
        glEnd();
    }

    @Override
    public void display() {
        //výpočet fps
        long mils = System.currentTimeMillis();
        fps = 1000 / (mils - oldmils);
        oldmils = mils;

        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        String text = this.getClass().getName() + ": " + (mouseLocked ? "[rmb] unlock " : "[lmb] lock ");
        String lightInfo = "Lights: ";

        trans += deltaTrans;

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        if (per)
            gluPerspective(45, width / (float) height, 0.1f, 500.0f);
        else
            glOrtho(-20 * width / (float) height,
                    20 * width / (float) height,
                    -20, 20, 0.1f, 500.0f);

        if (move) {
            uhel++;
        }

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glEnable(GL_LIGHTING);

        if (isLight0) {
            glEnable(GL_LIGHT0);
        }
        if (isLight1) {
            glEnable(GL_LIGHT1);
        }
        if (isLight2) {
            glEnable(GL_LIGHT2);
        }

        glPushMatrix();
        camera.setMatrix();
        float[] keyPos = new float[]{2, 8, 10, 0};
        float[] backPos = new float[]{-5, 8, -1, 1};
        float[] fillPos = new float[]{10, 5, -1, 1};
        glLightfv(GL_LIGHT0, GL_POSITION, keyPos);
        glLightfv(GL_LIGHT1, GL_POSITION, backPos);
        glLightfv(GL_LIGHT2, GL_POSITION, fillPos);

        glDisable(GL_LIGHTING);
        drawAxis();
        glEnable(GL_LIGHTING);

        glRotatef(uhel, 0, 1, 0);

        // rendering actual model
        final OBJLoader.Model model = models.get(actual);
        model.setTextureEnable(isTextured);
        model.draw();

        glCallList(1);
        glPopMatrix();

        glDisable(GL_LIGHT2);
        glDisable(GL_LIGHT1);
        glDisable(GL_LIGHT0);
        glDisable(GL_LIGHTING);

        text += per ? ", [P]ersp " : ", [p]ersp ";
        text += move ? ", Ani[M] " : ", Ani[m] ";
        text += isTextured ? "[T]exture, " : "[t]exture, ";
        text += "Space to switch model: " + (actual + 1);

        String textInfo = "position " + camera.getPosition().toString();
        textInfo += String.format(" azimuth %3.1f, zenith %3.1f", azimut, zenit);

        lightInfo += "[1] " + (isLight0 ? "on " : "off ");
        lightInfo += "[2] " + (isLight1 ? "on " : "off ");
        lightInfo += "[3] " + (isLight2 ? "on " : "off ");
        lightInfo += "FPS: " + fps;

        //create and draw text
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(3, 40, textInfo);
        textRenderer.addStr2D(3, 60, lightInfo);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
