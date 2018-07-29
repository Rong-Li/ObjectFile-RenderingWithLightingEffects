package Lighting;

import geometry.Halfplane3DH;
import geometry.Point3DH;
import geometry.Transformation;
import geometry.Vertex3D;
import polygon.Polygon;
import windowing.graphics.Color;

public class Lighting {
    private double kSpecular = 0.3;
    private double specularExponent = 8;
    private Light light;
    private Polygon polygon;
    private Color Ia;



    public Lighting(Light light, Polygon polygon, Color ambient){
        this.light = light;
        this.polygon = polygon;
        this.Ia = ambient;
    }

    public Color light(Vertex3D cameraSpacePoint, Color kDiffuse, Halfplane3DH normal, double kSpecular, double specularExponent) {
        //initialize variables for lighting formula

        Color Kd = kDiffuse;
        // Color Ia from constructor
        Color Ii = this.light.getIntensity();
        double fatti = getfatti(this.light, cameraSpacePoint);
        Transformation unitNormal = normal.getNormalVector().normalizeVector();



    }

    public double getfatti(Light light, Vertex3D point){
        double result = distanceBetween2Points(light.getCameraSpaceLocation(), point.getPoint3D());
        return result;
    }

    public double distanceBetween2Points(Point3DH p1, Point3DH p2){
        double deltaX = p2.getX() - p1.getX();
        double deltaY = p2.getY() - p1.getY();
        double deltaZ = p2.getX() - p1.getX();

        double temp = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        double result = Math.sqrt(temp);

        return result;
    }
}
