package de.prob2.ui.visualisation.fx.loader;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visualisation.fx.Visualisation;
import de.prob2.ui.visualisation.fx.loader.clazz.InMemoryClassloader;
import de.prob2.ui.visualisation.fx.loader.clazz.InMemoryCompiler;
import de.prob2.ui.visualisation.fx.loader.clazz.InMemoryCompilerException;

import javafx.scene.control.Alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualisationLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(VisualisationLoader.class);

	private final StageManager stageManager;

	private ClassLoader visualisationClassloader;

	public VisualisationLoader(StageManager stageManager) {
		this.stageManager = stageManager;
	}

	public Visualisation loadVisualization(Path selectedVisualisation) {
		final String fileName = selectedVisualisation.getFileName().toString();
		if (fileName.endsWith(".java")) {
			LOGGER.debug("Try to open visualization-class {}.", selectedVisualisation);
			return loadVisualisationClass(selectedVisualisation);
		} else if (fileName.endsWith(".jar")) {
			LOGGER.debug("Try to open visualization-jar {}.", selectedVisualisation);
			return loadVisualisationJar(selectedVisualisation);
		} else {
			throw new IllegalArgumentException("Unknown visualization file extension (not a .java or .jar file): " + fileName);
		}
	}

	private Visualisation loadVisualisationClass(Path selectedVisualization) {
		try {
			LOGGER.debug("Try to compile file {}.", selectedVisualization);
			String className = selectedVisualization.getFileName().toString().replace(".java", "");

			InMemoryClassloader classLoader = new InMemoryClassloader(this.getClass().getClassLoader());
			visualisationClassloader = classLoader;

			Class<?> visualizationClass = new InMemoryCompiler().compile(className, selectedVisualization, classLoader);
			LOGGER.debug("Successfully compiled class {}.", className);

			if (checkVisualizationClass(visualizationClass)) {
				LOGGER.debug("Class {} extends the abstract class Visualisation. Create an instance of it.", className);
				return (Visualisation)visualizationClass.getConstructor().newInstance();
			} else {
				LOGGER.warn("Class {} does not extend the abstract class Visualisation.", className);
				stageManager
						.makeAlert(Alert.AlertType.WARNING, "",
								"visualisation.fx.loader.alerts.noValidVisualisationClass.content", className)
						.showAndWait();
				return null;
			}
		} catch (InMemoryCompilerException e) {
			LOGGER.warn("Exception while compiling the class \"{}\".", selectedVisualization, e);
			stageManager.makeExceptionAlert(e, "visualisation.fx.loader.alerts.couldNotCompile.content", selectedVisualization)
					.showAndWait();
			return null;
		} catch (Exception e) {
			LOGGER.warn("Exception while loading the visualization:\n{}", selectedVisualization, e);
			stageManager.makeExceptionAlert(e, "visualisation.fx.loader.alerts.exceptionWhileLoading.content").showAndWait();
			return null;
		}
	}

	public void closeClassloader() {
		LOGGER.debug("Try to close visualization classloader.");
		if (visualisationClassloader instanceof Closeable) {
			try {
				LOGGER.debug("Classloader implements closeable, so close it!");
				((Closeable) visualisationClassloader).close();
			} catch (IOException e) {
				LOGGER.warn("VisualisationLoader: Cannot closeClassloader classloader!", e);
			}
		}
	}

	private Visualisation loadVisualisationJar(Path selectedVisualization) {
		try (JarFile selectedVisualizationJar = new JarFile(selectedVisualization.toFile())) {
			URL[] urls = {new URI("jar:file:" + selectedVisualizationJar.getName() +"!/").toURL()};
			URLClassLoader classloader = URLClassLoader.newInstance(urls, this.getClass().getClassLoader());
			visualisationClassloader = classloader;
			Class<?> visualizationClass = null;
			String className = null;
			Enumeration<JarEntry> jarEntries = selectedVisualizationJar.entries();
			while (jarEntries.hasMoreElements()) {
				JarEntry jarEntry = jarEntries.nextElement();
				if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")) {
					continue;
				}
				className = jarEntry.getName().substring(0, jarEntry.getName().length() - 6);
				className = className.replace('/', '.');
				visualizationClass = classloader.loadClass(className);
				if (checkVisualizationClass(visualizationClass)) {
					break;
				}
				visualizationClass = null;
			}
			if (visualizationClass != null) {
				LOGGER.debug("Found visualization-class {} in jar: {}", className, selectedVisualization);
				return (Visualisation)visualizationClass.getConstructor().newInstance();

			} else {
				LOGGER.warn("No visualization-class found in jar: {}", selectedVisualization);
				stageManager.makeAlert(Alert.AlertType.WARNING, "",
						"visualisation.fx.loader.alerts.noVisualisationClass.content", className).showAndWait();
				return null;
			}
		} catch (Exception e) {
			LOGGER.warn("Exception while loading the visualization: {}", selectedVisualization, e);
			stageManager.makeExceptionAlert(e, "visualisation.fx.loader.alerts.exceptionWhileLoading.content").showAndWait();
			return null;
		}
	}

	private boolean checkVisualizationClass(Class<?> visualizationClass) {
		return visualizationClass != null &&
				visualizationClass.getSuperclass() != null &&
				visualizationClass.getSuperclass().equals(Visualisation.class);
	}
}
