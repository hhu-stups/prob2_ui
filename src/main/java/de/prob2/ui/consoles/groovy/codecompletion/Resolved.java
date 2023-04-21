package de.prob2.ui.consoles.groovy.codecompletion;

import groovy.lang.MetaClass;

public interface Resolved {

	MetaClass getMetaClass();

	Resolved resolve(String name);

}
