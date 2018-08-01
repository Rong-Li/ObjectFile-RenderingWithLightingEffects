package polygon;

import polygon.Shader;
import shading.FaceShader;
import shading.PixelShader;
import shading.VertexShader;
import windowing.drawable.Drawable;

public interface PolygonRenderer {
	// assumes polygon is ccw.
	public void drawPolygon(Polygon polygon, Drawable drawable, FaceShader faceshader, VertexShader vertexshader, PixelShader pixelshader, boolean lighted);

	default public void drawPolygon(Polygon polygon, Drawable panel) {
		drawPolygon(polygon, panel,  a -> a, (a,b) -> b, (a,b) -> b.getColor(), false);
	};
}
