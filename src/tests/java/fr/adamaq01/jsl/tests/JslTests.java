package fr.adamaq01.jsl.tests;

import fr.adamaq01.jsl.JSL;
import fr.adamaq01.jsl.ShadingLanguage;

public class JslTests {
    public static void main(String[] args) {
        System.out.println(JSL.convert(new JslTestShader(), ShadingLanguage.GLSL));
    }
}
