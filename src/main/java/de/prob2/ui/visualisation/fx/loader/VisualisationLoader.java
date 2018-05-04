package de.prob2.ui.visualisation.fx.loader;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.io.Files;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visualisation.fx.Visualisation;
import de.prob2.ui.visualisation.fx.loader.clazz.InMemoryClassloader;
import de.prob2.ui.visualisation.fx.loader.clazz.InMemoryCompiler;
import de.prob2.ui.visualisation.fx.loader.clazz.InMemoryCompilerException;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Heinzen
 * @since 23.09.17
 */
public class VisualisationLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(VisualisationLoader.class);

	private final StageManager stageManager;
	private final ResourceBundle bundle;

	private ClassLoader visualisationClassloader;

	public VisualisationLoader(StageManager stageManager, ResourceBundle bundle) {
		this.stageManager = stageManager;
		this.bundle = bundle;
	}

	public Visualisation loadVisualization(File selectedVisualisation) {
		String selectedVisualisationExtension = Files.getFileExtension(selectedVisualisation.getName());
		if (selectedVisualisationExtension.equals("java")) {
			LOGGER.debug("Try to open visualization-class {}.", selectedVisualisation);
			return loadVisualisationClass(selectedVisualisation);
		} else if (selectedVisualisationExtension.equals("jar")){
			LOGGER.debug("Try to open visualization-jar {}.", selectedVisualisation);
			return loadVisualisationJar(selectedVisualisation);
		}
		return null;
	}

	private Visualisation loadVisualisationClass(File selectedVisualization) {
		String fileName = selectedVisualization.getName();
		try {
			LOGGER.debug("Try to compile file {}.", fileName);
			String className = fileName.replace(".java", "");

			InMemoryClassloader classLoader = new InMemoryClassloader(this.getClass().getClassLoader());
			visualisationClassloader = classLoader;

			Class<?> visualizationClass = new InMemoryCompiler().compile(className, selectedVisualization, classLoader);
			LOGGER.debug("Successfully compiled class {}.", className);

			if (checkVisualizationClass(visualizationClass)) {
				LOGGER.debug("Class {} extends the abstract class Visualisation. Create an instance of it.", className);
				return (Visualisation)visualizationClass.getConstructor().newInstance();
			} else {
				LOGGER.warn("Class {} does not extend the abstract class Visualisation.", className);
				showAlert(false, Alert.AlertType.WARNING, "visualisation.loader.no.extend", className);
				return null;
			}
		} catch (InMemoryCompilerException e) {
			LOGGER.warn("Exception while compiling the class \"{}\".", fileName, e);
			showAlert(true, Alert.AlertType.WARNING, "visualisation.loader.compiler",
			fileName, e.getCompilerMessage(), ButtonType.OK);
			return null;
		} catch (Exception e) {
			LOGGER.warn("Exception while loading the visualization:\n{}", fileName, e);
			showAlert(false, Alert.AlertType.ERROR,
					"visualisation.loader.exception");
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

	private Visualisation loadVisualisationJar(File selectedVisualization) {
		String fileName = selectedVisualization.getName();
		try (JarFile selectedVisualizationJar = new JarFile(selectedVisualization)) {
			URL[] urls = {new URL("jar:file:" + selectedVisualizationJar.getName() +"!/")};
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
				LOGGER.debug("Found visualization-class {} in jar: {}", className, fileName);
				return (Visualisation)visualizationClass.getConstructor().newInstance();

			} else {
				LOGGER.warn("No visualization-class found in jar: {}", fileName);
				showAlert(false, Alert.AlertType.WARNING,
						"visualisation.loader.no.class",
						fileName);
				return null;
			}
		} catch (Exception e) {
			LOGGER.warn("Exception while loading the visualization:\n{}", fileName, e);
			showAlert(false, Alert.AlertType.ERROR,
					"visualisation.loader.exception");
			return null;
		}
	}

	private boolean checkVisualizationClass(Class<?> visualizationClass) {
		return visualizationClass != null &&
				visualizationClass.getSuperclass() != null &&
				visualizationClass.getSuperclass().equals(Visualisation.class);
	}

	private void showAlert(boolean resizable, Alert.AlertType type, String key, Object... textParams) {
		Alert alert = stageManager.makeAlert(type,
				String.format(bundle.getString(key), textParams),
				ButtonType.OK);
		alert.initOwner(stageManager.getCurrent());
		alert.setTitle(bundle.getString("menu.visualisation"));
		alert.setResizable(resizable);
		alert.show();
	}
}
