package de.prob2.ui.consoles.groovy

import de.prob2.ui.consoles.groovy.objects.GroovyAbstractItem
import de.prob2.ui.consoles.groovy.objects.GroovyClassPropertyItem
import org.codehaus.groovy.runtime.HandleMetaClass
import org.codehaus.groovy.runtime.InvokerHelper

static handleProperties(Object object, Collection<? extends GroovyAbstractItem> properties) {
	for (PropertyValue p : object.metaPropertyValues) {
		properties.add(new GroovyClassPropertyItem(p))
	}
}

static handleProperties(Class <? extends Object> clazz, Collection<? extends GroovyAbstractItem> properties) {
	HandleMetaClass metaClass = new HandleMetaClass(InvokerHelper.getMetaClass(clazz))
	for (MetaProperty m : metaClass.getProperties()) {
		properties.add(new GroovyClassPropertyItem(m))
	}
}

static handleMethods(Class <? extends Object> clazz, Collection<? extends GroovyAbstractItem> methods, GroovyMethodOption option) {
	HandleMetaClass metaClass = new HandleMetaClass(InvokerHelper.getMetaClass(clazz))
	for (MetaMethod m : metaClass.metaMethods) {
		if ((option == GroovyMethodOption.ALL) || (option == GroovyMethodOption.NONSTATIC && !m.isStatic()) || (option == GroovyMethodOption.STATIC && !m.isStatic())) {
			methods.add(new GroovyClassPropertyItem(m))
		}
	}
}

