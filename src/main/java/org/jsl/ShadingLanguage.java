package org.jsl;

import org.jsl.glsl.GLSLEncoder;

/**
 * Created by Adamaq01 on 27/03/2018.
 */
// TODO: HLSL Encoder
public enum ShadingLanguage {

    GLSL("OpenGL Shading Language", GLSLEncoder.class), HLSL("High Level Shading Language", null);

    private String name;
    private Class<? extends CodeEncoder> encoder;

    ShadingLanguage(String name, Class<? extends CodeEncoder> encoder) {
        this.name = name;
        this.encoder = encoder;
    }

    public String getName() {
        return name;
    }

    public Class<? extends CodeEncoder> getEncoder() {
        return encoder;
    }
}
