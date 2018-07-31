package shading;

public class Shaders {
    public FaceShader faceshader;
    public PixelShader pixelshader;
    public VertexShader vertexshader;
    public Boolean Lighted = false;

    public Shaders(){

    }

    public Shaders(FaceShader faceshader, PixelShader pixelshader, VertexShader vertexshader) {
        this.faceshader = faceshader;
        this.pixelshader = pixelshader;
        this.vertexshader = vertexshader;
        this.Lighted = true;
    }

    public FaceShader getFaceshader() {
        return faceshader;
    }

    public PixelShader getPixelshader() {
        return pixelshader;
    }

    public VertexShader getVertexshader() {
        return vertexshader;
    }

    public Boolean getLighted() {
        return Lighted;
    }

    public void setLighted(Boolean lighted) {
        Lighted = lighted;
    }
}
