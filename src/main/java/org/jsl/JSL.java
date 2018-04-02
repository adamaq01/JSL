package org.jsl;

import org.jsl.bytecode.BytecodeDecoder;
import org.jsl.shaders.Shader;

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
        int index() default -1;
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

    public static class MethodSubstitute {

        /*
         * No default value
         */
        private String value;
        /*
         * Default value: true
         */
        private boolean usesParenthesis;
        /*
         * Default value: false
         */
        private boolean ownerBefore;
        /*
         * Default value: 0
         */
        private int ownerPosition;
        /*
         * Default value: false
         */
        private boolean actsAsField;

        public MethodSubstitute(String value) {
            this.value = value;
            this.usesParenthesis = true;
            this.ownerBefore = false;
            this.ownerPosition = 0;
            this.actsAsField = false;
        }

        public MethodSubstitute usesParenthesis(boolean value) {
            this.usesParenthesis = value;
            return this;
        }

        public MethodSubstitute ownerBefore(boolean value) {
            this.ownerBefore = value;
            return this;
        }

        public MethodSubstitute ownerPosition(int value) {
            this.ownerPosition = value;
            return this;
        }

        public MethodSubstitute actsAsField(boolean value) {
            this.actsAsField = value;
            return this;
        }

        public String getValue() {
            return value;
        }

        public boolean usesParenthesis() {
            return usesParenthesis;
        }

        public boolean ownerBefore() {
            return ownerBefore;
        }

        public int getOwnerPosition() {
            return ownerPosition;
        }

        public boolean actsAsField() {
            return actsAsField;
        }
    }
}
