package fr.adamaq01.jsl.glsl;

import fr.adamaq01.jsl.JSL;
import fr.adamaq01.jsl.Mappings;

/**
 * Created by Adamaq01 on 02/04/2018.
 */
public class JOMLMappings extends Mappings {

    public JOMLMappings() {
        this.mappings.put("mul", new JSL.MethodSubstitute("*").ownerBefore(true).usesParenthesis(false));
        this.mappings.put("add", new JSL.MethodSubstitute("+").ownerBefore(true).usesParenthesis(false));
        this.mappings.put("sub", new JSL.MethodSubstitute("-").ownerBefore(true).usesParenthesis(false));
        this.mappings.put("transform", new JSL.MethodSubstitute("*").ownerBefore(true).usesParenthesis(false));
    }
}
