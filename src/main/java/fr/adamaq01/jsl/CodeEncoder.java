package fr.adamaq01.jsl;

import java.util.*;

import fr.adamaq01.jsl.fragments.CodeFragment;

public abstract class CodeEncoder {

    public CodeDecoder decoder;

    public abstract String createSourceCode(List<CodeFragment> in, int version);

    public void onRequestResult(List<CodeFragment> fragments) {
    }
}
