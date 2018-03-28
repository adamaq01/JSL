package fr.adamaq01.jsl.shaders;

import fr.adamaq01.jsl.JSL.Base;
import fr.adamaq01.jsl.JSL.Out;
import fr.adamaq01.jsl.JSL.In;
import fr.adamaq01.jsl.ShaderType;
import org.joml.Vector4f;

/**
 * Created by Adamaq01 on 27/03/2018.
 */
public abstract class VertexShader extends Shader {

    // Inputs

    @In
    @Base
    protected int gl_VertexID;

    @In
    @Base
    protected int gl_InstanceID;

    // Outputs

    @Out
    @Base
    protected Vector4f gl_Position;

    @Out
    @Base
    protected float gl_PointSize;

    @Out
    @Base
    protected float[] gl_ClipDistance;

    // Other

    public VertexShader(int version) {
        super(ShaderType.VERTEX, version);
    }
}
