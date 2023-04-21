package de.prob2.ui.consoles.groovy.codecompletion;

import java.util.Objects;

import de.prob2.ui.consoles.groovy.GroovyMethodOption;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import groovy.lang.MetaClass;

public final class Clazz implements Resolved {

	private final Class<?> clazz;

	public Clazz(Class<?> clazz) {
		this.clazz = Objects.requireNonNull(clazz);
	}

	public Class<?> getClazz() {
		return clazz;
	}

	@Override
	public MetaClass getMetaClass() {
		return DefaultGroovyMethods.getMetaClass(clazz);
	}

	@Override
	public Resolved resolve(String name) {
		return ResolverUtils.getMember(null, getMetaClass(), GroovyMethodOption.STATIC, name);
	}
}
