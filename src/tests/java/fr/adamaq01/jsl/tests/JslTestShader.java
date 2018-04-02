package fr.adamaq01.jsl.tests;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import fr.adamaq01.jsl.JSL.In;
import fr.adamaq01.jsl.JSL.Uniform;
import fr.adamaq01.jsl.shaders.FragmentShader;


public class JslTestShader extends FragmentShader {
    
    @In
    protected Vector3f attributePosition;

    @Uniform
    protected Matrix4f model;
    
    @Uniform
    protected Matrix4f view;

    @Uniform
    protected Matrix4f projection;
    
    public JslTestShader() {
        super(330);
    }

    @Override
    public void main() {
        
        model.transform(view.transform(projection.transform(new Vector4f(attributePosition, 1.0f), gl_FragCoord)));
    }
}
