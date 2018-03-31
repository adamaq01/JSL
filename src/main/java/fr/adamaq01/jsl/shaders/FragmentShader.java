package fr.adamaq01.jsl.shaders;

import fr.adamaq01.jsl.JSL.Base;
import fr.adamaq01.jsl.JSL.Out;
import fr.adamaq01.jsl.JSL.In;
import fr.adamaq01.jsl.ShaderType;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Created by Adamaq01 on 27/03/2018.
 */
public abstract class FragmentShader extends Shader {

    // Inputs

    @In
    @Base
    protected Vector4f gl_FragCoord;

    @In
    @Base
    protected boolean gl_FrontFacing;

    @In
    @Base
    protected Vector2f gl_PointCoord;

    // Outputs

    @Out
    protected Vector4f gl_FragColor;

    @Out
    @Base
    protected float gl_FragDepth;

    // Other

    public FragmentShader(int version) {
        super(ShaderType.FRAGMENT, version);
    }
}
