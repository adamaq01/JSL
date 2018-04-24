package fr.adamaq01.jsl.tests;

import static org.joml.Math.sin;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import fr.adamaq01.jsl.JSL.In;
import fr.adamaq01.jsl.JSL.Layout;
import fr.adamaq01.jsl.JSL.Uniform;
import fr.adamaq01.jsl.shaders.FragmentShader;


public class JslTestShader extends FragmentShader {
    
    @In
    @Layout(location = 1, index = 2)
    protected Vector3f attributePosition;

    @Uniform
    protected Matrix4f model;
    
    @Uniform
    protected Matrix4f view;

    @Uniform
    protected Matrix4f projection;
    
    @Uniform
    protected double time;
    
    public JslTestShader() {
        super(330);
    }

    @Override
    public void main() {
        gl_FragCoord = foo(attributePosition);
        gl_FragCoord.x = (float) sin(time);
    }
    
    public Vector4f foo(Vector3f coords) {
        return model.transform(view.transform(projection.transform(new Vector4f(coords, 1.0f))));
    }
}
