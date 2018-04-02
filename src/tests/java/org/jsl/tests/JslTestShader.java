package org.jsl.tests;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import org.jsl.JSL.In;
import org.jsl.JSL.Layout;
import org.jsl.JSL.Uniform;
import org.jsl.shaders.FragmentShader;


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
        
        gl_FragCoord = model.transform(view.transform(projection.transform(new Vector4f(attributePosition, 1.0f))));
    }
}
