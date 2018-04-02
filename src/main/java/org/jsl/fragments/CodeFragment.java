package org.jsl.fragments;

import java.util.*;

public abstract class CodeFragment {

    public boolean forbiddenToWrite = false;
    private ArrayList<CodeFragment> children = new ArrayList<>();

    public void addChild(CodeFragment fragment) {
        children.add(fragment);
    }

    public ArrayList<CodeFragment> getChildren() {
        return children;
    }
}
