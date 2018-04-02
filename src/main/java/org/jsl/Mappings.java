package org.jsl;

import java.util.HashMap;

/**
 * Created by Adamaq01 on 02/04/2018.
 */
public class Mappings {

    protected HashMap<String, JSL.MethodSubstitute> mappings;

    public Mappings() {
        this.mappings = new HashMap<>();
    }

    public HashMap<String, JSL.MethodSubstitute> getMappings() {
        return mappings;
    }
}
