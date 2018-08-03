package Lighting;

import geometry.Halfplane3DH;
import geometry.Point3DH;
import geometry.Transformation;
import geometry.Vertex3D;
import windowing.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class Lighting {
    private Color Ia;
    private List<Light> listOfLights = new ArrayList<>();



    public Lighting(List<Light> listOfLights, Color ambient){
        this.Ia = ambient;
        this.listOfLights = listOfLights;
    }

    public Color light(Vertex3D cameraSpacePoint, Color kDiffuse, Halfplane3DH normal, double kSpecular, double specularExponent) {
        //first part
        Color kdIa = kDiffuse.multiply(Ia);
        double ks = kSpecular;


        //sum part
        Color sum = new Color(0,0,0);
        for (int i = 0; i < listOfLights.size(); i++){

            Color Ii = this.listOfLights.get(i).getIntensity();

            double fatti = getfatti(this.listOfLights.get(i), cameraSpacePoint);
            //System.out.println(cameraSpacePoint);

            Transformation unitNormal = normal.getNormalVector().normalizeVector();
            //unitNormal.printMatrix();

            Point3DH lightVector = this.listOfLights.get(i).getCameraSpaceLocation().subtract(cameraSpacePoint.getPoint3D());
            //Point3DH lightVector = cameraSpacePoint.getPoint3D().subtract(this.listOfLights.get(i).getCameraSpaceLocation());
            Transformation L = new Transformation(3,1);
            L.set(1,1,lightVector.getX());
            L.set(2,1,lightVector.getY());
            L.set(3,1,lightVector.getZ());
            Transformation unitL = L.normalizeVector();
            //unitL.printMatrix();



            Point3DH V = new Point3DH(0,0,-1);
            Point3DH newV = V.subtract(cameraSpacePoint.getPoint3D());
            //Point3DH newV = cameraSpacePoint.getPoint3D().subtract(V);
            Transformation v = new Transformation(3,1);
            v.set(1,1,newV.getX());
            v.set(2,1,newV.getY());
            v.set(3,1,newV.getZ());
            Transformation unitv = v.normalizeVector();
            //unitv.printMatrix();

            double nl = unitNormal.dotProduct(unitL);
            if(nl < 0){
                nl = 0;
            }
            //System.out.println(nl);

            //double nl = unitL.dotProduct(unitNormal);
            double doubled_nl = 2 * nl;
            Transformation temp = unitNormal.scale(doubled_nl);
            //temp = temp.normalizeVector();
            Transformation R = temp.substract(unitL);
            //R.printMatrix();
            //Transformation unitR = R;
            Transformation unitR = R.normalizeVector();


            //p from inputs


            //calculate constance that won't change each calculation
            double vr = unitv.dotProduct(unitR);
            //double vr = unitR.dotProduct(unitv);
            //double vr = unitR.dotProduct(v);
            if (vr < 0){
                vr = 0;
            }

            //v.printMatrix();
            //unitR.printMatrix();


            double vr_powered = Math.pow(vr,specularExponent);
            double KsVRp = ks * vr_powered;
            //System.out.println(specularExponent);

            //double KsVRp = ks * vr;
            //unitR.printMatrix();



            //get red component
            double red = Ii.getR() * fatti * (kDiffuse.getR() * nl + KsVRp);
            double green = Ii.getG() * fatti * (kDiffuse.getG() * nl + KsVRp);
            double blue = Ii.getB() * fatti * (kDiffuse.getB() * nl + KsVRp);

            //System.out.println(KsVRp);
            Color color = new Color(red,green,blue);
            sum = sum.add(color);
        }

        Color result = sum.add(kdIa);

        return result;
    }

    public double getfatti(Light light, Vertex3D point){
        double temp = distanceBetween2Points(light.getCameraSpaceLocation(), point.getPoint3D());
        double result = 1 / (light.getFattA() + (light.getFattB() * temp));
        return result;
    }

    public double distanceBetween2Points(Point3DH p1, Point3DH p2){
        double deltaX = p2.getX() - p1.getX();
        double deltaY = p2.getY() - p1.getY();
        double deltaZ = p2.getZ() - p1.getZ();

        double temp = (deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ);
        double result = Math.sqrt(temp);

        return result;
    }
}
