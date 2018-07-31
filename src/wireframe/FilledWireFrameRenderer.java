package wireframe;

import geometry.Vertex3D;
import line.DDALineRenderer;
import line.LineRenderer;
import polygon.Polygon;
import polygon.PolygonRenderer;
import polygon.Shader;
import shading.FaceShader;
import shading.PixelShader;
import shading.VertexShader;
import windowing.drawable.Drawable;

public class FilledWireFrameRenderer implements PolygonRenderer {
    private FilledWireFrameRenderer(){}
    private LineRenderer lineRenderer = DDALineRenderer.make();

    @Override
    public void drawPolygon(Polygon polygon, Drawable drawable, FaceShader faceshader, VertexShader vertexshader, PixelShader pixelshader) {
        if (outofRange(polygon,drawable)){
            return;
        }
        int numberofEdges = polygon.length();
        for (int i = 0; i < numberofEdges; i++){
            lineRenderer.drawLine(polygon.get(i), polygon.get(i+1), drawable);
        }


    }



    public boolean outofRange(Polygon polygon, Drawable panel){
        boolean result = false;
        Vertex3D p1 = polygon.get(0);
        Vertex3D p2 = polygon.get(1);
        Vertex3D p3 = polygon.get(2);
        if (p1.getIntX() > panel.getWidth() && p1.getIntY() > panel.getHeight()
                && p2.getIntX() > panel.getWidth() && p2.getIntY() > panel.getHeight()
                && p3.getIntX() > panel.getWidth() && p3.getIntY() > panel.getHeight()){
            result = true;
        }
        return result;
    }


    public static PolygonRenderer make() {
        return new FilledWireFrameRenderer();
    }
}
