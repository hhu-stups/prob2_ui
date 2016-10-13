package de.prob2.ui.groovy

import org.codehaus.groovy.runtime.HandleMetaClass
import org.codehaus.groovy.runtime.InvokerHelper

import de.prob2.ui.groovy.objects.GroovyClassPropertyItem;


def static handleProperties(Object object, Collection<GroovyClassPropertyItem> properties) {
	for (PropertyValue p : object.metaPropertyValues) {
		properties.add(new GroovyClassPropertyItem(p))
	}
}

def static handleProperties(Class <? extends Object> clazz, Collection<GroovyClassPropertyItem> properties) {
	HandleMetaClass metaClass = new HandleMetaClass(InvokerHelper.getMetaClass(clazz));
	for (MetaProperty m : metaClass.getProperties()) {
		properties.add(new GroovyClassPropertyItem(m))
	}
}

def static handleMethods(Class <? extends Object> clazz, Collection<GroovyClassPropertyItem> methods, GroovyMethodOption option)  {
	HandleMetaClass metaClass = new HandleMetaClass(InvokerHelper.getMetaClass(clazz));
	for (MetaMethod m : metaClass.metaMethods) {
		if((option == GroovyMethodOption.ALL) || (option == GroovyMethodOption.NONSTATIC && !m.isStatic()) || (option == GroovyMethodOption.STATIC && !m.isStatic())) {
			methods.add(new GroovyClassPropertyItem(m))
		}
	}
}

