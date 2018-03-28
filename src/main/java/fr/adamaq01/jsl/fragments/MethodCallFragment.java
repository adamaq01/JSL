package fr.adamaq01.jsl.fragments;

public class MethodCallFragment extends CodeFragment {
    public String methodOwner;
    public String methodName;
    public String[] argumentsTypes;
    public InvokeTypes invokeType;
    public String returnType;

    public static enum InvokeTypes {
        STATIC, VIRTUAL, SPECIAL
    }
}
