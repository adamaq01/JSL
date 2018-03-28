package fr.adamaq01.jsl;

/**
 * Created by Adamaq01 on 27/03/2018.
 */
public enum ShaderType {

    VERTEX("Vertex"), FRAGMENT("Fragment"), GEOMETRY("Geometry");

    private String type;

    ShaderType(String type) {
        this.type = type;
    }

    public String getType() {
        return type + " Shader";
    }
}
