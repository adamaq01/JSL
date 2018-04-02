package fr.adamaq01.jsl.glsl;

import fr.adamaq01.jsl.JSL;

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
