package org.jsl;

import java.util.*;

import org.jsl.fragments.CodeFragment;

public abstract class CodeEncoder {

    public CodeDecoder decoder;

    public abstract String createSourceCode(List<CodeFragment> in, int version);

    public void onRequestResult(List<CodeFragment> fragments) {
    }
}
