package de.prob2.ui.groovy

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
	Object o = clazz.newInstance();
	for (PropertyValue p : o.metaPropertyValues) {
		properties.add(new GroovyClassPropertyItem(p))
	}
}

def static handleMethods(Class <? extends Object> clazz, Collection<GroovyClassPropertyItem> methods) {
	Object o = clazz.newInstance();
	for (MetaMethod m : o.getMetaClass().metaMethods) {
		methods.add(new GroovyClassPropertyItem(m))
	}
}

