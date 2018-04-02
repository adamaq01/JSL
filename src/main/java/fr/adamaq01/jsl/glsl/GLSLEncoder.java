package fr.adamaq01.jsl.glsl;

import java.util.*;

import fr.adamaq01.jsl.*;
import fr.adamaq01.jsl.fragments.*;
import fr.adamaq01.jsl.glsl.fragments.StructFragment;
import fr.adamaq01.jsl.fragments.MethodCallFragment.InvokeTypes;
import org.joml.*;
import org.joml.Math;

public class GLSLEncoder extends CodeEncoder {

    private int indentation;
    private int glslversion;
    private NewClassFragment currentClass;
    private ArrayList<String> extensions = new ArrayList<>();
    private String space = " ";
    private String tab = "    ";
    private int currentLine;
    private Stack<String> stack;
    private Stack<String> typesStack;
    private HashMap<String, String> name2type;
    private HashMap<Object, String> constants;
    private HashMap<String, String> methodReplacements;
    private ArrayList<String> initialized;
    private StartOfMethodFragment currentMethod;
    private boolean convertNumbersToChars;
    private ArrayList<String> loadedStructs = new ArrayList<>();
    private int currentRequestType;
    private static final int STRUCT = 1;
    private Object requestData;
    private boolean allowedToWrite;
    private StringBuilder output;
    private Stack<CodeFragment> waiting;
    private Stack<String> newInstances;
    private String structOwnerMethodSeparator;

    public GLSLEncoder() {
        convertNumbersToChars = true;
        stack = new Stack<>();
        typesStack = new Stack<>();
        initialized = new ArrayList<>();
        name2type = new HashMap<>();
        constants = new HashMap<>();
        methodReplacements = new HashMap<>();
        waiting = new Stack<>();
        newInstances = new Stack<>();
        structOwnerMethodSeparator = "__";

        init();
    }

    public void init() {
        setGLSLTranslation("boolean", "bool");
        setGLSLTranslation("double", "float"); // not every GPU has double
        // precision;
        setGLSLTranslation(Vector2f.class.getCanonicalName(), "vec2");
        setGLSLTranslation(Vector2d.class.getCanonicalName(), "vec2");
        setGLSLTranslation(Vector2i.class.getCanonicalName(), "vec2");
        setGLSLTranslation(Vector3f.class.getCanonicalName(), "vec3");
        setGLSLTranslation(Vector3d.class.getCanonicalName(), "vec3");
        setGLSLTranslation(Vector3i.class.getCanonicalName(), "vec3");
        setGLSLTranslation(Vector4f.class.getCanonicalName(), "vec4");
        setGLSLTranslation(Vector4d.class.getCanonicalName(), "vec4");
        setGLSLTranslation(Vector4i.class.getCanonicalName(), "vec4");
        setGLSLTranslation(Matrix3f.class.getCanonicalName(), "mat3");
        setGLSLTranslation(Matrix4f.class.getCanonicalName(), "mat4");
        setGLSLTranslation(Integer.class.getCanonicalName(), "int");

        setGLSLTranslation(Math.class.getCanonicalName(), "");

        setGLSLTranslation(Sampler2D.class.getCanonicalName(), "sampler2D");

        JOMLMappings jomlMappings = new JOMLMappings();
        methodMappings.put(Vector2f.class, jomlMappings);
        methodMappings.put(Vector2d.class, jomlMappings);
        methodMappings.put(Vector2i.class, jomlMappings);
        methodMappings.put(Vector3f.class, jomlMappings);
        methodMappings.put(Vector3d.class, jomlMappings);
        methodMappings.put(Vector3i.class, jomlMappings);
        methodMappings.put(Vector4f.class, jomlMappings);
        methodMappings.put(Vector4d.class, jomlMappings);
        methodMappings.put(Vector4i.class, jomlMappings);
        methodMappings.put(Matrix3f.class, jomlMappings);
        methodMappings.put(Matrix4f.class, jomlMappings);
    }

    private HashMap<String, String> translations = new HashMap<>();
    private HashMap<String, String> conversionsToStructs = new HashMap<>();
    private HashMap<Class<?>, Mappings> methodMappings = new HashMap<>();

    public void addToStructConversion(String javaType, String structName) {
        conversionsToStructs.put(javaType, structName);
    }

    public boolean hasStructAttached(String javaType) {
        return conversionsToStructs.containsKey(javaType);
    }

    public void setGLSLTranslation(String javaType, String glslType) {
        translations.put(javaType, glslType);
    }

    public void removeGLSLTranslation(String javaType) {
        translations.remove(javaType);
    }

    private String toGLSL(String type) {
        if (type == null) return "";
        String copy = type;
        String end = "";
        while (copy.contains("[]")) {
            copy = copy.replaceFirst("\\[\\]", "");
            end += "[]";
        }
        type = copy;
        if (conversionsToStructs.containsKey(type)) {
            return conversionsToStructs.get(type) + end;
        }
        if (translations.containsKey(type)) {
            return translations.get(type) + end;
        }
        return type + end;
    }

    private String getEndOfLine(int currentLine) {
        String s = "";
        // if(currentLine % 2 == 0)
        {
            s = " //Line #" + currentLine;
        }
        return s;
    }

    public void convertNumbersToChar(boolean convert) {
        this.convertNumbersToChars = convert;
    }

    @Override
    public void onRequestResult(List<CodeFragment> fragments) {
        if (currentRequestType == STRUCT) {
            StructFragment currentStruct = (StructFragment) requestData;
            HashMap<String, String> fields = currentStruct.fields;
            for (int i = 0; i < fragments.size(); i++) {
                CodeFragment fragment = fragments.get(i);
                if (fragment.getClass() == FieldFragment.class) {
                    FieldFragment fieldFrag = (FieldFragment) fragment;
                    fields.put(fieldFrag.name, fieldFrag.type);
                    fragment.forbiddenToWrite = true;
                }

                currentStruct.addChild(fragment);
            }
        }
    }

    @Override
    public String createSourceCode(List<CodeFragment> in, int glslversion) {
        this.glslversion = glslversion;
        StringBuilder out = new StringBuilder();
        interpret(in);
        this.output = out;
        this.allowedToWrite = true;
        println("#version " + glslversion);
        for (int index = 0; index < in.size(); index++) {
            CodeFragment fragment = in.get(index);
            this.output = out;
            this.allowedToWrite = !fragment.forbiddenToWrite;
            if (!waiting.isEmpty()) {
                handleCodeFragment(waiting.pop(), index, in, out);
            }
            handleCodeFragment(fragment, index, in, out);
        }
        return out.toString();
    }

    private void handleCodeFragment(CodeFragment fragment, int index, List<CodeFragment> in, StringBuilder out) {
        if (fragment.getClass() == NewClassFragment.class) {
            handleClassFragment((NewClassFragment) fragment, in, index, out);
            currentClass = (NewClassFragment) fragment;
        } else if (fragment.getClass() == FieldFragment.class) {
            handleFieldFragment((FieldFragment) fragment, in, index, out);
        } else if (fragment.getClass() == StartOfMethodFragment.class) {
            handleStartOfMethodFragment((StartOfMethodFragment) fragment, in, index, out);
            this.currentMethod = (StartOfMethodFragment) fragment;
        } else if (fragment.getClass() == EndOfMethodFragment.class) {
            handleEndOfMethodFragment((EndOfMethodFragment) fragment, in, index, out);
        } else if (fragment.getClass() == LineNumberFragment.class) {
            currentLine = ((LineNumberFragment) fragment).line;
        } else if (fragment.getClass() == NewArrayFragment.class) {
            handleNewArrayFragment((NewArrayFragment) fragment, in, index, out);
        } else if (fragment.getClass() == NewMultiArrayFragment.class) {
            handleNewMultiArrayFragment((NewMultiArrayFragment) fragment, in, index, out);
        } else if (fragment.getClass() == PutFieldFragment.class) {
            handlePutFieldFragment((PutFieldFragment) fragment, in, index, out);
        } else if (fragment.getClass() == GetFieldFragment.class) {
            handleGetFieldFragment((GetFieldFragment) fragment, in, index, out);
        } else if (fragment.getClass() == IntPushFragment.class) {
            handleBiPushFragment((IntPushFragment) fragment, in, index, out);
        } else if (fragment.getClass() == NewPrimitiveArrayFragment.class) {
            handleNewPrimitiveArrayFragment((NewPrimitiveArrayFragment) fragment, in, index, out);
        } else if (fragment.getClass() == LoadVariableFragment.class) {
            handleLoadVariableFragment((LoadVariableFragment) fragment, in, index, out);
        } else if (fragment.getClass() == StoreVariableFragment.class) {
            handleStoreVariableFragment((StoreVariableFragment) fragment, in, index, out);
        } else if (fragment.getClass() == LdcFragment.class) {
            handleLdcFragment((LdcFragment) fragment, in, index, out);
        } else if (fragment.getClass() == LoadConstantFragment.class) {
            handleLoadConstantFragment((LoadConstantFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ReturnValueFragment.class) {
            handleReturnValueFragment((ReturnValueFragment) fragment, in, index, out);
        } else if (fragment.getClass() == AddFragment.class) {
            handleAddFragment((AddFragment) fragment, in, index, out);
        } else if (fragment.getClass() == SubFragment.class) {
            handleSubFragment((SubFragment) fragment, in, index, out);
        } else if (fragment.getClass() == MulFragment.class) {
            handleMulFragment((MulFragment) fragment, in, index, out);
        } else if (fragment.getClass() == DivFragment.class) {
            handleDivFragment((DivFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ArrayOfArrayLoadFragment.class) {
            handleArrayOfArrayLoadFragment((ArrayOfArrayLoadFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ArrayStoreFragment.class) {
            handleArrayStoreFragment((ArrayStoreFragment) fragment, in, index, out);
        } else if (fragment.getClass() == IfStatementFragment.class) {
            handleIfStatementFragment((IfStatementFragment) fragment, in, index, out);
        } else if (fragment.getClass() == EndOfBlockFragment.class) {
            handleEndOfBlockFragment((EndOfBlockFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ElseStatementFragment.class) {
            handleElseStatementFragment((ElseStatementFragment) fragment, in, index, out);
        } else if (fragment.getClass() == MethodCallFragment.class) {
            try {
                handleMethodCallFragment((MethodCallFragment) fragment, in, index, out);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else if (fragment.getClass() == ModFragment.class) {
            handleModFragment((ModFragment) fragment, in, index, out);
        } else if (fragment.getClass() == CastFragment.class) {
            handleCastFragment((CastFragment) fragment, in, index, out);
        } else if (fragment.getClass() == LeftShiftFragment.class) {
            handleLeftShiftFragment((LeftShiftFragment) fragment, in, index, out);
        } else if (fragment.getClass() == RightShiftFragment.class) {
            handleRightShiftFragment((RightShiftFragment) fragment, in, index, out);
        } else if (fragment.getClass() == AndFragment.class) {
            handleAndFragment((AndFragment) fragment, in, index, out);
        } else if (fragment.getClass() == OrFragment.class) {
            handleOrFragment((OrFragment) fragment, in, index, out);
        } else if (fragment.getClass() == XorFragment.class) {
            handleXorFragment((XorFragment) fragment, in, index, out);
        } else if (fragment.getClass() == IfNotStatementFragment.class) {
            handleIfNotStatementFragment((IfNotStatementFragment) fragment, in, index, out);
        } else if (fragment.getClass() == PopFragment.class) {
            handlePopFragment((PopFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ReturnFragment.class) {
            handleReturnFragment((ReturnFragment) fragment, in, index, out);
        } else if (fragment.getClass() == DuplicateFragment.class) {
            handleDuplicateFragment((DuplicateFragment) fragment, in, index, out);
        } else if (fragment.getClass() == NewInstanceFragment.class) {
            handleNewInstanceFragment((NewInstanceFragment) fragment, in, index, out);
        } else if (fragment.getClass() == EqualCheckFragment.class) {
            handleEqualCheckFragment((EqualCheckFragment) fragment, in, index, out);
        } else if (fragment.getClass() == NotEqualCheckFragment.class) {
            handleNotEqualCheckFragment((NotEqualCheckFragment) fragment, in, index, out);
        } else if (fragment.getClass() == CompareFragment.class) {
            handleCompareFragment((CompareFragment) fragment, in, index, out);
        } else if (fragment.getClass() == StructFragment.class) {
            handleStructFragment((StructFragment) fragment, in, index, out);
        }
    }

    private void handleCompareFragment(CompareFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String right = stack.pop();
        String left = stack.pop();
        stack.push(left + space + (fragment.inferior ? "<" : ">") + space + right);
    }

    private void handleNotEqualCheckFragment(NotEqualCheckFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String right = stack.pop();
        String left = stack.pop();
        stack.push("(" + left + space + "!=" + space + right + ")");
    }

    private void handleEqualCheckFragment(EqualCheckFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String right = stack.pop();
        String left = stack.pop();
        stack.push("(" + left + space + "==" + space + right + ")");
    }

    private void handleNewInstanceFragment(NewInstanceFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        newInstances.push(fragment.type);
    }

    private void handleStructFragment(StructFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        println(getIndent() + "struct " + fragment.name);
        println(getIndent() + "{");
        indentation++;
        Iterator<String> it = fragment.fields.keySet().iterator();
        while (it.hasNext()) {
            String name = it.next();
            String type = toGLSL(fragment.fields.get(name));
            println(getIndent() + type + space + name + ";");
        }
        indentation--;
        println(getIndent() + "};");

        StartOfMethodFragment currentMethod = null;
        String instanceName = ("" + fragment.name.charAt(0)).toLowerCase() + fragment.name.substring(1) + "Instance";
        for (int i = 0; i < fragment.getChildren().size(); i++) {
            CodeFragment fragment1 = fragment.getChildren().get(i);
            if (fragment1 == null) continue;
            this.output = out;
            this.allowedToWrite = !fragment1.forbiddenToWrite;
            if (fragment1 instanceof StartOfMethodFragment) {
                StartOfMethodFragment method = (StartOfMethodFragment) fragment1;
                currentMethod = method;
                String oldName = currentMethod.name;
                method.varNameMap.put(0, instanceName);
                boolean isConstructor = false;
                if (currentMethod.name.equals("<init>") || currentMethod.name.equals(fragment.name + structOwnerMethodSeparator + "new")) {
                    currentMethod.name = "new";
                    method.returnType = fragment.name;
                    isConstructor = true;
                } else if (!method.argumentsNames.contains(instanceName)) {
                    method.argumentsNames.add(0, instanceName);
                    method.argumentsTypes.add(0, fragment.name);
                }
                if (!currentMethod.name.startsWith(fragment.name + structOwnerMethodSeparator))
                    currentMethod.name = fragment.name + structOwnerMethodSeparator + currentMethod.name;
                String key = toGLSL(currentMethod.owner) + "." + oldName;
                methodReplacements.put(key, currentMethod.name);
            }
            if (fragment1 instanceof LoadVariableFragment) {
                LoadVariableFragment var = (LoadVariableFragment) fragment1;
                var.variableName = currentMethod.varNameMap.get(var.variableIndex);
            } else if (fragment1 instanceof StoreVariableFragment) {
                StoreVariableFragment var = (StoreVariableFragment) fragment1;
                var.variableName = currentMethod.varNameMap.get(var.variableIndex);
            }
            if (!waiting.isEmpty()) {
                handleCodeFragment(waiting.pop(), index, in, out);
            }

            this.allowedToWrite = !fragment1.forbiddenToWrite;
            if (fragment1.getClass() == EndOfMethodFragment.class && currentMethod.name.equals(fragment.name + structOwnerMethodSeparator + "new")) {
                println(getIndent() + "return " + instanceName + ";");
            }

            handleCodeFragment(fragment1, i, fragment.getChildren(), out);
            if (fragment1.getClass() == StartOfMethodFragment.class && currentMethod.name.equals(fragment.name + structOwnerMethodSeparator + "new")) {
                println(getIndent() + fragment.name + space + instanceName + ";");
            }
        }
    }

    private void handleDuplicateFragment(DuplicateFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        if (!newInstances.isEmpty()) return;
        if (fragment.wait > 0) {
            waiting.add(fragment);
            fragment.wait--;
        } else {
            String a = stack.pop();
            stack.push(a);
            stack.push(a);
        }
    }

    private void interpret(List<CodeFragment> in) {
        Stack<String> copy = stack;
        Stack<String> tmpstack = new Stack<>();
        stack = tmpstack;
        StartOfMethodFragment currentMethod = null;
        StringBuilder nullPrinter = new StringBuilder();
        for (int i = 0; i < in.size(); i++) {
            boolean dontHandle = false;
            CodeFragment fragment = in.get(i);
            if (fragment.getClass() == StartOfMethodFragment.class) {
                currentMethod = (StartOfMethodFragment) fragment;
            } else if (fragment.getClass() == FieldFragment.class) {
                FieldFragment fieldFrag = (FieldFragment) fragment;
                if (hasStructAttached(fieldFrag.type) && !loadedStructs.contains(toGLSL(fieldFrag.type))) {
                    loadedStructs.add(toGLSL(fieldFrag.type));
                    StructFragment struct = new StructFragment();
                    struct.name = conversionsToStructs.get(fieldFrag.type);
                    HashMap<String, String> fields = new HashMap<>();
                    struct.fields = fields;
                    currentRequestType = STRUCT;
                    requestData = struct;
                    String s = "/" + fieldFrag.type.replace(".", "/") + ".class";
                    try {
                        requestAnalysisForEncoder(decoder, Class.forName(s));
                    } catch (Exception e) {
                        throw new JSLException(e);
                    }
                    in.add(i, struct);
                    currentRequestType = 0;
                    i--;
                }
            } else if (fragment.getClass() == StoreVariableFragment.class) {
                StoreVariableFragment storeFrag = (StoreVariableFragment) fragment;
                String type = storeFrag.variableType;
                if (hasStructAttached(type) && !loadedStructs.contains(toGLSL(type))) {
                    loadedStructs.add(toGLSL(type));
                    StructFragment struct = new StructFragment();
                    struct.name = conversionsToStructs.get(type);
                    HashMap<String, String> fields = new HashMap<>();
                    struct.fields = fields;
                    currentRequestType = STRUCT;
                    requestData = struct;
                    String s = "/" + type.replace(".", "/") + ".class";
                    try {
                        requestAnalysisForEncoder(decoder, Class.forName(s));
                    } catch (Exception e) {
                        throw new JSLException(e);
                    }
                    in.add(i, struct);
                    currentRequestType = 0;
                    i--;
                }
            } else if (fragment.getClass() == PutFieldFragment.class) {
                PutFieldFragment storeFrag = (PutFieldFragment) fragment;
                if (currentMethod != null && currentMethod.name.equals("<init>")) {
                    for (int ii = 0; ii < in.size(); ii++) {
                        CodeFragment fragment1 = in.get(ii);
                        if (fragment1.getClass() == FieldFragment.class) {
                            FieldFragment fieldFrag = (FieldFragment) fragment1;
                            if (fieldFrag.name.equals(storeFrag.fieldName) && fieldFrag.type.equals(storeFrag.fieldType) && !(fieldFrag.access.isFinal() && fieldFrag.initialValue != null)) {
                                fieldFrag.initialValue = stack.peek();
                                dontHandle = true;
                                storeFrag.forbiddenToWrite = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!dontHandle) {
                this.output = nullPrinter;
                this.allowedToWrite = !fragment.forbiddenToWrite;
                if (!waiting.isEmpty()) {
                    handleCodeFragment(waiting.pop(), i, in, nullPrinter);
                }
                handleCodeFragment(fragment, i, in, nullPrinter);
            }
        }

        waiting.clear();
        currentLine = 0;
        indentation = 0;
        initialized.clear();
        name2type.clear();
        currentClass = null;
        typesStack.clear();
        extensions.clear();
        constants.clear();
        stack = copy;
    }

    private void println() {
        println("");
    }

    private void println(String s) {
        if (allowedToWrite) output.append(s + "\n");
    }

    private void handleReturnFragment(ReturnFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        if (in.size() <= index + 1 || in.get(index + 1).getClass() == EndOfMethodFragment.class) {
            ;
        } else {
            println(getIndent() + "return;" + getEndOfLine(currentLine));
        }
    }

    private void handlePopFragment(PopFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        println(getIndent() + stack.pop() + ";" + getEndOfLine(currentLine));
    }

    private int countChar(String str, char c) {
        int nbr = 0;
        for (int i = 0; i < str.length(); i++)
            if (str.charAt(i) == c) nbr++;
        return nbr;
    }

    private void handleIfNotStatementFragment(IfNotStatementFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String condition = stack.pop();
        println(getIndent() + "if(!" + condition + ")" + getEndOfLine(currentLine));
        println(getIndent() + "{");
        indentation++;
    }

    private void handleXorFragment(XorFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String b = stack.pop();
        String a = stack.pop();
        stack.push("(" + a + " || " + b + ")");
    }

    private void handleOrFragment(OrFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String b = stack.pop();
        String a = stack.pop();
        stack.push("(" + a + space + (fragment.isDouble ? "||" : "|") + space + b + ")");
    }

    private void handleAndFragment(AndFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String b = stack.pop();
        String a = stack.pop();
        stack.push("(" + a + space + (fragment.isDouble ? "&&" : "&") + space + b + ")");
    }

    private void handleRightShiftFragment(RightShiftFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String b = stack.pop();
        String a = stack.pop();
        stack.push(a + ">>" + (!fragment.signed ? ">" : "") + b);
    }

    private void handleLeftShiftFragment(LeftShiftFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String b = stack.pop();
        String a = stack.pop();
        stack.push(a + "<<" + (!fragment.signed ? "<" : "") + b);
    }

    private void handleCastFragment(CastFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String toCast = stack.pop();
        String withoutPreviousCast = toCast;

        String previousType = null;
        if (withoutPreviousCast.startsWith("(")) {
            previousType = withoutPreviousCast.substring(1, withoutPreviousCast.indexOf(")") - 1);
        } else
            previousType = toGLSL(currentMethod.varName2TypeMap.get(withoutPreviousCast));
        if (!previousType.equals(toGLSL(fragment.to)))
            stack.push("(" + toGLSL(fragment.to) + ")" + toCast);
    }

    private void handleModFragment(ModFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String a = stack.pop();
        String b = stack.pop();
        stack.push("mod(" + b + ", " + a + ")");
    }

    private void handleMethodCallFragment(MethodCallFragment fragment, List<CodeFragment> in, int index, StringBuilder out) throws ClassNotFoundException {
        String s = "";
        String n = fragment.methodName;
        boolean isConstructor = false;
        if (n.equals("<init>")) {
            n = toGLSL(fragment.methodOwner);
            isConstructor = true;
            if (!newInstances.isEmpty()) newInstances.pop();
        }
        String key = fragment.methodName;
        if (toGLSL(fragment.methodOwner) != null && !toGLSL(fragment.methodOwner).equals("null") && !toGLSL(fragment.methodOwner).trim().equals(""))
            key = toGLSL(fragment.methodOwner) + "." + key;
        if (methodReplacements.containsKey(key)) {
            String nold = key;
            n = methodReplacements.get(key);
        }
        if (fragment.invokeType == InvokeTypes.SPECIAL && currentMethod.name.equals("<init>") && fragment.methodOwner.equals(currentClass.superclass)) {
            this.allowedToWrite = false;
        }

        s += n + "(";
        ArrayList<String> args = new ArrayList<String>();
        for (@SuppressWarnings("unused")
                String type : fragment.argumentsTypes) {
            String arg = stack.pop();
            if (arg.startsWith("(") && arg.endsWith(")") && countChar(arg, '(') == countChar(arg, ')')) {
                arg = arg.substring(1, arg.length() - 1);
            }
            args.add(arg);
        }
        String argsStr = "";
        for (int i = 0; i < args.size(); i++) {
            if (i != 0) argsStr += ", ";
            argsStr += args.get(args.size() - 1 - i);
        }
        s += argsStr;
        s += ")";

        boolean ownerBefore = false;
        boolean parenthesis = true;
        int ownerPosition = 0;
        boolean actsAsField = false;
        for (CodeFragment child : fragment.getChildren()) {
            if (child.getClass() == AnnotationFragment.class) {
                AnnotationFragment annot = (AnnotationFragment) child;
                if (annot.name.equals(JSL.Substitute.class.getCanonicalName())) {
                    if (!annot.values.get("value").equals("$"))
                        n = (String) annot.values.get("value");
                    if (annot.values.containsKey("ownerBefore"))
                        ownerBefore = (Boolean) annot.values.get("ownerBefore");
                    if (annot.values.containsKey("ownerPosition"))
                        ownerPosition = (Integer) annot.values.get("ownerPosition");
                    if (annot.values.containsKey("actsAsField"))
                        actsAsField = (Boolean) annot.values.get("actsAsField");
                    if (annot.values.containsKey("usesParenthesis"))
                        parenthesis = (Boolean) annot.values.get("usesParenthesis");
                }
            }
        }

        if (methodMappings.containsKey(Class.forName(fragment.methodOwner))) {
            Mappings mappings = methodMappings.get(Class.forName(fragment.methodOwner));
            if (mappings.getMappings().containsKey(fragment.methodName)) {
                JSL.MethodSubstitute methodSubstitute = mappings.getMappings().get(fragment.methodName);
                n = methodSubstitute.getValue();
                ownerBefore = methodSubstitute.ownerBefore();
                ownerPosition = methodSubstitute.getOwnerPosition();
                actsAsField = methodSubstitute.actsAsField();
                parenthesis = methodSubstitute.usesParenthesis();
            }
        }

        if (fragment.invokeType == InvokeTypes.VIRTUAL) {
            String owner = stack.pop();
            if (owner.equals(currentClass.className) || owner.equals("this")) {
                owner = null;
            } else {
                if (owner.startsWith("(") && owner.endsWith(")") && countChar(owner, '(') == countChar(owner, ')')) {
                    owner = owner.substring(1, owner.length() - 1);
                }
            }
            if (!ownerBefore) {
                if (actsAsField) {
                    if (n.length() >= 1)
                        s = (owner != null ? owner : "") + "." + n;
                    else
                        s = (owner != null ? owner : "");
                    if (argsStr.length() > 0) {
                        s += " = " + argsStr;
                    }
                } else
                    s = n + (parenthesis ? "(" : "") + (owner != null ? owner + (argsStr.length() > 0 ? ", " : "") : "") + argsStr + (parenthesis ? ")" : "");
            } else
                s = (owner != null ? owner : "") + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : "");
            if (fragment.returnType.equals("void")) {
                println(getIndent() + s + ";" + getEndOfLine(currentLine));
            } else
                stack.push("(" + s + ")");
        } else if (fragment.invokeType == InvokeTypes.STATIC) {
            String ownership = "";
            String owner = toGLSL(fragment.methodOwner);
            if (owner != null && !owner.trim().equals("") && !owner.equals("null"))
                ownership = owner + (n.length() > 0 ? "." : "");
            stack.push(ownership + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : ""));
        } else {
            stack.push(n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : ""));
        }

    }

    private void handleElseStatementFragment(ElseStatementFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        println(getIndent() + "else" + getEndOfLine(currentLine));
        println(getIndent() + "{");
        indentation++;
    }

    private void handleEndOfBlockFragment(EndOfBlockFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        indentation--;
        println(getIndent() + "}");
    }

    private void handleIfStatementFragment(IfStatementFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String condition = stack.pop();
        println(getIndent() + "if(" + condition + ")" + getEndOfLine(currentLine));
        println(getIndent() + "{");
        indentation++;
    }

    private void handleArrayStoreFragment(ArrayStoreFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String result = "";
        String toAdd = "";
        for (int i = 0; i < 2; i++) {
            String lastType = typesStack.pop();
            String copy = lastType;
            int dimensions = 0;
            if (copy != null) while (copy.indexOf("[]") >= 0) {
                copy = copy.substring(copy.indexOf("[]") + 2);
                dimensions++;
            }
            String val = stack.pop();
            String arrayIndex = "";
            for (int dim = 0; dim < dimensions; dim++) {
                arrayIndex = "[" + stack.pop() + "]" + arrayIndex;
            }
            String name = stack.pop();
            if (i == 1)
                result = val + toAdd + " = " + result;
            else if (i == 0) {
                result = val + result;
                toAdd = "[" + name + "]";
            }
        }
        println(getIndent() + result + ";" + getEndOfLine(currentLine));
    }

    private void handleArrayOfArrayLoadFragment(ArrayOfArrayLoadFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String value = stack.pop();
        String name = stack.pop();
        stack.push(name + "[" + value + "]");
        if (name2type.containsKey(name + "[" + value + "]")) {
            name2type.put(name + "[" + value + "]", name.substring(0, name.indexOf("[")));
        }
        typesStack.push(name2type.get(name + "[" + value + "]"));
    }

    private void handleDivFragment(DivFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String a = stack.pop();
        String b = stack.pop();
        stack.push(b + "/" + a);
    }

    private void handleMulFragment(MulFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String a = stack.pop();
        String b = stack.pop();
        stack.push(b + "*" + a);
    }

    private void handleSubFragment(SubFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String a = stack.pop();
        String b = stack.pop();
        stack.push(b + "-" + a);
    }

    private void handleAddFragment(AddFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String a = stack.pop();
        String b = stack.pop();
        stack.push(b + "+" + a);
    }

    private void handleReturnValueFragment(ReturnValueFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        println(getIndent() + "return" + space + stack.pop() + ";" + getEndOfLine(currentLine));
    }

    private void handleLoadConstantFragment(LoadConstantFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        stack.push(fragment.value + "");
    }

    private void handleLdcFragment(LdcFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        if (constants.containsKey(fragment.value))
            stack.push("" + constants.get(fragment.value));
        else if (fragment.value instanceof String)
            stack.push("\"" + fragment.value + "\"");
        else if (fragment.value instanceof Number)
            stack.push("" + fragment.value);
    }

    private void handleStoreVariableFragment(StoreVariableFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String value = stack.pop();
        if (value.startsWith("(") && value.endsWith(")") && countChar(value, '(') == countChar(value, ')')) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.equals(fragment.variableName + "+1")) {
            println(getIndent() + fragment.variableName + "++;" + getEndOfLine(currentLine));
            return;
        } else if (value.equals(fragment.variableName + "-1")) {
            println(getIndent() + fragment.variableName + "--;" + getEndOfLine(currentLine));
            return;
        }
        String glslType = toGLSL(currentMethod.varName2TypeMap.get(fragment.variableName));
        if (glslType.equals("bool")) {
            if (value.equals("0"))
                value = "false";
            else if (value.equals("1")) value = "true";
        } else if (glslType.equals("char")) {
            if (convertNumbersToChars) {
                try {
                    value = "'" + Character.valueOf((char) Integer.parseInt(value)) + "'";
                } catch (Exception e) {
                    ;
                }
            }
        }
        if (initialized.contains(fragment.variableName)) {
            println(getIndent() + fragment.variableName + " = " + value + ";" + getEndOfLine(currentLine));
        } else {
            initialized.add(fragment.variableName);
            println(getIndent() + toGLSL(currentMethod.varName2TypeMap.get(fragment.variableName)) + space + fragment.variableName + " = " + value + ";" + getEndOfLine(currentLine));
        }
    }

    private void handleLoadVariableFragment(LoadVariableFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        stack.push(fragment.variableName);
    }

    private void handleNewPrimitiveArrayFragment(NewPrimitiveArrayFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String dimension = "[" + stack.pop() + "]";
        stack.push(fragment.type + dimension);
    }

    private void handleBiPushFragment(IntPushFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        stack.push(fragment.value + "");
    }

    private void handleGetFieldFragment(GetFieldFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String owner = toGLSL(stack.pop());
        String ownership = owner + ".";
        if (owner.equals("this")) ownership = "";
        stack.push(ownership + fragment.fieldName);
        typesStack.push(fragment.fieldType);
    }

    private void handlePutFieldFragment(PutFieldFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String value = stack.pop();
        if (value.startsWith("(") && value.endsWith(")") && countChar(value, '(') == countChar(value, ')')) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.equals(fragment.fieldName + "+1")) {
            println(getIndent() + fragment.fieldName + "++;" + getEndOfLine(currentLine));
            return;
        } else if (value.equals(fragment.fieldName + "-1")) {
            println(getIndent() + fragment.fieldName + "--;" + getEndOfLine(currentLine));
            return;
        }
        String glslType = toGLSL(currentMethod.varName2TypeMap.get(fragment.fieldName));
        if (glslType.equals("bool")) {
            if (value.equals("0"))
                value = "false";
            else if (value.equals("1")) value = "true";
        }
        String owner = stack.pop();
        String ownership = owner + ".";
        for (int i = 0; i < index; i++) {
            CodeFragment frag = in.get(i);
            if (frag.getClass() == FieldFragment.class) {
                FieldFragment fieldFrag = (FieldFragment) frag;
                if (fieldFrag.access.isFinal() && fieldFrag.name.equals(fragment.fieldName)) {
                    return;
                }
            }
        }
        if (owner.equals("this")) ownership = "";
        println(getIndent() + ownership + fragment.fieldName + space + "=" + space + value + ";" + getEndOfLine(currentLine));
    }

    private void handleNewMultiArrayFragment(NewMultiArrayFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String s = "";
        ArrayList<String> list = new ArrayList<String>();
        for (int dim = 0; dim < fragment.dimensions; dim++) {
            list.add(stack.pop());
        }
        for (int dim = 0; dim < fragment.dimensions; dim++) {
            s += "[" + list.get(list.size() - dim - 1) + "]";
        }
        stack.push(toGLSL(fragment.type) + s);
    }

    private void handleNewArrayFragment(NewArrayFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String s = "[" + stack.pop() + "]";
        stack.push(toGLSL(fragment.type) + s);
    }

    private void handleEndOfMethodFragment(EndOfMethodFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        if (fragment.name.equals("<init>")) return;
        println("}");
        indentation--;
    }

    private void handleStartOfMethodFragment(StartOfMethodFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        if (fragment.name.equals("<init>")) return;
        initialized.clear();
        println();
        String args = "";
        for (int i = 0; i < fragment.argumentsNames.size(); i++) {
            String s = toGLSL(fragment.argumentsTypes.get(i)) + space + fragment.argumentsNames.get(i);
            if (i != 0) args += ", ";
            args += s;
        }
        println(toGLSL(fragment.returnType) + space + fragment.name + "(" + args + ")\n{");
        indentation++;
    }

    private void handleFieldFragment(FieldFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        String storageType = null;
        for (CodeFragment child : fragment.getChildren()) {
            if (child instanceof AnnotationFragment) {
                AnnotationFragment annot = (AnnotationFragment) child;
                if (annot.name.equals(JSL.Uniform.class.getCanonicalName())) {
                    storageType = "uniform";
                } else if (annot.name.equals(JSL.Attribute.class.getCanonicalName())) {
                    storageType = "attribute";
                } else if (annot.name.equals(JSL.In.class.getCanonicalName())) {
                    storageType = "in";
                } else if (annot.name.equals(JSL.Out.class.getCanonicalName())) {
                    storageType = "out";
                } else if (annot.name.equals(JSL.Varying.class.getCanonicalName())) {
                    storageType = "varying";
                } else if (annot.name.equals(JSL.Layout.class.getCanonicalName())) {
                    int location = (Integer) annot.values.get("location");
                    out.append("layout(location = " + location + ") ");
                }
            }
        }
        if (storageType == null) {
            storageType = "uniform";
        }
        if (fragment.access.isFinal()) {
            if (fragment.access.isStatic()) {
                println("#define" + space + fragment.name + space + fragment.initialValue);
                constants.put(fragment.initialValue, fragment.name);
            } else {
                storageType = "const";
                println(storageType + space + toGLSL(fragment.type) + space + fragment.name + space + "=" + space + fragment.initialValue + ";");
                constants.put(fragment.initialValue, fragment.name);
            }
        } else {
            if (fragment.initialValue != null) {
                println(storageType + space + toGLSL(fragment.type) + space + fragment.name + space + "=" + space + fragment.initialValue + ";");
            } else
                println(storageType + space + toGLSL(fragment.type) + space + fragment.name + ";");
        }
    }

    @SuppressWarnings("unchecked")
    private void handleClassFragment(NewClassFragment fragment, List<CodeFragment> in, int index, StringBuilder out) {
        println("// Original class name: " + fragment.className + " compiled from " + fragment.sourceFile + " and of version " + fragment.classVersion);
        for (CodeFragment child : fragment.getChildren()) {
            if (child instanceof AnnotationFragment) {
                AnnotationFragment annotFragment = (AnnotationFragment) child;
                println();
                if (annotFragment.name.equals(JSL.Extensions.class.getCanonicalName())) {
                    ArrayList<String> values = (ArrayList<String>) annotFragment.values.get("value");
                    for (String extension : values)
                        println("#extension " + extension + " : enable" + getEndOfLine(currentLine));
                }
            }
        }
    }

    private String getIndent() {
        String s = "";
        for (int i = 0; i < indentation; i++)
            s += tab;
        return s;
    }

    private void requestAnalysisForEncoder(CodeDecoder codeDecoder, Class<?> requestData) {
        List<CodeFragment> fragments = codeDecoder.handleClass(requestData);
        onRequestResult(fragments);
    }
}
