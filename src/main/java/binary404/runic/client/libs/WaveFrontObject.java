package binary404.runic.client.libs;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import javafx.geometry.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.CallbackI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaveFrontObject implements IModelCustom {

    private static Pattern vertexPattern = Pattern.compile("(v( (\\-){0,1}\\d+\\.\\d+){3,4} *\\n)|(v( (\\-){0,1}\\d+\\.\\d+){3,4} *$)");
    private static Pattern vertexNormalPattern = Pattern.compile("(vn( (\\-){0,1}\\d+\\.\\d+){3,4} *\\n)|(vn( (\\-){0,1}\\d+\\.\\d+){3,4} *$)");
    private static Pattern textureCoordinatePattern = Pattern.compile("(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *\\n)|(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *$)");
    private static Pattern face_V_VT_VN_Pattern = Pattern.compile("(f( \\d+/\\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+/\\d+){3,4} *$)");
    private static Pattern face_V_VT_Pattern = Pattern.compile("(f( \\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+){3,4} *$)");
    private static Pattern face_V_VN_Pattern = Pattern.compile("(f( \\d+//\\d+){3,4} *\\n)|(f( \\d+//\\d+){3,4} *$)");
    private static Pattern face_V_Pattern = Pattern.compile("(f( \\d+){3,4} *\\n)|(f( \\d+){3,4} *$)");
    private static Matcher vertexMatcher;
    private static Matcher vertexNormalMatcher;
    private static Matcher textureCoordinateMatcher;
    private static Matcher face_V_VT_VN_Matcher;
    private static Matcher face_V_VT_Matcher;
    private static Matcher face_V_VN_Matcher;
    private static Matcher face_V_Matcher;
    private static Pattern groupObjectPattern = Pattern.compile("([go]( [\\w\\d\\.]+) *\\n)|([go]( [\\w\\d\\.]+) *$)");
    private static Matcher groupObjectMatcher;
    public ArrayList<Vertex> vertices;
    public ArrayList<Vertex> vertexNormals;
    public ArrayList<TextureCoordinate> textureCoordinates;
    public ArrayList<GroupObject> groupObjects;
    private GroupObject currentGroupObject;
    private String fileName;

    public WaveFrontObject(ResourceLocation resource) throws ModelFormatException {
        this.vertices = new ArrayList<>();
        this.vertexNormals = new ArrayList<>();
        this.textureCoordinates = new ArrayList<>();
        this.groupObjects = new ArrayList<>();


        this.fileName = resource.toString();


        try {
            IResource res = Minecraft.getInstance().getResourceManager().getResource(resource);
            loadObjModel(res.getInputStream());
        } catch (IOException e) {

            throw new ModelFormatException("IO Exception reading model format", e);
        }
    }

    public WaveFrontObject(String filename, InputStream inputStream) throws ModelFormatException {
        this.vertices = new ArrayList<>();
        this.vertexNormals = new ArrayList<>();
        this.textureCoordinates = new ArrayList<>();
        this.groupObjects = new ArrayList<>();
        this.fileName = filename;
        loadObjModel(inputStream);
    }


    private void loadObjModel(InputStream inputStream) throws ModelFormatException {
        BufferedReader reader = null;

        String currentLine = null;
        int lineCount = 0;


        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));

            while ((currentLine = reader.readLine()) != null) {

                lineCount++;
                currentLine = currentLine.replaceAll("\\s+", " ").trim();

                if (currentLine.startsWith("#") || currentLine.length() == 0) {
                    continue;
                }

                if (currentLine.startsWith("v ")) {

                    Vertex vertex = parseVertex(currentLine, lineCount);
                    if (vertex != null) {
                        this.vertices.add(vertex);
                    }
                    continue;
                }
                if (currentLine.startsWith("vn ")) {

                    Vertex vertex = parseVertexNormal(currentLine, lineCount);
                    if (vertex != null) {
                        this.vertexNormals.add(vertex);
                    }
                    continue;
                }
                if (currentLine.startsWith("vt ")) {

                    TextureCoordinate textureCoordinate = parseTextureCoordinate(currentLine, lineCount);
                    if (textureCoordinate != null) {
                        this.textureCoordinates.add(textureCoordinate);
                    }
                    continue;
                }
                if (currentLine.startsWith("f ")) {


                    if (this.currentGroupObject == null) {
                        this.currentGroupObject = new GroupObject("Default");
                    }

                    Face face = parseFace(currentLine, lineCount);

                    if (face != null) {
                        this.currentGroupObject.faces.add(face);
                    }
                    continue;
                }
                if ((currentLine.startsWith("g ") | currentLine.startsWith("o "))) {

                    GroupObject group = parseGroupObject(currentLine, lineCount);

                    if (group != null) {
                        if (this.currentGroupObject != null) {
                            this.groupObjects.add(this.currentGroupObject);
                        }
                    }

                    this.currentGroupObject = group;
                }
            }

            this.groupObjects.add(this.currentGroupObject);
        } catch (IOException e) {

            throw new ModelFormatException("IO Exception reading model format", e);
        } finally {


            try {

                reader.close();
            } catch (IOException iOException) {
            }


            try {
                inputStream.close();
            } catch (IOException iOException) {
            }
        }
    }


    public void renderAll() {
        Tessellator tessellator = Tessellator.getInstance();

        if (this.currentGroupObject != null) {

            tessellator.getBuffer().begin(this.currentGroupObject.glDrawingMode, DefaultVertexFormats.POSITION_TEX);
        } else {

            tessellator.getBuffer().begin(4, DefaultVertexFormats.POSITION_TEX);
        }
        tessellateAll(tessellator);

        tessellator.draw();
    }


    public void tessellateAll(Tessellator tessellator) {
        for (GroupObject groupObject : this.groupObjects) {
            groupObject.render(tessellator);
        }
    }


    public void renderOnly(String... groupNames) {
        for (GroupObject groupObject : this.groupObjects) {

            for (String groupName : groupNames) {

                if (groupName.equalsIgnoreCase(groupObject.name)) {
                    groupObject.render();
                }
            }
        }
    }

    public void tessellateOnly(Tessellator tessellator, String... groupNames) {
        for (GroupObject groupObject : this.groupObjects) {

            for (String groupName : groupNames) {

                if (groupName.equalsIgnoreCase(groupObject.name)) {
                    groupObject.render(tessellator);
                }
            }
        }
    }


    public String[] getPartNames() {
        ArrayList<String> l = new ArrayList<>();
        for (GroupObject groupObject : this.groupObjects) {
            l.add(groupObject.name);
        }
        return l.toArray(new String[0]);
    }


    public void renderPart(String partName) {
        for (GroupObject groupObject : this.groupObjects) {

            if (partName.equalsIgnoreCase(groupObject.name)) {
                groupObject.render();
            }
        }
    }

    public void renderPart(String partName, IVertexBuilder buffer) {
        for (GroupObject object : this.groupObjects) {
            if (partName.equalsIgnoreCase(object.name)) {
                object.render(buffer);
            }
        }
    }

    public void tessellatePart(Tessellator tessellator, String partName) {
        for (GroupObject groupObject : this.groupObjects) {

            if (partName.equalsIgnoreCase(groupObject.name)) {
                groupObject.render(tessellator);
            }
        }
    }


    public void renderAllExcept(String... excludedGroupNames) {
        for (GroupObject groupObject : this.groupObjects) {

            boolean skipPart = false;
            for (String excludedGroupName : excludedGroupNames) {

                if (excludedGroupName.equalsIgnoreCase(groupObject.name)) {
                    skipPart = true;
                }
            }
            if (!skipPart) {
                groupObject.render();
            }
        }
    }

    public void tessellateAllExcept(Tessellator tessellator, String... excludedGroupNames) {
        for (GroupObject groupObject : this.groupObjects) {

            boolean exclude = false;
            for (String excludedGroupName : excludedGroupNames) {

                if (excludedGroupName.equalsIgnoreCase(groupObject.name)) {
                    exclude = true;
                }
            }
            if (!exclude) {
                groupObject.render(tessellator);
            }
        }
    }


    private Vertex parseVertex(String line, int lineCount) throws ModelFormatException {
        Vertex vertex = null;

        if (isValidVertexLine(line)) {

            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");


            try {
                if (tokens.length == 2) {
                    return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]));
                }
                if (tokens.length == 3) {
                    return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
                }
            } catch (NumberFormatException e) {

                throw new ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), e);
            }

        } else {

            throw new ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }

        return vertex;
    }


    private Vertex parseVertexNormal(String line, int lineCount) throws ModelFormatException {
        Vertex vertexNormal = null;

        if (isValidVertexNormalLine(line)) {

            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");


            try {
                if (tokens.length == 3) {
                    return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
                }
            } catch (NumberFormatException e) {

                throw new ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), e);
            }

        } else {

            throw new ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }

        return vertexNormal;
    }


    private TextureCoordinate parseTextureCoordinate(String line, int lineCount) throws ModelFormatException {
        TextureCoordinate textureCoordinate = null;

        if (isValidTextureCoordinateLine(line)) {

            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");


            try {
                if (tokens.length == 2)
                    return new TextureCoordinate(Float.parseFloat(tokens[0]), 1.0F - Float.parseFloat(tokens[1]));
                if (tokens.length == 3) {
                    return new TextureCoordinate(Float.parseFloat(tokens[0]), 1.0F - Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
                }
            } catch (NumberFormatException e) {

                throw new ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), e);
            }

        } else {

            throw new ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }

        return textureCoordinate;
    }


    private Face parseFace(String line, int lineCount) throws ModelFormatException {
        Face face = null;

        if (isValidFaceLine(line)) {

            face = new Face();

            String trimmedLine = line.substring(line.indexOf(" ") + 1);
            String[] tokens = trimmedLine.split(" ");
            String[] subTokens = null;

            if (tokens.length == 3) {

                if (this.currentGroupObject.glDrawingMode == -1) {
                    this.currentGroupObject.glDrawingMode = 4;
                } else if (this.currentGroupObject.glDrawingMode != 4) {
                    throw new ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Invalid number of points for face (expected 4, found " + tokens.length + ")");
                }

            } else if (tokens.length == 4) {

                if (this.currentGroupObject.glDrawingMode == -1) {

                    this.currentGroupObject.glDrawingMode = 7;
                } else if (this.currentGroupObject.glDrawingMode != 7) {

                    throw new ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Invalid number of points for face (expected 3, found " + tokens.length + ")");
                }
            }


            if (isValidFace_V_VT_VN_Line(line)) {
                face.vertices = new Vertex[tokens.length];
                face.textureCoordinates = new TextureCoordinate[tokens.length];
                face.vertexNormals = new Vertex[tokens.length];

                for (int i = 0; i < tokens.length; i++) {

                    subTokens = tokens[i].split("/");

                    face.vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    face.textureCoordinates[i] = this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                    face.vertexNormals[i] = this.vertexNormals.get(Integer.parseInt(subTokens[2]) - 1);
                }

                face.faceNormal = face.calculateFaceNormal();

            } else if (isValidFace_V_VT_Line(line)) {
                face.vertices = new Vertex[tokens.length];
                face.textureCoordinates = new TextureCoordinate[tokens.length];

                for (int i = 0; i < tokens.length; i++) {

                    subTokens = tokens[i].split("/");

                    face.vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    face.textureCoordinates[i] = this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                }

                face.faceNormal = face.calculateFaceNormal();

            } else if (isValidFace_V_VN_Line(line)) {
                face.vertices = new Vertex[tokens.length];
                face.vertexNormals = new Vertex[tokens.length];

                for (int i = 0; i < tokens.length; i++) {

                    subTokens = tokens[i].split("//");

                    face.vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    face.vertexNormals[i] = this.vertexNormals.get(Integer.parseInt(subTokens[1]) - 1);
                }

                face.faceNormal = face.calculateFaceNormal();

            } else if (isValidFace_V_Line(line)) {
                face.vertices = new Vertex[tokens.length];

                for (int i = 0; i < tokens.length; i++) {
                    face.vertices[i] = this.vertices.get(Integer.parseInt(tokens[i]) - 1);
                }

                face.faceNormal = face.calculateFaceNormal();
            } else {
                throw new ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
            }

        } else {

            throw new ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }

        return face;
    }


    private GroupObject parseGroupObject(String line, int lineCount) throws ModelFormatException {
        GroupObject group = null;

        if (isValidGroupObjectLine(line)) {

            String trimmedLine = line.substring(line.indexOf(" ") + 1);

            if (trimmedLine.length() > 0) {
                group = new GroupObject(trimmedLine);
            }
        } else {

            throw new ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }

        return group;
    }


    private static boolean isValidVertexLine(String line) {
        if (vertexMatcher != null) {
            vertexMatcher.reset();
        }

        vertexMatcher = vertexPattern.matcher(line);
        return vertexMatcher.matches();
    }


    private static boolean isValidVertexNormalLine(String line) {
        if (vertexNormalMatcher != null) {
            vertexNormalMatcher.reset();
        }

        vertexNormalMatcher = vertexNormalPattern.matcher(line);
        return vertexNormalMatcher.matches();
    }


    private static boolean isValidTextureCoordinateLine(String line) {
        if (textureCoordinateMatcher != null) {
            textureCoordinateMatcher.reset();
        }

        textureCoordinateMatcher = textureCoordinatePattern.matcher(line);
        return textureCoordinateMatcher.matches();
    }


    private static boolean isValidFace_V_VT_VN_Line(String line) {
        if (face_V_VT_VN_Matcher != null) {
            face_V_VT_VN_Matcher.reset();
        }

        face_V_VT_VN_Matcher = face_V_VT_VN_Pattern.matcher(line);
        return face_V_VT_VN_Matcher.matches();
    }


    private static boolean isValidFace_V_VT_Line(String line) {
        if (face_V_VT_Matcher != null) {
            face_V_VT_Matcher.reset();
        }

        face_V_VT_Matcher = face_V_VT_Pattern.matcher(line);
        return face_V_VT_Matcher.matches();
    }


    private static boolean isValidFace_V_VN_Line(String line) {
        if (face_V_VN_Matcher != null) {
            face_V_VN_Matcher.reset();
        }

        face_V_VN_Matcher = face_V_VN_Pattern.matcher(line);
        return face_V_VN_Matcher.matches();
    }


    private static boolean isValidFace_V_Line(String line) {
        if (face_V_Matcher != null) {
            face_V_Matcher.reset();
        }

        face_V_Matcher = face_V_Pattern.matcher(line);
        return face_V_Matcher.matches();
    }


    private static boolean isValidFaceLine(String line) {
        return (isValidFace_V_VT_VN_Line(line) || isValidFace_V_VT_Line(line) || isValidFace_V_VN_Line(line) || isValidFace_V_Line(line));
    }


    private static boolean isValidGroupObjectLine(String line) {
        if (groupObjectMatcher != null) {
            groupObjectMatcher.reset();
        }

        groupObjectMatcher = groupObjectPattern.matcher(line);
        return groupObjectMatcher.matches();
    }


    public String getType() {
        return "obj";
    }


    public class TextureCoordinate {
        public float u;
        public float v;
        public float w;

        public TextureCoordinate(float u, float v) {
            this(u, v, 0.0F);
        }


        public TextureCoordinate(float u, float v, float w) {
            this.u = u;
            this.v = v;
            this.w = w;
        }
    }


    public class Face {
        public Vertex[] vertices;

        public Vertex[] vertexNormals;
        public Vertex faceNormal;
        public TextureCoordinate[] textureCoordinates;

        public void addFaceForRender(Tessellator tessellator) {
            addFaceForRender(tessellator, 5.0E-4F);
        }

        public void addFaceForRender(IVertexBuilder buffer) {
            if (this.faceNormal == null) {
                this.faceNormal = calculateFaceNormal();
            }

            float textureOffset = 5.0E-4F;

            float averageU = 0.0F;
            float averageV = 0.0F;

            if (this.textureCoordinates != null && this.textureCoordinates.length > 0) {

                for (int i = 0; i < this.textureCoordinates.length; i++) {

                    averageU += (this.textureCoordinates[i]).u;
                    averageV += (this.textureCoordinates[i]).v;
                }

                averageU /= this.textureCoordinates.length;
                averageV /= this.textureCoordinates.length;
            }


            for (int i = 0; i < this.vertices.length; i++) {


                if (this.textureCoordinates != null && this.textureCoordinates.length > 0) {

                    float offsetU = textureOffset;
                    float offsetV = textureOffset;

                    if ((this.textureCoordinates[i]).u > averageU) {
                        offsetU = -offsetU;
                    }
                    if ((this.textureCoordinates[i]).v > averageV) {
                        offsetV = -offsetV;
                    }

                    buffer.pos((this.vertices[i]).x, (this.vertices[i]).y, (this.vertices[i]).z).tex(((this.textureCoordinates[i]).u + offsetU), ((this.textureCoordinates[i]).v + offsetV))
                            .normal(this.faceNormal.x, this.faceNormal.y, this.faceNormal.z).endVertex();
                } else {

                    buffer.pos((this.vertices[i]).x, (this.vertices[i]).y, (this.vertices[i]).z).normal(this.faceNormal.x, this.faceNormal.y, this.faceNormal.z).endVertex();
                }
            }
        }

        public void addFaceForRender(Tessellator tessellator, float textureOffset) {
            if (this.faceNormal == null) {
                this.faceNormal = calculateFaceNormal();
            }


            float averageU = 0.0F;
            float averageV = 0.0F;

            if (this.textureCoordinates != null && this.textureCoordinates.length > 0) {

                for (int i = 0; i < this.textureCoordinates.length; i++) {

                    averageU += (this.textureCoordinates[i]).u;
                    averageV += (this.textureCoordinates[i]).v;
                }

                averageU /= this.textureCoordinates.length;
                averageV /= this.textureCoordinates.length;
            }


            for (int i = 0; i < this.vertices.length; i++) {


                if (this.textureCoordinates != null && this.textureCoordinates.length > 0) {

                    float offsetU = textureOffset;
                    float offsetV = textureOffset;

                    if ((this.textureCoordinates[i]).u > averageU) {
                        offsetU = -offsetU;
                    }
                    if ((this.textureCoordinates[i]).v > averageV) {
                        offsetV = -offsetV;
                    }

                    tessellator.getBuffer().pos((this.vertices[i]).x, (this.vertices[i]).y, (this.vertices[i]).z).tex(((this.textureCoordinates[i]).u + offsetU), ((this.textureCoordinates[i]).v + offsetV))
                            .normal(this.faceNormal.x, this.faceNormal.y, this.faceNormal.z).endVertex();
                } else {

                    tessellator.getBuffer().pos((this.vertices[i]).x, (this.vertices[i]).y, (this.vertices[i]).z).normal(this.faceNormal.x, this.faceNormal.y, this.faceNormal.z).endVertex();
                }
            }
        }


        public Vertex calculateFaceNormal() {
            Vec3d v1 = new Vec3d(((this.vertices[1]).x - (this.vertices[0]).x), ((this.vertices[1]).y - (this.vertices[0]).y), ((this.vertices[1]).z - (this.vertices[0]).z));
            Vec3d v2 = new Vec3d(((this.vertices[2]).x - (this.vertices[0]).x), ((this.vertices[2]).y - (this.vertices[0]).y), ((this.vertices[2]).z - (this.vertices[0]).z));
            Vec3d normalVector = null;

            normalVector = v1.crossProduct(v2).normalize();

            return new Vertex((float) normalVector.x, (float) normalVector.y, (float) normalVector.z);
        }
    }


    public class GroupObject {
        public String name;

        public ArrayList<Face> faces;
        public int glDrawingMode;

        public GroupObject() {
            this("");
        }


        public GroupObject(String name) {
            this(name, -1);
        }


        public GroupObject(String name, int glDrawingMode) {
            this.faces = new ArrayList<Face>();
            this.name = name;
            this.glDrawingMode = glDrawingMode;
        }


        public void render() {
            if (this.faces.size() > 0) {
                Tessellator tessellator = Tessellator.getInstance();
                tessellator.getBuffer().begin(this.glDrawingMode, DefaultVertexFormats.POSITION_TEX);
                render(tessellator);
                tessellator.draw();
            }
        }

        public void render(IVertexBuilder buffer) {
            if (this.faces.size() > 0) {
                for (Face face : this.faces) {
                    face.addFaceForRender(buffer);
                }
            }
        }

        public void render(Tessellator tessellator) {
            if (this.faces.size() > 0) {
                for (Face face : this.faces) {
                    face.addFaceForRender(tessellator);
                }
            }
        }
    }

    public class ModelFormatException
            extends RuntimeException {
        private static final long serialVersionUID = 2023547503969671835L;


        public ModelFormatException() {
        }


        public ModelFormatException(String message, Throwable cause) {
            super(message, cause);
        }


        public ModelFormatException(String message) {
            super(message);
        }


        public ModelFormatException(Throwable cause) {
            super(cause);
        }
    }
}
