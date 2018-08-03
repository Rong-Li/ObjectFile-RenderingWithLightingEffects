package Lighting;

import geometry.Point3DH;
import windowing.graphics.Color;

public class Light {
    private Color intensity;
    private Point3DH cameraSpaceLocation;
    private double fattA;
    private double fattB;

    public Light(Color intensity, Point3DH cameraSpaceLocation, double fattA, double fattB) {
        this.intensity = intensity;
        this.cameraSpaceLocation = cameraSpaceLocation;
        this.fattA = fattA;
        this.fattB = fattB;
    }

    //getters
    public Color getIntensity() {
        return intensity;
    }

    public Point3DH getCameraSpaceLocation() {
        return cameraSpaceLocation;
    }

    public double getFattA() {
        return fattA;
    }

    public double getFattB() {
        return fattB;
    }

}
