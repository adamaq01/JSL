package fr.adamaq01.jsl;

/**
 * Created by Adamaq01 on 28/03/2018.
 */
public class JSLException extends RuntimeException {

    public JSLException(String message) {
        super(message);
    }

    public JSLException(Exception exception) {
        super(exception);
    }
}
