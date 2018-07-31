package Lighting;

import geometry.Halfplane3DH;
import geometry.Point3DH;
import geometry.Transformation;
import geometry.Vertex3D;
import windowing.graphics.Color;

public class Lighting {

    private Light light;
    private Color Ia;



    public Lighting(Light light, Color ambient){
        this.light = light;
        this.Ia = ambient;
    }

    public Color light(Vertex3D cameraSpacePoint, Color kDiffuse, Halfplane3DH normal, double kSpecular, double specularExponent) {
        //initialize variables for lighting formula

        Color kd = kDiffuse;

        // Color Ia from constructor

        Color Ii = this.light.getIntensity();

        double fatti = getfatti(this.light, cameraSpacePoint);
        //System.out.println(fatti);

        Transformation unitNormal = normal.getNormalVector().normalizeVector();
        //unitNormal.printMatrix();

        Point3DH lightVector = this.light.getCameraSpaceLocation().subtract(cameraSpacePoint.getPoint3D());
        Transformation L = new Transformation(3,1);
        L.set(1,1,lightVector.getX());
        L.set(2,1,lightVector.getY());
        L.set(3,1,lightVector.getZ());
        Transformation unitL = L.normalizeVector();

        double ks = kSpecular;

        Point3DH V = new Point3DH(0,0,-1);
        Point3DH newV = V.subtract(cameraSpacePoint.getPoint3D());
        //Point3DH newV = cameraSpacePoint.getPoint3D().subtract(V);
        Transformation v = new Transformation(3,1);
//        v.set(1,1,newV.getX());
//        v.set(2,1,newV.getY());
//        v.set(3,1,newV.getZ());
        v.set(1,1,0);
        v.set(2,1,0);
        v.set(3,1,-1);
        v = v.normalizeVector();

        double nl = unitNormal.dotProduct(unitL);
        Transformation temp = unitNormal.scale(2*nl);
        Transformation R = temp.substract(unitL);
        //R.printMatrix();
        //Transformation unitR = R;
        Transformation unitR = R.normalizeVector();


        //p from inputs


        //calculate constance that won't change each calculation
        double vr = v.dotProduct(unitR);
        //System.out.println(vr);

        double KsVRp = ks * Math.pow(vr,specularExponent);
        //double KsVRp = ks * vr;
        //System.out.println(KsVRp);



        //get red component
        double red = kd.getR() * Ia.getR() + Ii.getR() * fatti * (kd.getR() * nl + KsVRp);
        double green = kd.getG() * Ia.getG() + Ii.getG() * fatti * (kd.getG() * nl + KsVRp);
        double blue = kd.getB() * Ia.getB() + Ii.getB() * fatti * (kd.getB() * nl + KsVRp);

        Color result = new Color(red,green,blue);
        //System.out.println();
        return result;
    }

    public double getfatti(Light light, Vertex3D point){
        double temp = distanceBetween2Points(light.getCameraSpaceLocation(), point.getPoint3D());
        double result = 1 / (light.getFattA() + light.getFattB() * temp);
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
