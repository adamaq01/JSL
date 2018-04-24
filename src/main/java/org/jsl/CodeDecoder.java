package org.jsl;

import java.util.List;

import org.jsl.fragments.CodeFragment;

public abstract class CodeDecoder {

	public abstract List<CodeFragment> handleClass(Class<?> data);
}
