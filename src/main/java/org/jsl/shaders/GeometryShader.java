package org.jsl.shaders;

import org.jsl.JSL.Base;
import org.jsl.JSL.Out;
import org.jsl.JSL.In;
import org.jsl.ShaderType;
import org.joml.Vector4f;

/**
 * Created by Adamaq01 on 27/03/2018.
 */
public abstract class GeometryShader extends Shader {

    // Inputs - Outputs

    @In
    @Out
    @Base
    protected Vector4f gl_Position;

    @In
    @Out
    @Base
    protected float gl_PointSize;

    @In
    @Out
    @Base
    protected float[] gl_ClipDistance;

    // Other

    public GeometryShader(int version) {
        super(ShaderType.GEOMETRY, version);
    }
}
