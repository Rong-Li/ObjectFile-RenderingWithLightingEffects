package polygon;

import geometry.Point3DH;
import geometry.Vertex3D;
import shading.FaceShader;
import shading.PixelShader;
import shading.Shaders;
import shading.VertexShader;
import windowing.drawable.Drawable;
import windowing.graphics.Color;

public class FilledPolygonRenderer implements PolygonRenderer {
    private FaceShader faceshader;
    private PixelShader pixelshader;
    private VertexShader vertexshader;
    private Color lightColor;
    private Polygon polygon;
    private boolean lighted;
    private FilledPolygonRenderer() {
    }

    @Override
    public void drawPolygon(Polygon thePolygon, Drawable drawable, FaceShader faceshader, VertexShader vertexshader, PixelShader pixelshader, boolean lighted) {
        this.faceshader = faceshader;
        this.vertexshader = vertexshader;
        this.pixelshader = pixelshader;
        this.polygon = thePolygon;
        this.lighted = lighted;

        //System.out.println(lighted);
        if (lighted){
            polygon = faceshader.shade(thePolygon);
        }

        if(lighted){
            Vertex3D[] p = new Vertex3D[3];
            p[0] = vertexshader.shade(polygon, polygon.get(0));
            p[1] = vertexshader.shade(polygon, polygon.get(1));
            p[2] = vertexshader.shade(polygon, polygon.get(2));
            Color maintainedPolygonColor = polygon.getLightColor();
            polygon = Polygon.makeEnsuringClockwise(p);
            polygon.setLightColor(maintainedPolygonColor);
        }




        if (outofRange(polygon, drawable)) {
            return;
        }
        if (polygon.get(0).getY() == polygon.get(1).getY()
                && polygon.get(1).getY() == polygon.get(2).getY()) {
            return;
        }
        if (polygon.get(0).getX() == polygon.get(1).getX()
                && polygon.get(1).getX() == polygon.get(2).getX()) {
            return;
        }

        Chain left_chain = this.polygon.leftChain();
        Chain right_chain = this.polygon.rightChain();


        Vertex3D p_top = left_chain.get(0);
        Vertex3D p_bottomLeft = left_chain.get(1);
        Vertex3D p_bottomRight = right_chain.get(1);



        // if having horizontal bottom line
        if (left_chain.get(1).getIntY() == right_chain.get(1).getIntY()) {
            Horizontal_Bottom(p_top, p_bottomLeft, p_bottomRight, drawable);
        }
        // if having Non-horizontal bottom line
        //if left edge if shorter
        else if (left_chain.get(1).getIntY() > right_chain.get(1).getIntY()
                && left_chain.get(1).getIntY() != left_chain.get(0).getIntY()
                && right_chain.get(1).getIntY() != left_chain.get(0).getIntY()) {
            //System.out.println("wrong2");

            Non_Horizontal_LeftShort(p_top, p_bottomLeft, p_bottomRight, drawable);
        }
        //if right edge is shorter
        else if (left_chain.get(1).getIntY() < right_chain.get(1).getIntY()
                && left_chain.get(1).getIntY() != left_chain.get(0).getIntY()
                && right_chain.get(1).getIntY() != left_chain.get(0).getIntY()) {
            //System.out.println("wrong3");
            Non_Horizontal_RightShort(p_top, p_bottomLeft, p_bottomRight, drawable);
        }

        //if horizontal top line
        else if (left_chain.get(1).getIntY() == left_chain.get(0).getIntY()
                || right_chain.get(1).getIntY() == right_chain.get(0).getIntY()) {
            //System.out.println("Right");

            // 1st case when the top is at right node
            if (left_chain.get(1).getIntY() == left_chain.get(0).getIntY()) {
                Vertex3D p_topLeft = left_chain.get(1);
                Vertex3D p_topRight = left_chain.get(0);
                Vertex3D p_bottom = right_chain.get(1);

                Horizontal_top(p_topLeft, p_topRight, p_bottom, drawable);
            }
            // 2nd case when the top is at left node
            else if (right_chain.get(1).getIntY() == right_chain.get(0).getIntY()) {
                Vertex3D p_topLeft = left_chain.get(0);
                Vertex3D p_topRight = right_chain.get(1);
                Vertex3D p_bottom = left_chain.get(1);

                Horizontal_top(p_topLeft, p_topRight, p_bottom, drawable);
            }
        }

    }


    private void Non_Horizontal_RightShort(Vertex3D p_top, Vertex3D p_bottomLeft, Vertex3D p_bottomRight, Drawable drawable) {
        Vertex3D p_middle = p_bottomRight;
        //left long edge
        double deltaX1 = p_top.getIntX() - p_bottomLeft.getIntX();
        double deltaY1 = p_top.getIntY() - p_bottomLeft.getIntY();
        Color m1 = DecrementforColors(p_top, p_bottomLeft);
        Point3DH cameraSpaceM1 = DecrementforCameraSpacePoint(p_top, p_bottomLeft);
        Point3DH normalM1 = DecrementforNormals(p_top, p_bottomLeft);
        double z_slope1 = DecrementforZ(p_top, p_bottomLeft);

        //right short top edge
        double deltaX2 = p_bottomRight.getIntX() - p_top.getIntX();
        double deltaY2 = p_bottomRight.getIntY() - p_top.getIntY();
        Color m2 = DecrementforColors(p_top, p_bottomRight);
        Point3DH cameraSpaceM2 = DecrementforCameraSpacePoint(p_top, p_bottomRight);
        Point3DH normalM2 = DecrementforNormals(p_top, p_bottomRight);

        double z_slope2 = DecrementforZ(p_top, p_bottomRight);

        //right short bot edge
        double deltaX2_2 = p_bottomRight.getIntX() - p_bottomLeft.getIntX();
        double deltaY2_2 = p_bottomRight.getIntY() - p_bottomLeft.getIntY();
        Color m2_2 = DecrementforColors(p_bottomRight, p_bottomLeft);
        Point3DH cameraSpaceM2_2 = DecrementforCameraSpacePoint(p_bottomRight, p_bottomLeft);
        Point3DH normalM2_2 = DecrementforNormals(p_bottomRight, p_bottomLeft);
        double z_slope2_2 = DecrementforZ(p_bottomRight, p_bottomLeft);

        double L_slope = deltaX1 / deltaY1;
        double R_slope = deltaX2 / deltaY2;
        double R2_slope = deltaX2_2 / deltaY2_2;

        double start_point = p_top.getIntX();
        double end_point = p_top.getIntX();

        int y = p_top.getIntY();
        Color c1 = p_top.getColor();
        Color c2 = p_top.getColor();
        double z1 = p_top.getZ();
        double z2 = p_top.getZ();
        Point3DH cameraSpace_p1 = p_top.getCameraPoint();
        Point3DH cameraSpace_p2 = p_top.getCameraPoint();
        Point3DH normal_p1 = p_top.getNormal();
        Point3DH normal_p2 = p_top.getNormal();


        //rendering begin
        while (y >= p_bottomLeft.getIntY()) {
            if (y > p_middle.getIntY()) {
                blerping_fillPixels_leftToRight(start_point, end_point, y, c1, c2, drawable, z1, z2, cameraSpace_p1, cameraSpace_p2, normal_p1, normal_p2);
                start_point = start_point - L_slope;
                end_point = end_point - R_slope;
                c1 = c1.subtract(m1);
                c2 = c2.subtract(m2);
                z1 = z1 - z_slope1;
                z2 = z2 - z_slope2;
                cameraSpace_p1 = cameraSpace_p1.subtract(cameraSpaceM1);
                cameraSpace_p2 = cameraSpace_p2.subtract(cameraSpaceM2);
                normal_p1 = normal_p1.subtract(normalM1);
                normal_p2 = normal_p2.subtract(normalM2);
                y--;
            } else {
                blerping_fillPixels_leftToRight(start_point, end_point, y, c1, c2, drawable, z1, z2, cameraSpace_p1, cameraSpace_p2, normal_p1, normal_p2);
                start_point = start_point - L_slope;
                end_point = end_point - R2_slope;
                c1 = c1.subtract(m1);
                c2 = c2.subtract(m2_2);
                z1 = z1 - z_slope1;
                z2 = z2 - z_slope2_2;
                cameraSpace_p1 = cameraSpace_p1.subtract(cameraSpaceM1);
                cameraSpace_p2 = cameraSpace_p2.subtract(cameraSpaceM2_2);
                normal_p1 = normal_p1.subtract(normalM1);
                normal_p2 = normal_p2.subtract(normalM2_2);
                y--;
            }
        }
    }

    private void Non_Horizontal_LeftShort(Vertex3D p_top, Vertex3D p_bottomLeft, Vertex3D p_bottomRight, Drawable drawable) {
        Vertex3D p_middle = p_bottomLeft;

        double deltaX1_1 = p_top.getIntX() - p_bottomLeft.getIntX();
        double deltaY1_1 = p_top.getIntY() - p_bottomLeft.getIntY();
        Color m1_1 = DecrementforColors(p_top, p_bottomLeft);
        Point3DH cameraSpaceM1_1 = DecrementforCameraSpacePoint(p_top, p_bottomLeft);
        Point3DH normalM1_1 = DecrementforNormals(p_top, p_bottomLeft);

        double z_slope1_1 = DecrementforZ(p_top, p_bottomLeft);


        double deltaX1_2 = p_bottomRight.getIntX() - p_bottomLeft.getIntX();
        double deltaY1_2 = p_bottomRight.getIntY() - p_bottomLeft.getIntY();
        Color m1_2 = DecrementforColors(p_bottomRight, p_bottomLeft);
        Point3DH cameraSpaceM1_2 = DecrementforCameraSpacePoint(p_bottomRight, p_bottomLeft);
        Point3DH normalM1_2 = DecrementforNormals(p_bottomRight, p_bottomLeft);

        double z_slope1_2 = DecrementforZ(p_bottomRight, p_bottomLeft);

        double deltaX2 = p_bottomRight.getIntX() - p_top.getIntX();
        double deltaY2 = p_bottomRight.getIntY() - p_top.getIntY();
        Color m2 = DecrementforColors(p_bottomRight, p_top);
        Point3DH cameraSpaceM2 = DecrementforCameraSpacePoint(p_bottomRight, p_top);
        Point3DH normalM2 = DecrementforNormals(p_bottomRight, p_top);
        double z_slope2 = DecrementforZ(p_bottomRight, p_top);


        double L1_slope = deltaX1_1 / deltaY1_1;
        double L2_slope = deltaX1_2 / deltaY1_2;
        double R_slope = deltaX2 / deltaY2;

        double start_point = p_top.getIntX();
        double end_point = p_top.getIntX();


        int y = p_top.getIntY();
        Color c1 = p_top.getColor();
        Color c2 = p_top.getColor();
        double z1 = p_top.getZ();
        double z2 = p_top.getZ();
        Point3DH cameraSpace_p1 = p_top.getCameraPoint();
        Point3DH cameraSpace_p2 = p_top.getCameraPoint();
        Point3DH normal_p1 = p_top.getNormal();
        Point3DH normal_p2 = p_top.getNormal();


        //rendering begin
        while (y >= p_bottomRight.getIntY()) {
            if (y > p_middle.getIntY()) {
                blerping_fillPixels_leftToRight(start_point, end_point, y, c1, c2, drawable, z1, z2, cameraSpace_p1, cameraSpace_p2, normal_p1, normal_p2);
                start_point = start_point - L1_slope;
                end_point = end_point - R_slope;
                c1 = c1.subtract(m1_1);
                c2 = c2.subtract(m2);
                z1 = z1 - z_slope1_1;
                z2 = z2 - z_slope2;
                cameraSpace_p1 = cameraSpace_p1.subtract(cameraSpaceM1_1);
                cameraSpace_p2 = cameraSpace_p2.subtract(cameraSpaceM2);
                normal_p1 = normal_p1.subtract(normalM1_1);
                normal_p2 = normal_p2.subtract(normalM2);
                y--;
            } else {
                blerping_fillPixels_leftToRight(start_point, end_point, y, c1, c2, drawable, z1, z2, cameraSpace_p1, cameraSpace_p2, normal_p1, normal_p2);
                start_point = start_point - L2_slope;
                end_point = end_point - R_slope;
                c1 = c1.subtract(m1_2);
                c2 = c2.subtract(m2);
                z1 = z1 - z_slope1_2;
                z2 = z2 - z_slope2;
                cameraSpace_p1 = cameraSpace_p1.subtract(cameraSpaceM1_2);
                cameraSpace_p2 = cameraSpace_p2.subtract(cameraSpaceM2);
                normal_p1 = normal_p1.subtract(normalM1_2);
                normal_p2 = normal_p2.subtract(normalM2);
                y--;
            }
        }
    }

    private void Horizontal_Bottom(Vertex3D p_top, Vertex3D p_bottomLeft, Vertex3D p_bottomRight, Drawable drawable) {
        double deltaX1 = p_top.getIntX() - p_bottomLeft.getIntX();
        double deltaY1 = p_top.getIntY() - p_bottomLeft.getIntY();
        Color m1 = DecrementforColors(p_top, p_bottomLeft);
        Point3DH cameraSpaceM1 = DecrementforCameraSpacePoint(p_top, p_bottomLeft);
        Point3DH normalM1 = DecrementforNormals(p_top, p_bottomLeft);
        double z_slope1 = DecrementforZ(p_top, p_bottomLeft);

        double deltaX2 = p_top.getIntX() - p_bottomRight.getIntX();
        double deltaY2 = p_top.getIntY() - p_bottomRight.getIntY();
        Color m2 = DecrementforColors(p_top, p_bottomRight);
        Point3DH cameraSpaceM2 = DecrementforCameraSpacePoint(p_top, p_bottomRight);
        Point3DH normalM2 = DecrementforNormals(p_top, p_bottomRight);
        double z_slope2 = DecrementforZ(p_top, p_bottomRight);

        double L_slope = deltaX1 / deltaY1;
        double R_slope = deltaX2 / deltaY2;
        double start_point = p_top.getIntX();
        double end_point = p_top.getIntX();
        int y = p_top.getIntY();
        Color c1 = p_top.getColor();
        Color c2 = p_top.getColor();
        Point3DH cameraSpace_p1 = p_top.getCameraPoint();
        Point3DH cameraSpace_p2 = p_top.getCameraPoint();
        double z1 = p_top.getZ();
        double z2 = p_top.getZ();
        Point3DH normal_p1 = p_top.getNormal();
        Point3DH normal_p2 = p_top.getNormal();

        //rendering begin
        while (y > p_bottomLeft.getIntY()) {
            blerping_fillPixels_leftToRight(start_point, end_point, y, c1, c2, drawable, z1, z2, cameraSpace_p1, cameraSpace_p2, normal_p1, normal_p2);
            start_point = start_point - L_slope;
            end_point = end_point - R_slope;
            c1 = c1.subtract(m1);
            c2 = c2.subtract(m2);
            z1 = z1 - z_slope1;
            z2 = z2 - z_slope2;
            cameraSpace_p1 = cameraSpace_p1.subtract(cameraSpaceM1);
            cameraSpace_p2 = cameraSpace_p2.subtract(cameraSpaceM2);
            normal_p1 = normal_p1.subtract(normalM1);
            normal_p2 = normal_p2.subtract(normalM2);
            y--;
        }
    }

    private void Horizontal_top(Vertex3D p_topLeft, Vertex3D p_topRight, Vertex3D p_bottom, Drawable drawable) {
        double deltaX1 = p_topLeft.getIntX() - p_bottom.getIntX();
        double deltaY1 = p_topLeft.getIntY() - p_bottom.getIntY();
        Color m1 = DecrementforColors(p_topLeft, p_bottom);
        Point3DH cameraSpaceM1 = DecrementforCameraSpacePoint(p_topLeft, p_bottom);
        Point3DH normalM1 = DecrementforNormals(p_topLeft, p_bottom);

        double z_slope1 = DecrementforZ(p_topLeft, p_bottom);


        double deltaX2 = p_topRight.getIntX() - p_bottom.getIntX();
        double deltaY2 = p_topRight.getIntY() - p_bottom.getIntY();
        Color m2 = DecrementforColors(p_topRight, p_bottom);
        Point3DH cameraSpaceM2 = DecrementforCameraSpacePoint(p_topRight, p_bottom);
        Point3DH normalM2 = DecrementforNormals(p_topRight, p_bottom);

        double z_slope2 = DecrementforZ(p_topRight, p_bottom);

        double L_slope = deltaX1 / deltaY1;
        double R_slope = deltaX2 / deltaY2;
        double start_point = p_topLeft.getIntX();
        double end_point = p_topRight.getIntX();
        int y = p_topLeft.getIntY();
        Color c1 = p_topLeft.getColor();
        Color c2 = p_topRight.getColor();
        double z1 = p_topLeft.getZ();
        double z2 = p_topRight.getZ();
        Point3DH cameraSpace_p1 = p_topLeft.getCameraPoint();
        Point3DH cameraSpace_p2 = p_topRight.getCameraPoint();
        Point3DH normal_p1 = p_topLeft.getNormal();
        Point3DH normal_p2 = p_topRight.getNormal();

        //rendering begin
        while (y >= p_bottom.getIntY()) {
            blerping_fillPixels_leftToRight(start_point, end_point, y, c1, c2, drawable, z1, z2, cameraSpace_p1, cameraSpace_p2, normal_p1, normal_p2);
            start_point = start_point - L_slope;
            end_point = end_point - R_slope;
            c1 = c1.subtract(m1);
            c2 = c2.subtract(m2);
            z1 = z1 - z_slope1;
            z2 = z2 - z_slope2;
            y--;
            cameraSpace_p1 = cameraSpace_p1.subtract(cameraSpaceM1);
            cameraSpace_p2 = cameraSpace_p2.subtract(cameraSpaceM2);
            normal_p1 = normal_p1.subtract(normalM1);
            normal_p2 = normal_p2.subtract(normalM2);
        }
    }


    private Point3DH DecrementforNormals(Vertex3D p1, Vertex3D p2){
        double deltaY = p1.getIntY() - p2.getIntY();

        double x1 = p1.getNormal().getX();
        double x2 = p2.getNormal().getX();
        double deltaNormalX = x1 - x2;
        double mx = deltaNormalX / deltaY;

        double y1 = p1.getNormal().getY();
        double y2 = p2.getNormal().getY();
        double deltaNormalY = y1 - y2;
        double my = deltaNormalY / deltaY;

        double z1 = p1.getNormal().getZ();
        double z2 = p2.getNormal().getZ();
        double deltaNormalZ = z1 - z2;
        double mz = deltaNormalZ / deltaY;

        Point3DH result = new Point3DH(mx,my,mz);
        return result;
    }

    private Point3DH DecrementforCameraSpacePoint(Vertex3D p1, Vertex3D p2){
        double deltaY = p1.getIntY() - p2.getIntY();

        double x1 = p1.getCameraPoint().getX();
        double x2 = p2.getCameraPoint().getX();
        double deltaCameraX = x1 - x2;
        double mx = deltaCameraX / deltaY;

        double y1 = p1.getCameraPoint().getY();
        double y2 = p2.getCameraPoint().getY();
        double deltaCameraY = y1 - y2;
        double my = deltaCameraY / deltaY;

        double z1 = p1.getCameraPoint().getZ();
        double z2 = p2.getCameraPoint().getZ();
        double deltaCameraZ = z1 - z2;
        double mz = deltaCameraZ / deltaY;

        Point3DH result = new Point3DH(mx,my,mz);
        return result;
    }


    private Color DecrementforColors(Vertex3D p1, Vertex3D p2) {
        double deltaY = p1.getIntY() - p2.getIntY();

        double r1 = p1.getColor().getR();
        double r2 = p2.getColor().getR();
        double deltaR = r1 - r2;
        double mr = deltaR / deltaY;

        double g1 = p1.getColor().getG();
        double g2 = p2.getColor().getG();
        double deltaG = g1 - g2;
        double mg = deltaG / deltaY;

        double b1 = p1.getColor().getB();
        double b2 = p2.getColor().getB();
        double deltaB = b1 - b2;
        double mb = deltaB / deltaY;

        Color result = new Color(mr, mg, mb);
        return result;
    }

    private double DecrementforZ(Vertex3D p1, Vertex3D p2) {
        double deltaY = p1.getIntY() - p2.getIntY();
        double deltaZ = p1.getZ() - p2.getZ();
        double result = deltaZ / deltaY;
        return result;
    }

    private void blerping_fillPixels_leftToRight(double x_start, double x_end, int y,
                                                 Color c1, Color c2, Drawable drawable,
                                                 double z1, double z2,
                                                 Point3DH cameraSpace_p1, Point3DH cameraSpace_p2,
                                                 Point3DH normal_p1, Point3DH normal_p2) {
        Color newColor = c1;
        double z = z1;
        Point3DH newCameraSpacePoint = cameraSpace_p1;
        Point3DH newNormal = normal_p1;

        double deltaX = x_end - x_start;

        double deltaR = c2.getR() - c1.getR();
        double deltaG = c2.getG() - c1.getG();
        double deltaB = c2.getB() - c1.getB();
        double deltaZ = z2 - z1;
        double m_r = deltaR / deltaX;
        double m_g = deltaG / deltaX;
        double m_b = deltaB / deltaX;
        Color addOn = new Color(m_r, m_g, m_b);


        double z_slope = deltaZ / deltaX;

        double deltaCameraX = cameraSpace_p2.getX() - cameraSpace_p1.getX();
        double deltaCameraY = cameraSpace_p2.getY() - cameraSpace_p1.getY();
        double deltaCameraZ = cameraSpace_p2.getZ() - cameraSpace_p1.getZ();
        double cameraSpaceM_X = deltaCameraX / deltaX;
        double cameraSpaceM_Y = deltaCameraY / deltaX;
        double cameraSpaceM_Z = deltaCameraZ / deltaX;
        Point3DH cameraSpace_addon = new Point3DH(cameraSpaceM_X,cameraSpaceM_Y,cameraSpaceM_Z);

        double deltaNormalX = normal_p2.getX() - normal_p1.getX();
        double deltaNormalY = normal_p2.getY() - normal_p1.getY();
        double deltaNormalZ = normal_p2.getZ() - normal_p1.getZ();
        double normalM_X = deltaNormalX / deltaX;
        double normalM_Y = deltaNormalY / deltaX;
        double normalM_Z = deltaNormalZ / deltaX;
        Point3DH normal_addon = new Point3DH(normalM_X,normalM_Y,normalM_Z);



        int start = (int) Math.round(x_start);
        int end = (int) Math.round(x_end);
        if (start == end) {
            if(start < drawable.getWidth() && y < drawable.getHeight()){
                if(lighted){
                    Vertex3D vertex = new Vertex3D(start,y,1/z, c1);
                    vertex.setCameraPoint(newCameraSpacePoint);
                    vertex.setHasNormal(true);
                    vertex.setNormal(newNormal);
                    this.lightColor = pixelshader.shade(this.polygon,vertex);
                    drawable.setPixel(start, y, 1/z, c1.multiply(lightColor).asARGB());

                    //drawable.setPixel(start, y, 1/z, c1.asARGB());
                }
                else{
                    drawable.setPixel(start, y, 1/z, c1.asARGB());
                }
            }
        }

        else {
            for (int i = start; i < end; i++) {
                if(i < drawable.getWidth() && y < drawable.getHeight()){
                    if(lighted){
                        Vertex3D vertex = new Vertex3D(i,y,1/z, newColor);
                        vertex.setCameraPoint(newCameraSpacePoint);
                        vertex.setHasNormal(true);
                        vertex.setNormal(newNormal);
                        this.lightColor = pixelshader.shade(this.polygon,vertex);
                        drawable.setPixel(i, y, 1/z, newColor.multiply(lightColor).asARGB());
                        //System.out.println(newColor);
                        //drawable.setPixel(i, y, 1/z, newColor.asARGB());
                    }
                    else {
                        drawable.setPixel(i, y, 1/z, newColor.asARGB());
                    }
                }
                newColor = newColor.add(addOn);
                z = z + z_slope;
                newCameraSpacePoint = newCameraSpacePoint.add(cameraSpace_addon);
                newNormal = newNormal.add(normal_addon);
            }
        }
    }


    public static PolygonRenderer make() {
        return new FilledPolygonRenderer();
    }

    public boolean outofRange(Polygon polygon, Drawable panel) {
        boolean result = false;
        Vertex3D p1 = polygon.get(0);
        Vertex3D p2 = polygon.get(1);
        Vertex3D p3 = polygon.get(2);
        if (vertexIsOutSideOfPanel(p1,panel) && vertexIsOutSideOfPanel(p2,panel) && vertexIsOutSideOfPanel(p3,panel)) {
            result = true;
        }
        return result;
    }

    public boolean vertexIsOutSideOfPanel(Vertex3D p, Drawable panel){
        boolean result = false;
        if (p.getIntX() > panel.getWidth() || p.getIntY() > panel.getHeight()){
            result = true;
        }
        return result;
    }
}
