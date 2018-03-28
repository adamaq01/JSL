package fr.adamaq01.jsl;

import fr.adamaq01.jsl.bytecode.BytecodeDecoder;
import fr.adamaq01.jsl.shaders.Shader;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Adamaq01 on 27/03/2018.
 */
public class JSL {

    public static String convert(Shader shader, ShadingLanguage shadingLanguage) {
        CodeDecoder decoder = new BytecodeDecoder();
        CodeEncoder encoder;
        try {
            encoder = shadingLanguage.getEncoder().newInstance();
        } catch (Exception e) {
            throw new JSLException(e);
        }
        encoder.decoder = decoder;
        return encoder.createSourceCode(decoder.handleClass(shader.getClass()), shader.getVersion());
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Extensions {

        String[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Base {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Const {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Layout {

        int location();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Attribute {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Uniform {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Varying {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface In {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Out {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Substitute {

        String value();

        boolean usesParenthesis() default true;

        boolean ownerBefore() default false;

        int ownerPosition() default 0;

        boolean actsAsField() default false;

    }
}
