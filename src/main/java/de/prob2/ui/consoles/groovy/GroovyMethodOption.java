package de.prob2.ui.consoles.groovy;

import java.lang.reflect.Modifier;

public enum GroovyMethodOption {
	STATIC, NONSTATIC, ALL;

	public boolean matches(int modifiers) {
		return matches(Modifier.isStatic(modifiers));
	}

	public boolean matches(boolean isStatic) {
		if (this == STATIC) {
			return isStatic;
		} else if (this == NONSTATIC) {
			return !isStatic;
		} else {
			return true;
		}
	}
}
