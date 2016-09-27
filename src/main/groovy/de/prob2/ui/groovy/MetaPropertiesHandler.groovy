package de.prob2.ui.groovy

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.codehaus.groovy.runtime.HandleMetaClass
import org.codehaus.groovy.runtime.InvokerHelper


def static handleProperties(Object object, Collection<GroovyClassPropertyItem> properties) {
	
	for (PropertyValue p : object.metaPropertyValues) {
		properties.add(new GroovyClassPropertyItem(p))
	}
}

def static handleMethods(Object object, Collection<GroovyClassPropertyItem> methods) {
	// We have to use getMetaClass() as a method instead of by property access here.
	// Otherwise getting the metaclass does not work properly for some objects (such as maps - getting a property on a map is interpreted as a map lookup).
	// noinspection JavaStylePropertiesInvocation
	for (MetaMethod m : object.getMetaClass().metaMethods) {
		methods.add(new GroovyClassPropertyItem(m))
	}
}

def static handleProperties(Class <? extends Object> clazz, Collection<GroovyClassPropertyItem> properties) {
	HandleMetaClass metaClass = new HandleMetaClass(InvokerHelper.getMetaClass(clazz));
	for (MetaProperty m : metaClass.getProperties()) {
		properties.add(new GroovyClassPropertyItem(m))
	}
}

def static handleMethods(Class <? extends Object> clazz, Collection<GroovyClassPropertyItem> methods) {
	HandleMetaClass metaClass = new HandleMetaClass(InvokerHelper.getMetaClass(clazz));
	for (MetaMethod m : metaClass.metaMethods) {
		methods.add(new GroovyClassPropertyItem(m))
	}
}

