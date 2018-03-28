package fr.adamaq01.jsl.shaders;

import fr.adamaq01.jsl.JSL.Base;
import fr.adamaq01.jsl.JSL.Out;
import fr.adamaq01.jsl.JSL.In;
import fr.adamaq01.jsl.ShaderType;
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
