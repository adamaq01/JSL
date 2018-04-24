package fr.adamaq01.jsl.tests;

import static org.joml.Math.sin;

public class JslTestShaderChild extends JslTestShader {
    @Override
    public void main() {
        gl_FragCoord = foo(attributePosition);
        gl_FragCoord.x = (float) sin(time);
    }
}
