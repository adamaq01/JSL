package org.jsl.tests;

import org.jsl.JSL;
import org.jsl.ShadingLanguage;

public class JslTests {
    public static void main(String[] args) {
        System.out.println(JSL.convert(new JslTestShaderChild(), ShadingLanguage.GLSL));
    }
}
