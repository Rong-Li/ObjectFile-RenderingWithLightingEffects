package client.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import Lighting.Light;
import Lighting.Lighting;
import client.Clipper;
import geometry.*;
import geometry.Point3DH;
import line.LineRenderer;
import client.RendererTrio;
import geometry.Transformation;
import polygon.Polygon;
import polygon.PolygonRenderer;
import polygon.Shader;
import shading.FaceShader;
import shading.PixelShader;
import shading.Shaders;
import shading.VertexShader;
import windowing.drawable.DepthCueingDrawable;
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
    private Transformation projectedToScreen;
    private Transformation simplePerspectiveMatrix;
    private Stack<Transformation> matrixStack;

    private LineBasedReader reader;
    private Stack<LineBasedReader> readerStack;

    private Color defaultColor = Color.WHITE;
    public static Color ambientLight = Color.BLACK;

    private Drawable drawable;
    private Drawable depthCueingDrawable;
    private Drawable ZbufferDrawable;

    private LineRenderer lineRenderer;
    private PolygonRenderer filledRenderer;
    private PolygonRenderer wireframeRenderer;
    private Transformation cameraToScreen;
    private Clipper clipper;
    private Shader ambientShader;
    private FaceShader faceshader;
    private PixelShader pixelshader;
    private VertexShader vertexshader;
    private ShaderStyle shaderStyle;
    private Light light;
    private List<Light> listOfLights= new ArrayList<>();
    private Point3DH lightOrigin;
    private double kSpecular = 0.3;
    private double specularExponent = 8;
    private Color lightColor;
    private Halfplane3DH normal;
    private boolean thePolygonhHasnormal = false;
    private boolean lighted = true;

    public enum RenderStyle {
        FILLED,
        WIREFRAME;
    }
    public enum ShaderStyle {
        FLAT,
        GOURAUD,
        PHONG;
    }
    public SimpInterpreter(String filename, Drawable drawable, RendererTrio renderers) {
        this.drawable = drawable;
        this.depthCueingDrawable = drawable;
        this.ZbufferDrawable = drawable;
        this.lineRenderer = renderers.getLineRenderer();
        this.filledRenderer = renderers.getFilledRenderer();
        this.wireframeRenderer = renderers.getWireframeRenderer();
        this.defaultColor = Color.WHITE;
        CTM = Transformation.identity();
        makeWorldToScreenTransform(drawable.getDimensions());

        reader = new LineBasedReader(filename);
        readerStack = new Stack<>();
        renderStyle = RenderStyle.FILLED;
        shaderStyle = ShaderStyle.PHONG;
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
        //worldToScreen.printMatrix();
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
            case "{" :          push();     break;
            case "}" :          pop();      break;
            case "wire" :       wire();     break;
            case "filled" :     filled();   break;
            case "flat" :       flat();   break;
            case "gouraud" :    gouraud();   break;
            case "phong" :      phong();   break;

            case "file" :		interpretFile(tokens);		break;
            case "scale" :		interpretScale(tokens);		break;
            case "translate" :	interpretTranslate(tokens);	break;
            case "rotate" :		interpretRotate(tokens);	break;
            case "line" :		interpretLine(tokens);		break;
            case "polygon" :	interpretPolygon(tokens);	break;
            case "camera" :		interpretCamera(tokens);	break;
            case "surface" :	interpretSurface(tokens);	break;
            case "ambient" :	interpretAmbient(tokens);	break;
            case "depth" :		interpretDepth(tokens);		break;
    		case "obj" :		interpretObj(tokens);		break;
    		case "light" :		interpretLight(tokens);		break;

            default :
                System.err.println("bad input line: " + tokens);
                break;
        }
    }


    private void interpretLight(String[] tokens) {
        double r = cleanNumber(tokens[1]);
        double g = cleanNumber(tokens[2]);
        double b = cleanNumber(tokens[3]);
        Color I_light = new Color(r,g,b);
        double A = cleanNumber(tokens[4]);
        double B = cleanNumber(tokens[5]);

        Transformation vector = new Transformation(4,1);
        vector.set(1,1,lightOrigin.getX());
        vector.set(2,1,lightOrigin.getY());
        vector.set(3,1,lightOrigin.getZ());
        vector.set(4,1,lightOrigin.getW());
        //vector.printMatrix();
        vector = vector.matrixMultiplication(this.CTM);
        //vector.printMatrix();
        Point3DH resultLocation = new Point3DH(vector.get(1,1), vector.get(2,1), vector.get(3,1));

        this.light = new Light(I_light,resultLocation, A, B);
        this.listOfLights.add(light);
    }

    private void interpretObj(String[] tokens) {
        String quotedFilename = tokens[1];
        int length = quotedFilename.length();
        assert quotedFilename.charAt(0) == '"' && quotedFilename.charAt(length-1) == '"';
        String filename = quotedFilename.substring(1, length-1);
        objFile(filename + ".obj");
    }

    private void objFile(String filename) {
        ObjReader objReader = new ObjReader(filename, defaultColor);
        objReader.read();
        objReader.render(this);
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
    private void phong() { this.shaderStyle = ShaderStyle.PHONG; this.lighted = true;}
    private void gouraud() { this.shaderStyle = ShaderStyle.GOURAUD; this.lighted = true;}
    private void flat() { this.shaderStyle = ShaderStyle.FLAT; this.lighted = true;}

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
        CTM = scale.matrixMultiplication(CTM);
        //System.out.println("it is a Scaling Matrix: ");
        //CTM.printMatrix();
    }
    private void interpretTranslate(String[] tokens) {
        double tx = cleanNumber(tokens[1]);
        double ty = cleanNumber(tokens[2]);
        double tz = cleanNumber(tokens[3]);

        Transformation translate = Transformation.translateMatrix(tx,ty,tz);
        CTM = translate.matrixMultiplication(CTM);
        //System.out.println("it is a Translating Matrix: ");
        //CTM.printMatrix();
    }
    private void interpretRotate(String[] tokens) {
        String axisString = tokens[1];
        double angleInDegrees = cleanNumber(tokens[2]);
        Transformation rotate = Transformation.identity();
        if(axisString.equals("Z")){
            //System.out.println("it is a Z");
            rotate = Transformation.rotateAroundZ(angleInDegrees);
        } else if(axisString.equals("Y")){
            //System.out.println("it is a Y");
            rotate = Transformation.rotateAroundY(angleInDegrees);
        } else if(axisString.equals("X")){
            //System.out.println("it is a X");
            rotate = Transformation.rotateAroundX(angleInDegrees);
        }
        //System.out.println("Rotate by " + axisString);
        CTM = rotate.matrixMultiplication(CTM);
        //CTM.printMatrix();
    }
    private static double cleanNumber(String string) {
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
        for (int i = 0; i < 3; i++){
            Transformation vector = Transformation.vertexToVector(vertices[i]);
            vector = vector.matrixMultiplication(this.CTM);
            vertices[i] = new Vertex3D(vector.get(1,1), vector.get(2,1), vector.get(3,1), defaultColor);
            //System.out.println(vertices[i]);
        }
        Polygon polygon = Polygon.makeEnsuringClockwise(vertices);

        RenderPolygon(polygon);
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
        //System.out.println("then****!!!!!!" + result.toString());
        return result;
    }

    public static Point3DH interpretPoint(String[] tokens, int startingIndex) {
        double x = cleanNumber(tokens[startingIndex]);
        double y = cleanNumber(tokens[startingIndex + 1]);
        double z = cleanNumber(tokens[startingIndex + 2]);

        Point3DH result = new Point3DH(x, y, z, 1.0);
        return result;
    }
    public static Point3DH interpretPointWithW(String[] tokens, int startingIndex) {
        double x = cleanNumber(tokens[startingIndex]);
        double y = cleanNumber(tokens[startingIndex + 1]);
        double z = cleanNumber(tokens[startingIndex + 2]);
        double w = cleanNumber(tokens[startingIndex + 3]);
        Transformation vector = new Transformation(4,1);
        vector.set(1,1,x);
        vector.set(2,1,y);
        vector.set(3,1,z);
        vector.set(4,1,w);
        vector = vector.homogeneousTransfer_4X1();

        Point3DH result = new Point3DH(vector.get(1,1), vector.get(2,1), vector.get(3,1), vector.get(4,1));
        return result;
    }
    public static Color interpretColor(String[] tokens, int startingIndex) {
        double r = cleanNumber(tokens[startingIndex]);
        double g = cleanNumber(tokens[startingIndex + 1]);
        double b = cleanNumber(tokens[startingIndex + 2]);

        Color result = new Color(r,g,b);
        //result = result.multiply(ambientLight);
        return result;
    }




    private void interpretCamera(String[] tokens) {

        worldToScreen = CTM.InversedMatrix();
        CTM = CTM.matrixMultiplication(worldToScreen);

        //multiply everything in matrix stack with worldToscreen
        Stack<Transformation> temp = new Stack<>();
        while(!matrixStack.empty()){
            Transformation t = matrixStack.pop();
            t = t.matrixMultiplication(worldToScreen);
            temp.push(t);
        }
        while(!temp.empty()){
            Transformation tt = temp.pop();
            matrixStack.push(tt);
        }


        //Projection part
        double xLow = cleanNumber(tokens[1]);
        double yLow = cleanNumber(tokens[2]);
        double xHigh = cleanNumber(tokens[3]);
        double yHigh = cleanNumber(tokens[4]);
        //set clipper
        double hither = cleanNumber(tokens[5]);
        double yon = cleanNumber(tokens[6]);
        this.lightOrigin = new Point3DH(0, 0, 0);
        this.clipper = new Clipper(hither, yon, xLow, xHigh, yLow, yHigh);
        projectedToScreen = Transformation.identity();
        double scaleSize_X = 650/(xHigh - xLow);
        double scaleSize_Y = 650/(yHigh - yLow);
        double translate_X = 650 - xHigh * 325;
        double translate_Y = 650 - yHigh * 325;
        //scalling
        projectedToScreen.set(1,1,scaleSize_X);
        projectedToScreen.set(2,2,scaleSize_Y);
        //translating
        projectedToScreen.set(1,4,translate_X);
        projectedToScreen.set(2,4,translate_Y);

        if (xHigh != yHigh){
            projectedToScreen.set(2,2,0.5*projectedToScreen.get(2,2));
            projectedToScreen.set(2,4,projectedToScreen.get(2,4)-160);
        }

        //simple matrix

        simplePerspectiveMatrix = Transformation.identity();
        simplePerspectiveMatrix.set(4,4,0);
        simplePerspectiveMatrix.set(4,3,-1);



        this.cameraToScreen = simplePerspectiveMatrix.matrixMultiplication(projectedToScreen);
    }



    private void interpretAmbient(String[] tokens) {
        double r = cleanNumber(tokens[1]);
        double g = cleanNumber(tokens[2]);
        double b = cleanNumber(tokens[3]);
        ambientLight = new Color(r,g,b);

    }


    private void interpretSurface(String[] tokens) {
        double r = cleanNumber(tokens[1]);
        double g = cleanNumber(tokens[2]);
        double b = cleanNumber(tokens[3]);
        Color color = new Color(r,g,b);
        this.defaultColor = color;
//        this.kSpecular = 0.8;
//        this.specularExponent = 64;
        this.kSpecular = cleanNumber(tokens[4]);
        this.specularExponent = cleanNumber(tokens[5]);
    }
    private void interpretDepth(String[] tokens) {
        double near = cleanNumber(tokens[1]);
        double far = cleanNumber(tokens[2]);
        double r = cleanNumber(tokens[3]);
        double g = cleanNumber(tokens[4]);
        double b = cleanNumber(tokens[5]);
        Color color = new Color(r,g,b);
        depthCueingDrawable = new DepthCueingDrawable(this.drawable, (int)Math.round(near), (int)Math.round(far), color);
        this.drawable = depthCueingDrawable;
    }

    public Vertex3D transformToPerspective(Vertex3D vertex){
        Transformation vector = Transformation.vertexToVector(vertex);

        double z_toKeep = vector.get(3,1);
        vector = vector.matrixMultiplication(simplePerspectiveMatrix);
        vector = vector.homogeneousTransfer_4X1();

        Vertex3D result = new Vertex3D(vector.get(1,1), vector.get(2,1), 1/z_toKeep, vertex.getColor());
        if(vertex.isHasNormal()){
            result.setHasNormal(true);
            result.setNormal(vertex.getNormal());
        }
        return result;

    }
    public Vertex3D transformToCamera(Vertex3D vertex) {
        Transformation vector = Transformation.vertexToVector(vertex);


        vector = vector.matrixMultiplication(projectedToScreen);

        Vertex3D result = new Vertex3D(vector.get(1,1), vector.get(2,1), vector.get(3,1), vertex.getColor());
        result.setCameraPoint(vertex.getCameraPoint());
        if(vertex.isHasNormal()){
            result.setHasNormal(true);
            result.setNormal(vertex.getNormal());
        }
        return result;
    }



    public void RenderPolygon(Polygon polygon){
        Point3DH centerPoint3DH = centerPointofPolygon(polygon);
        //System.out.println(polygon.get(0).getNormal());
        Vertex3D centerPoint = new Vertex3D(centerPoint3DH, defaultColor);
        Lighting lighting = new Lighting(this.listOfLights, ambientLight);
        normal = new Halfplane3DH(polygon);
        int sum = 0;
        for (int i = 0; i < polygon.length(); i++){
            if (polygon.get(i).isHasNormal() == true){
                sum++;
            }
        }
        if (sum == polygon.length()){
            this.thePolygonhHasnormal = true;
        }





        if(shaderStyle == ShaderStyle.FLAT){
            faceshader = fShaderPolygon ->{
                if (thePolygonhHasnormal == true){
                    Point3DH p1 = polygon.get(0).getNormal();
                    Point3DH p2 = polygon.get(1).getNormal();
                    Point3DH p3 = polygon.get(2).getNormal();
                    Transformation n = new Transformation(3,1);
                    n.set(1,1,(p1.getX() + p2.getX() + p3.getX())/3);
                    n.set(2,1,(p1.getY() + p2.getY() + p3.getY())/3);
                    n.set(3,1,(p1.getZ() + p2.getZ() + p3.getZ())/3);
                    normal = new Halfplane3DH(n);
                }
                else {
                    normal = new Halfplane3DH(polygon);
                }


                lightColor = lighting.light(centerPoint, defaultColor, normal, kSpecular, specularExponent);
                Polygon result = fShaderPolygon;
                result.setLightColor(lightColor);
                return result;
            };

            vertexshader = (vShaderPolygon, vShaderVertex) ->{
                vShaderVertex.setNormal(normal.getPlaneNormal());
                return vShaderVertex;
            };

            pixelshader = (pShaderPolygon, pShaderVertex) ->{
                return pShaderPolygon.getLightColor();
            };
        }





        //GROURAUD
        else if(shaderStyle == ShaderStyle.GOURAUD){
            faceshader = fShaderPolygon ->{
                normal = new Halfplane3DH(polygon);
                return fShaderPolygon;
            };

            vertexshader = (vShaderPolygon, vShaderVertex) ->{
                if (vShaderVertex.isHasNormal() == true){
                    normal = new Halfplane3DH(vShaderVertex.getNormal());
                    Vertex3D cameraspaceVertex = new Vertex3D(vShaderVertex.getCameraPoint(), vShaderVertex.getColor());
                    lightColor = lighting.light(cameraspaceVertex, polygon.get(0).getColor(), normal, kSpecular, specularExponent);
                    Vertex3D result = new Vertex3D(vShaderVertex.getPoint3D(), vShaderVertex.getColor().multiply(lightColor));
                    result.setCameraPoint(vShaderVertex.getCameraPoint());
                    result.setHasNormal(true);
                    result.setNormal(vShaderVertex.getNormal());
                    return result;
                }
                else{
                    Vertex3D cameraspaceVertex = new Vertex3D(vShaderVertex.getCameraPoint(), vShaderVertex.getColor());
                    lightColor = lighting.light(cameraspaceVertex, polygon.get(0).getColor(), normal, kSpecular, specularExponent);
                    Vertex3D result = new Vertex3D(vShaderVertex.getPoint3D(), vShaderVertex.getColor().multiply(lightColor));
                    result.setCameraPoint(vShaderVertex.getCameraPoint());
                    result.setNormal(normal.getPlaneNormal());
                    return result;
                }
            };

            pixelshader = (pShaderPolygon, pShaderVertex) ->{
                return pShaderPolygon.getLightColor();
            };
        }




        //PHONG
        else if(shaderStyle == ShaderStyle.PHONG){
            faceshader = fShaderPolygon ->{
                normal = new Halfplane3DH(polygon);
                return fShaderPolygon;
            };

            vertexshader = (vShaderPolygon, vShaderVertex) ->{
                if (vShaderVertex.isHasNormal() == true){
                    return vShaderVertex;
                }
                else{
                    //vShaderVertex.setHasNormal(true);
                    vShaderVertex.setNormal(normal.getPlaneNormal());
                    return vShaderVertex;
                }
            };

            pixelshader = (pShaderPolygon, pShaderVertex) ->{
                Halfplane3DH theNormal = new Halfplane3DH(pShaderVertex.getNormal());
                Vertex3D cameraspaceVertex = new Vertex3D(pShaderVertex.getCameraPoint(), pShaderVertex.getColor());
                lightColor = lighting.light(cameraspaceVertex, pShaderVertex.getColor(), theNormal, kSpecular, specularExponent);
                return lightColor;
            };
        }




        //clip
        List<Vertex3D> array_clippedZ= this.clipper.clipZ_toVertexArray(polygon);




        for (int i = 0; i < array_clippedZ.size(); i++){
            //System.out.println("True Camera: " + array_clippedZ.get(i).getPoint3D());
            Vertex3D temp = transformToPerspective(array_clippedZ.get(i));
            //System.out.println("after perspective: " + temp.getPoint3D());
            array_clippedZ.set(i,temp);
        }
        if(array_clippedZ.size() == 0){
            return;
        }
        List<Vertex3D> array_clippedX = this.clipper.clipX_toVertexArray(array_clippedZ);

        if(array_clippedX.size() == 0){
            return;
        }
        List<Vertex3D> array_clippedY = this.clipper.clipY_toVertexArray(array_clippedX);
        if(array_clippedY.size() == 0){
            return;
        }

        for (int i = 0; i < array_clippedY.size(); i++){
            double cameraspaceZ = array_clippedY.get(i).getZ();
            double cameraspaceX = array_clippedY.get(i).getX() * (-1/cameraspaceZ);
            double cameraspaceY = array_clippedY.get(i).getY() * (-1/cameraspaceZ);
            Point3DH cameraSpace = new Point3DH(cameraspaceX,cameraspaceY,1/cameraspaceZ);
            array_clippedY.get(i).setCameraPoint(cameraSpace);


            Vertex3D temp = transformToCamera(array_clippedY.get(i));
            if(!temp.isHasNormal()){
                temp.setNormal(normal.getPlaneNormal());
            }
            //System.out.println("cameraspaced: " + temp.getCameraPoint());


            array_clippedY.set(i,temp);
        }
        //System.out.println("");



        Vertex3D[] result = new Vertex3D[array_clippedY.size()];
        Polygon finalPolygon = Polygon.makeEnsuringClockwise(array_clippedY.toArray(result));


        if(this.renderStyle == RenderStyle.FILLED){
            List<Polygon> listOfPolygons = Clipper.Triangulation(finalPolygon);
            for (int i = 0; i < listOfPolygons.size(); i++){
                filledRenderer.drawPolygon(listOfPolygons.get(i),drawable, faceshader, vertexshader, pixelshader, lighted);
            }
        }
        else if(this.renderStyle == RenderStyle.WIREFRAME){
            wireframeRenderer.drawPolygon(finalPolygon,ZbufferDrawable, faceshader, vertexshader, pixelshader, lighted);
        }
    }


    public Transformation getCTM() {
        return CTM;
    }

    public Drawable getDrawable() {
        return drawable;
    }
    //limitation of being an triangular.
    public Point3DH centerPointofPolygon(Polygon polygon){
        double centerX = 0;
        double centerY = 0;
        double centerZ = 0;
        double edges = polygon.length();
        for (int i = 0; i < edges; i++){
            centerX = centerX + polygon.get(i).getX();
            centerY = centerY + polygon.get(i).getY();
            centerZ = centerZ + polygon.get(i).getZ();
        }

        Point3DH result = new Point3DH(centerX/edges,centerY/edges,centerZ/edges);

        return result;
    }
}
