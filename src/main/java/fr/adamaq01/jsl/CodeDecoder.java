package fr.adamaq01.jsl;

import java.util.List;

import fr.adamaq01.jsl.fragments.CodeFragment;

public abstract class CodeDecoder {

	public abstract List<CodeFragment> handleClass(Class<?> data);
}
