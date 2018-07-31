package geometry;

import polygon.Polygon;

public class Halfplane3DH {
    private Point3DH planeNormal;
    private Transformation normalVector;


    public Point3DH getPlaneNormal() {
        return planeNormal;
    }

    public Transformation getNormalVector() {
        return normalVector;
    }

    public Halfplane3DH(Polygon polygon){
        this.planeNormal = getNormalFromPolygon(polygon);
        this.normalVector = new Transformation(3,1);
        this.normalVector.set(1,1,planeNormal.getX());
        this.normalVector.set(2,1,planeNormal.getY());
        this.normalVector.set(3,1,planeNormal.getZ());
    }

    //if we are using average normal of each vertex
    public Halfplane3DH(Transformation vector){
        this.normalVector = vector;
    }

    public Point3DH getNormalFromPolygon(Polygon polygon){
        Vertex3D a = polygon.get(1).subtract(polygon.get(0));
        Vertex3D b = polygon.get(2).subtract(polygon.get(0));
        Point3DH result = crossProduct(a,b);
        return result;
    }

    private Point3DH crossProduct(Vertex3D a, Vertex3D b) {
        double x = a.getY() * b.getZ() - a.getZ() * b.getY();
        double y = a.getZ() * b.getX() - a.getX() * b.getZ();
        double z = a.getX() * b.getY() - a.getY() * b.getX();

        Point3DH result = new Point3DH(x,y,z);
        return result;
    }
}
