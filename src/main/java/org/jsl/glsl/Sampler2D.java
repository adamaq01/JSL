package org.jsl.glsl;

import org.jsl.JSL;

/**
 * Created by Adamaq01 on 02/04/2018.
 */
public class Sampler2D {

    public int id;

    @JSL.Substitute(value = "", usesParenthesis = false, ownerBefore = true)
    public Sampler2D(int id) {
        this.id = id;
    }
}
