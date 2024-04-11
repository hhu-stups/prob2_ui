package de.prob2.ui.visualisation.sequencechart;

import java.nio.file.Path;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public final class JavaLocator {

	@Inject
	private JavaLocator() {
	}

	public String getJavaExecutable() {
		String javaHome = System.getProperty("java.home");
		Path javaBinPath;
		if (javaHome != null && !javaHome.isEmpty()) {
			javaBinPath = Path.of(javaHome, "bin/java");
		} else {
			javaBinPath = Path.of("java");
		}

		return javaBinPath.toString();
	}
}
