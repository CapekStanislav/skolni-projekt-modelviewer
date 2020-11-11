package lwjglutils;

import transforms.Vec2D;
import transforms.Vec3D;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

/**
 * Class {@code OBJLoader} is a utility class, which has only one method loadModel().
 * The method returns the nested class {@link Model}. The class Model is a top-level representation
 * of renderable object.
 * <p>
 * Model's topology:
 * <ul>
 *     <li>Model</li>
 *     <ul>
 *         <li>Material</li>
 *         <ul>
 *             <li>Ambient color</li>
 *             <li>Diffuse color</li>
 *             <li>Specular color</li>
 *             <li>Specular exponent</li>
 *             <li>alpha</li>
 *             <li>Ambient map</li>
 *             <li>Diffuse map</li>
 *         </ul>
 *         <li>Faces</li>
 *         <ul>
 *             <li>Vertices</li>
 *             <ul>
 *                 <li>Position</li>
 *                 <li>Texture coordination</li>
 *                 <li>Normal</li>
 *             </ul>
 *         </ul>
 *         <li>Parts</li>
 *         <ul>
 *             <li>Instances of class Model (children)</li>
 *         </ul>
 *     </ul>
 * </ul>
 *
 * @author Stanislav Čapek
 * @version 1.0
 * @see Material
 * @see Face
 * @see Vertex
 * @see Topology
 */
public class OBJLoader {

    /**
     * Private constructor- utility class
     */
    private OBJLoader() {
    }

    /**
     * Tries to load the model from .obj file with the material in .mtl file.
     * <p>
     * Obj and mtl file has to have the same name and placed in the same directory.
     * If mtl file has reference to any texture, It has to be in a relative path
     * from the mtl's file directory.
     *
     * @param modelPath relative path
     * @return new Model
     */
    public static Model loadModel(String modelPath) {
        List<Vec3D> vertexBuffer = new ArrayList<>();
        List<Vec2D> textCoordBuffer = new ArrayList<>();
        List<Vec3D> normalBuffer = new ArrayList<>();
        List<Face> faceBuffer = new ArrayList<>();
        List<Material> materials = new ArrayList<>();

        // loading obj file
        try (final InputStream is = OBJLoader.class.getClassLoader().getResourceAsStream(modelPath)) {

            if (is == null) {
                throw new IOException("File not found");
            }
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            reader.lines().forEach(s -> {
                final String[] strings = s.split("\\s+");
                switch (strings[0]) {
                    case "v":
                        vertexBuffer.add(stringToVec3D(strings));
                        break;
                    case "vt":
                        textCoordBuffer.add(stringToVec2D(strings));
                        break;
                    case "vn":
                        normalBuffer.add(stringToVec3D(strings));
                        break;
                    case "f":
                        List<Vertex> v = new ArrayList<>();
                        Topology topology = Topology.getTopology(strings.length - 1);

                        for (int i = 1; i < strings.length; i++) {
                            final String[] indicies = strings[i].split("/");

                            final Vertex vertex = new Vertex(
                                    vertexBuffer.get(Integer.parseInt(indicies[0]) - 1),
                                    textCoordBuffer.get(Integer.parseInt(indicies[1]) - 1),
                                    normalBuffer.get(Integer.parseInt(indicies[2]) - 1)
                            );
                            v.add(vertex);
                        }
                        faceBuffer.add(new Face(topology, v));
                        break;
                    case "mtllib":
                        materials.addAll(loadMaterials(modelPath, strings[1]));
                        break;
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
//        For this implementation model accepts one material. But method loadMaterials() can
//      parse more materials from .mtl file.
        return new Model(
                materials.size() > 0 ? materials.get(0) : null,
                faceBuffer
        );
    }

    /**
     * Materials from .mtl library
     *
     * @param modelPath model's path
     * @param mtllib    library's name
     * @return list of materials - can be empty
     */
    private static List<Material> loadMaterials(String modelPath, String mtllib) {
        final String mtlPath = modelPath.substring(0, modelPath.lastIndexOf("/") + 1);
        final List<Material> materials = new ArrayList<>();

        // loading mtl file
        try (final InputStream is = OBJLoader.class.getClassLoader().getResourceAsStream(mtlPath + mtllib)) {

            if (is == null) {
                final String[] splitPath = modelPath.split("/");
                throw new RuntimeException("No material for " + splitPath[splitPath.length - 1]);
            }

            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            boolean foundMat = false;

            String name = "";
            Vec3D ambientColor = null; //Ka
            Vec3D diffuseColor = null; //Kd
            Vec3D specularColor = null; //Ks
            float specularExponent = 0f; //Ns
            float alpha = 0f; //d
            OGLTexture2D diffuseMap = null;
            OGLTexture2D ambientMap = null;

            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String[] strings = line.split("\\s+");
                    final String keyword = strings[0];
                    if (keyword.equals("newmtl")) {
                        if (foundMat) {
                            materials.add(
                                    new Material(
                                            name, ambientColor, diffuseColor,
                                            specularColor, specularExponent, alpha,
                                            ambientMap, diffuseMap
                                    )
                            );
                            // erase data
                            name = strings[1];
                            ambientColor = null;
                            diffuseColor = null;
                            specularColor = null;
                            specularExponent = 0f;
                            alpha = 0f;
                            diffuseMap = null;
                            ambientMap = null;

                        } else {
                            name = strings[1];
                            foundMat = true;
                        }
                    }
                    switch (keyword) {
                        case "Ka":
                            ambientColor = stringToVec3D(strings);
                            break;
                        case "Kd":
                            diffuseColor = stringToVec3D(strings);
                            break;
                        case "Ks":
                            specularColor = stringToVec3D(strings);
                            break;
                        case "Ns":
                            specularExponent = Float.parseFloat(strings[1]);
                            break;
                        case "d":
                            alpha = Float.parseFloat(strings[1]);
                            break;
                        case "map_Ka":
                            ambientMap = loadTexture(mtlPath + strings[1]);
                            break;
                        case "map_Kd":
                            diffuseMap = loadTexture(mtlPath + strings[1]);
                            break;
                    }
                }
                materials.add(
                        new Material(
                                name, ambientColor, diffuseColor,
                                specularColor, specularExponent, alpha,
                                ambientMap, diffuseMap
                        )
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return materials;
    }

    /**
     * Load texture from file.
     *
     * @param path relative to obj file
     * @return texture
     */
    private static OGLTexture2D loadTexture(String path) {
        System.out.println("path = " + path);
        try {
            return new OGLTexture2D(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse data as s string to float values and return vector3D
     *
     * @param data string array
     * @return vector (x,y,z)
     */
    private static Vec3D stringToVec3D(String[] data) {
        return new Vec3D(stringToFloat(data[1]),
                stringToFloat(data[2]),
                stringToFloat(data[3])
        );
    }

    /**
     * Parse data as s string to float values and return vector2D
     *
     * @param data string array
     * @return vector (x,y)
     */
    private static Vec2D stringToVec2D(String[] data) {
        return new Vec2D(
                stringToFloat(data[1]),
                stringToFloat(data[2])
        );
    }

    /**
     * Parse a string to float
     *
     * @param data a string
     * @return float
     */
    private static float stringToFloat(String data) {
        return Float.parseFloat(data);
    }

    /**
     * Topology describes how the model is structured and the connection between points.
     */
    public enum Topology {
        /**
         * Points corresponds to OpenGL GL_POINTS constant
         */
        POINTS(1, GL_POINTS),
        /**
         * Lines corresponds to OpenGL GL_LINES constant
         */
        LINES(2, GL_LINES),
        /**
         * Triangles corresponds to OpenGL GL_TRIANGLES constant
         */
        TRIANGLES(3, GL_TRIANGLES),
        /**
         * Quads corresponds to OpenGL GL_QUADS
         */
        QUADS(4, GL_QUADS);

        /**
         * Number of vertices
         */
        private final int vertices;
        /**
         * OpenGL constant
         */
        private final int openGL;

        Topology(int vertices, int openGLConst) {
            this.vertices = vertices;
            this.openGL = openGLConst;
        }

        public int getVertices() {
            return vertices;
        }

        public int getOpenGL() {
            return openGL;
        }

        /**
         * Returns the corresponding Topology via its a count of vertices
         *
         * @param vertices a count
         * @return match Topology
         */
        public static Topology getTopology(int vertices) {
            return Arrays.stream(values())
                    .filter(topology -> topology.vertices == vertices)
                    .findFirst()
                    .orElseThrow();
        }
    }

    /**
     * Vertex has several informations. It has position, texture coordination,
     * and normal vector.
     */
    public static class Vertex {
        public final Vec3D position;
        public final Vec2D texCoord;
        public final Vec3D normal;

        public Vertex(Vec3D position, Vec2D texCoord, Vec3D normal) {
            this.position = position;
            this.texCoord = texCoord;
            this.normal = normal;
        }

        public Vec3D getPosition() {
            return position;
        }

        public Vec2D getTexCoord() {
            return texCoord;
        }

        public Vec3D getNormal() {
            return normal;
        }

        @Override
        public String toString() {
            return "Vertex{" +
                    "position=" + position +
                    ", texCoord=" + texCoord +
                    ", normal=" + normal +
                    '}';
        }
    }

    /**
     * The Face is defined by its topology. Count vertices in the list should correspond
     * to a count of topology's vertices.
     */
    public static class Face {
        public final Topology topology;
        public final List<Vertex> vertices;

        /**
         * Constructor. Default {@link Topology#TRIANGLES}
         *
         * @param vertices list
         */
        public Face(List<Vertex> vertices) {
            this(Topology.TRIANGLES, vertices);
        }

        /**
         * Constructor
         *
         * @param topology type of geometry
         * @param vertices list of vertices
         */
        public Face(Topology topology, List<Vertex> vertices) {
            this.topology = topology;
            this.vertices = vertices;
        }

        public List<Vertex> getVertices() {
            return vertices;
        }

        @Override
        public String toString() {
            return "Face{" +
                    "vertices=" + vertices +
                    '}';
        }
    }

    /**
     * The material contains key values to draw an object.<br>
     * {@code ambient color}, {@code diffuse color}, {@code specular color},
     * {@code specular exponent}, {@code alpha}.
     */
    public static class Material {

        private final String name;
        private Vec3D ambientColor;
        private Vec3D diffuseColor;
        private Vec3D specularColor;
        private float specularExponent;
        private float alpha;

        private OGLTexture2D ambientTexture;
        private OGLTexture2D diffuseTexture;

        /**
         * Constructor
         *
         * @param name
         * @param ambientColor
         * @param diffuseColor
         * @param specularColor
         * @param specularExponent
         * @param alpha
         * @param ambientTexture
         * @param diffuseTexture
         */
        public Material(
                String name,
                Vec3D ambientColor, Vec3D diffuseColor,
                Vec3D specularColor, float specularExponent,
                float alpha, OGLTexture2D ambientTexture,
                OGLTexture2D diffuseTexture
        ) {
            this.name = name;
            this.ambientColor = ambientColor;
            this.diffuseColor = diffuseColor;
            this.specularColor = specularColor;
            this.specularExponent = mapToRange(specularExponent);
            this.alpha = alpha;
            this.ambientTexture = ambientTexture;
            this.diffuseTexture = diffuseTexture;
        }

        /**
         * Mapping specular exponent value from a range of mtl file to OpenGL's range.
         *
         * @param f value
         * @return mapped value
         */
        private float mapToRange(float f) {
            int a2 = 1000;
            int b2 = 128;
            float r = f * (b2 / (float) a2);
            return r;
        }

        public Vec3D getAmbientColor() {
            return ambientColor;
        }

        public void setAmbientColor(Vec3D ambientColor) {
            this.ambientColor = ambientColor;
        }

        public Vec3D getDiffuseColor() {
            return diffuseColor;
        }

        public void setDiffuseColor(Vec3D diffuseColor) {
            this.diffuseColor = diffuseColor;
        }

        public Vec3D getSpecularColor() {
            return specularColor;
        }

        public void setSpecularColor(Vec3D specularColor) {
            this.specularColor = specularColor;
        }

        public float getSpecularExponent() {
            return specularExponent;
        }

        public void setSpecularExponent(float specularExponent) {
            this.specularExponent = specularExponent;
        }

        public float getAlpha() {
            return alpha;
        }

        public void setAlpha(float alpha) {
            this.alpha = alpha;
        }

        public OGLTexture2D getAmbientTexture() {
            return ambientTexture;
        }

        public void setAmbientTexture(OGLTexture2D ambientTexture) {
            this.ambientTexture = ambientTexture;
        }

        public OGLTexture2D getDiffuseTexture() {
            return diffuseTexture;
        }

        public void setDiffuseTexture(OGLTexture2D diffuseTexture) {
            this.diffuseTexture = diffuseTexture;
        }

        /**
         * Return array of 4 float, where "w" is 1
         *
         * @param vector to convert
         * @return array
         */
        public float[] toFloatArray(Vec3D vector) {
            return new float[]{
                    ((float) vector.getX()),
                    ((float) vector.getY()),
                    ((float) vector.getZ()),
                    1
            };
        }

        @Override
        public String toString() {
            return "Material{" +
                    "name='" + name + '\'' +
                    ", ambientColor=" + ambientColor +
                    ", diffuseColor=" + diffuseColor +
                    ", specularColor=" + specularColor +
                    ", specularExponent=" + specularExponent +
                    ", alpha=" + alpha +
                    ", ambientTexture=" + ambientTexture +
                    ", diffuseTexture=" + diffuseTexture +
                    '}';
        }
    }

    /**
     * The class Model is a top-level representation of renderable object.
     * <p><br>
     * The model acts as a tree structure. One instance may have N children.
     * Any child may have N own children. Calling method draw() on any Model's
     * instance draws this instance and all their children.<br>
     * <p>
     * Beware of cycling your model structure. For example let's have models A, B, and C.
     * A->B->C->A, then after drawing model C it's the loop back to A.
     *<br>
     * <p>
     * Model's topology:
     * <ul>
     *     <li>Model</li>
     *     <ul>
     *         <li>Material</li>
     *         <ul>
     *             <li>Ambient color</li>
     *             <li>Diffuse color</li>
     *             <li>Specular color</li>
     *             <li>Specular exponent</li>
     *             <li>alpha</li>
     *             <li>Ambient map</li>
     *             <li>Diffuse map</li>
     *         </ul>
     *         <li>Faces</li>
     *         <ul>
     *             <li>Vertices</li>
     *             <ul>
     *                 <li>Position</li>
     *                 <li>Texture coordination</li>
     *                 <li>Normal</li>
     *             </ul>
     *         </ul>
     *         <li>Parts</li>
     *         <ul>
     *             <li>Instances of class Model (children)</li>
     *         </ul>
     *     </ul>
     * </ul>
     *
     * @see Material
     * @see Face
     * @see Vertex
     */
    public static class Model {
        private Material material;
        private final List<Face> faceBuffer;
        private boolean textureEnable = true;
        private final Model instance;
        private List<Model> parts = new ArrayList<>();

        /**
         * Constructor
         *
         * @param material   a model's material
         * @param faceBuffer a model's children (can be empty)
         */
        public Model(Material material, List<Face> faceBuffer) {
            this.material = material;
            this.faceBuffer = faceBuffer;
            this.instance = this;
        }

        /**
         * Constructor
         *
         * @param faceBuffer a model's children (can be empty)
         */
        public Model(List<Face> faceBuffer) {
            this(null, faceBuffer);
        }

        /**
         * Return loaded or set material. Can return null.
         *
         * @return an instance's material
         */
        public Material getMaterial() {
            return material;
        }

        /**
         * Set new material to the model
         *
         * @param material new material
         */
        public void setMaterial(Material material) {
            this.material = material;
        }

        /**
         * Check if is a diffuse texture enable for this instance.
         *
         * @return is a texture applied
         */
        public boolean isTextureEnable() {
            return textureEnable;
        }

        /**
         * Apply a texture for this instance and for all their children.
         *
         * @param textureEnable apply a texture
         */
        public void setTextureEnable(boolean textureEnable) {
            this.textureEnable = textureEnable;
            for (Model part : parts) {
                part.textureEnable = textureEnable;
            }
        }

        /**
         * Add a child to a model
         *
         * @param part a child
         * @return success
         */
        public boolean addPart(Model part) {
            return this.parts.add(part);
        }

        /**
         * Remove a child from a model
         *
         * @param part a child to remove
         * @return success
         */
        public boolean removePart(Model part) {
            return this.parts.remove(part);
        }

        /**
         * Draw this instance and all their children
         */
        public void draw() {
            drawParts(this);
            for (Model part : parts) {
                part.draw();
            }
        }

        /**
         * Actual drawing method
         *
         * @param part model
         */
        private void drawParts(Model part) {
            Material material = part.material;
            boolean textureEnable = part.textureEnable;
            final List<Face> faceBuffer = part.faceBuffer;

            if (material != null) {
                if (material.getDiffuseTexture() != null && textureEnable) {
                    glEnable(GL_TEXTURE_2D);
                    material.getDiffuseTexture().bind();
                }
                glMaterialfv(GL_FRONT, GL_AMBIENT, material.toFloatArray(material.getAmbientColor()));
                glMaterialfv(GL_FRONT, GL_DIFFUSE, material.toFloatArray(material.getDiffuseColor()));
                glMaterialfv(GL_FRONT, GL_SPECULAR, material.toFloatArray(material.getSpecularColor()));
                glMaterialf(GL_FRONT, GL_SHININESS, material.getSpecularExponent());
            }

            for (Face face : faceBuffer) {

                glBegin(face.topology.openGL);
                for (Vertex vertex : face.vertices) {
                    glTexCoord2d(
                            vertex.texCoord.getX(),
                            1 - vertex.texCoord.getY()
                    );
                    glNormal3d(
                            vertex.normal.getX(),
                            vertex.normal.getY(),
                            vertex.normal.getZ()
                    );
                    glVertex3d(
                            vertex.position.getX(),
                            vertex.position.getY(),
                            vertex.position.getZ()
                    );
                }
                glEnd();
            }

            if (material != null) {
                if (material.getDiffuseTexture() != null) {
                    glDisable(GL_TEXTURE_2D);
                }
            }
        }
    }
}

