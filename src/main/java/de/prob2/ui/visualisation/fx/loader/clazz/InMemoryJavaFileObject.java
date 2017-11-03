package de.prob2.ui.visualisation.fx.loader.clazz;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * Created by Christoph Heinzen on 27.04.17.
 */
public class InMemoryJavaFileObject extends SimpleJavaFileObject {

	private final ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
	private final String className;

	InMemoryJavaFileObject(String className) {
		super(URI.create("string:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
		this.className = className;
	}

	String getClassName() {
		return className;
	}

	byte[] getClassBytes() {
		return baos.toByteArray();
	}

	@Override
	public OutputStream openOutputStream() {
		return baos;
	}
}
