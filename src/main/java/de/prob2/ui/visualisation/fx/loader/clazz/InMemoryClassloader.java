package de.prob2.ui.visualisation.fx.loader.clazz;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Christoph Heinzen on 27.04.17.
 */
public class InMemoryClassloader extends ClassLoader {

	private final Map<String, InMemoryJavaFileObject> classFiles = new HashMap<>();

	public InMemoryClassloader(ClassLoader parentClassLoader) {
		super(parentClassLoader);
	}

	void addClassFile(InMemoryJavaFileObject javaFileObject) {
		classFiles.put(javaFileObject.getClassName(), javaFileObject);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		InMemoryJavaFileObject fileObject = classFiles.get(name);

		if (fileObject != null) {
			byte[] bytes = fileObject.getClassBytes();
			return defineClass(name, bytes, 0, bytes.length);
		}

		return super.findClass(name);
	}

}
