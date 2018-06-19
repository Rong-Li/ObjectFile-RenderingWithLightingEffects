package client.interpreter;

import java.util.Stack;

import geometry.Point3DH;
import geometry.Transformation;
import geometry.Vertex3D;
import line.LineRenderer;
//import notProvided.client.Clipper;
import client.RendererTrio;
import geometry.Transformation;
import polygon.Polygon;
import polygon.PolygonRenderer;
import windowing.drawable.Drawable;
import windowing.graphics.Color;
import windowing.graphics.Dimensions;

public class SimpInterpreter {
    private static final int NUM_TOKENS_FOR_POINT = 3;
    private static final int NUM_TOKENS_FOR_COMMAND = 1;
    private static final int NUM_TOKENS_FOR_COLORED_VERTEX = 6;
    private static final int NUM_TOKENS_FOR_UNCOLORED_VERTEX = 3;
    private static final char COMMENT_CHAR = '#';
    private RenderStyle renderStyle;

    private Transformation CTM;
    private Transformation worldToScreen;
    private Stack<Transformation> matrixStack;

    private static int WORLD_LOW_X = -100;
    private static int WORLD_HIGH_X = 100;
    private static int WORLD_LOW_Y = -100;
    private static int WORLD_HIGH_Y = 100;

    private LineBasedReader reader;
    private Stack<LineBasedReader> readerStack;

    private Color defaultColor = Color.WHITE;
    private Color ambientLight = Color.BLACK;

    private Drawable drawable;
    private Drawable depthCueingDrawable;

    private LineRenderer lineRenderer;
    private PolygonRenderer filledRenderer;
    private PolygonRenderer wireframeRenderer;
    private Transformation cameraToScreen;
    //private Clipper clipper;

    public enum RenderStyle {
        FILLED,
        WIREFRAME;
    }
    public SimpInterpreter(String filename, Drawable drawable, RendererTrio renderers) {
        this.drawable = drawable;
        this.depthCueingDrawable = drawable;
        this.lineRenderer = renderers.getLineRenderer();
        this.filledRenderer = renderers.getFilledRenderer();
        this.wireframeRenderer = renderers.getWireframeRenderer();
        this.defaultColor = Color.WHITE;
        CTM = Transformation.identity();
        makeWorldToScreenTransform(drawable.getDimensions());

        reader = new LineBasedReader(filename);
        readerStack = new Stack<>();
        renderStyle = RenderStyle.FILLED;
        this.matrixStack = new Stack<>();
    }

    private void makeWorldToScreenTransform(Dimensions dimensions) {
        this.worldToScreen = Transformation.identity();
        //scalling
        this.worldToScreen.set(1,1,3.25);
        this.worldToScreen.set(2,2,3.25);
//        this.worldToScreen.set(3,3,3.25);
        //translating
        this.worldToScreen.set(1,4,324);
        this.worldToScreen.set(2,4,324);
        //this.worldToScreen.printMatrix();
        System.out.println("Temporate delete right away");
        worldToScreen.printMatrix();
    }

    public void interpret() {
        while(reader.hasNext() ) {
            String line = reader.next().trim();
            interpretLine(line);
            while(!reader.hasNext()) {
                if(readerStack.isEmpty()) {
                    return;
                }
                else {
                    reader = readerStack.pop();
                }
            }
        }
    }
    public void interpretLine(String line) {
        if(!line.isEmpty() && line.charAt(0) != COMMENT_CHAR) {
            String[] tokens = line.split("[ \t,()]+");
            if(tokens.length != 0) {
                interpretCommand(tokens);
            }
        }
    }
    private void interpretCommand(String[] tokens) {
        switch(tokens[0]) {
            case "{" :      push();   break;
            case "}" :      pop();    break;
            case "wire" :   wire();   break;
            case "filled" : filled(); break;

            case "file" :		interpretFile(tokens);		break;
            case "scale" :		interpretScale(tokens);		break;
            case "translate" :	interpretTranslate(tokens);	break;
            case "rotate" :		interpretRotate(tokens);	break;
            case "line" :		interpretLine(tokens);		break;
            case "polygon" :	interpretPolygon(tokens);	break;
//		case "camera" :		interpretCamera(tokens);	break;
//		case "surface" :	interpretSurface(tokens);	break;
//		case "ambient" :	interpretAmbient(tokens);	break;
//		case "depth" :		interpretDepth(tokens);		break;
//		case "obj" :		interpretObj(tokens);		break;

            default :
                System.err.println("bad input line: " + tokens);
                break;
        }
    }

    private void push() {
        this.matrixStack.push(CTM);
    }
    private void pop() {
        this.CTM = this.matrixStack.pop();
    }
    private void wire() {
        this.renderStyle = RenderStyle.WIREFRAME;
    }
    private void filled() {
        this.renderStyle = RenderStyle.FILLED;
    }

    // this one is complete.
    private void interpretFile(String[] tokens) {
        String quotedFilename = tokens[1];
        int length = quotedFilename.length();
        assert quotedFilename.charAt(0) == '"' && quotedFilename.charAt(length-1) == '"';
        String filename = quotedFilename.substring(1, length-1);
        file(filename + ".simp");
    }
    private void file(String filename) {
        readerStack.push(reader);
        reader = new LineBasedReader(filename);
    }

    //
    private void interpretScale(String[] tokens) {
        double sx = cleanNumber(tokens[1]);
        double sy = cleanNumber(tokens[2]);
        double sz = cleanNumber(tokens[3]);

        Transformation scale = Transformation.scaleMatrix(sx,sy,sz);
        CTM = this.CTM.matrixMultiplication(scale);
        System.out.println("it is a Scaling Matrix: ");
        CTM.printMatrix();
    }
    private void interpretTranslate(String[] tokens) {
        double tx = cleanNumber(tokens[1]);
        double ty = cleanNumber(tokens[2]);
        double tz = cleanNumber(tokens[3]);

        Transformation translate = Transformation.translateMatrix(tx,ty,tz);
        CTM = this.CTM.matrixMultiplication(translate);
        System.out.println("it is a Translating Matrix: ");
        CTM.printMatrix();
    }
    private void interpretRotate(String[] tokens) {
        String axisString = tokens[1];
        double angleInDegrees = cleanNumber(tokens[2]);
        Transformation rotate = Transformation.identity();
        if(axisString.equals("Z")){
            System.out.println("it is a Z");
            rotate = Transformation.rotateAroundZ(angleInDegrees);
        } else if(axisString.equals("Y")){
            System.out.println("it is a Y");
            rotate = Transformation.rotateAroundY(angleInDegrees);
        } else if(axisString.equals("X")){
            System.out.println("it is a X");
            rotate = Transformation.rotateAroundX(angleInDegrees);
        }
        System.out.println("Rotate by " + axisString);
        CTM = this.CTM.matrixMultiplication(rotate);
        CTM.printMatrix();
    }
    private double cleanNumber(String string) {
        return Double.parseDouble(string);
    }

    private enum VertexColors {
        COLORED(NUM_TOKENS_FOR_COLORED_VERTEX),
        UNCOLORED(NUM_TOKENS_FOR_UNCOLORED_VERTEX);

        private int numTokensPerVertex;

        private VertexColors(int numTokensPerVertex) {
            this.numTokensPerVertex = numTokensPerVertex;
        }
        public int numTokensPerVertex() {
            return numTokensPerVertex;
        }
    }
    private void interpretLine(String[] tokens) {
        Vertex3D[] vertices = interpretVertices(tokens, 2, 1);
        lineRenderer.drawLine(vertices[0], vertices[1], this.drawable);

    }
    private void interpretPolygon(String[] tokens) {
        Vertex3D[] vertices = interpretVertices(tokens, 3, 1);
        Polygon polygon = Polygon.makeEnsuringClockwise(vertices);
        filledRenderer.drawPolygon(polygon, this.drawable, null);
    }



    public Vertex3D[] interpretVertices(String[] tokens, int numVertices, int startingIndex) {
        VertexColors vertexColors = verticesAreColored(tokens, numVertices);
        Vertex3D vertices[] = new Vertex3D[numVertices];

        for(int index = 0; index < numVertices; index++) {
            vertices[index] = interpretVertex(tokens, startingIndex + index * vertexColors.numTokensPerVertex(), vertexColors);
        }
        return vertices;
    }
    public VertexColors verticesAreColored(String[] tokens, int numVertices) {
        return hasColoredVertices(tokens, numVertices) ? VertexColors.COLORED :
                VertexColors.UNCOLORED;
    }
    public boolean hasColoredVertices(String[] tokens, int numVertices) {
        return tokens.length == numTokensForCommandWithNVertices(numVertices);
    }
    public int numTokensForCommandWithNVertices(int numVertices) {
        return NUM_TOKENS_FOR_COMMAND + numVertices*(NUM_TOKENS_FOR_COLORED_VERTEX);
    }


    private Vertex3D interpretVertex(String[] tokens, int startingIndex, VertexColors colored) {
        Point3DH point = interpretPoint(tokens, startingIndex);

        Color color = defaultColor;
        if(colored == VertexColors.COLORED) {
            color = interpretColor(tokens, startingIndex + NUM_TOKENS_FOR_POINT);
        }

        Vertex3D result = new Vertex3D(point,color);
        return result;
    }
    public Point3DH interpretPoint(String[] tokens, int startingIndex) {
        double x = cleanNumber(tokens[startingIndex]);
        double y = cleanNumber(tokens[startingIndex + 1]);
        double z = cleanNumber(tokens[startingIndex + 2]);

        Transformation vector = new Transformation(4,1);
        vector.set(1,1, x);
        vector.set(2,1, y);
        vector.set(3,1, z);
        vector.set(4,1,1);

        vector = vector.matrixMultiplication(this.CTM);
        vector.printMatrix();
        System.out.println("Temporate delete right away");
        System.out.println("Temporate delete right away");
        vector = vector.matrixMultiplication(worldToScreen);
        System.out.println("T！！！！！！！！");
        vector.printMatrix();

        Point3DH result = new Point3DH(vector.get(1,1), vector.get(2,1), vector.get(3,1), 1.0);
        return result;
    }
    public Color interpretColor(String[] tokens, int startingIndex) {
        double r = cleanNumber(tokens[startingIndex]);
        double g = cleanNumber(tokens[startingIndex + 1]);
        double b = cleanNumber(tokens[startingIndex + 2]);

        return Color.WHITE;
    }

//    private void line(Vertex3D p1, Vertex3D p2) {
//        Vertex3D screenP1 = transformToCamera(p1);
//        Vertex3D screenP2 = transformToCamera(p2);
//        // TODO: finish this method
//    }
//    private void polygon(Vertex3D p1, Vertex3D p2, Vertex3D p3) {
//        Vertex3D screenP1 = transformToCamera(p1);
//        Vertex3D screenP2 = transformToCamera(p2);
//        Vertex3D screenP3 = transformToCamera(p3);
//        // TODO: finish this method
//    }
//
//    private Vertex3D transformToCamera(Vertex3D vertex) {
//        // TODO: finish this method
//    }







}
