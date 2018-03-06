package de.prob2.ui.config;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import com.google.inject.Singleton;

import javafx.stage.FileChooser;
import javafx.stage.Window;

@Singleton
public class FileChooserManager {
	public enum Kind {
		PROJECTS_AND_MACHINES, PLUGINS, VISUALISATIONS, PERSPECTIVES, TRACES
	}

	private final Map<Kind, File> initialDirectories = new EnumMap<>(Kind.class);

	public File showOpenDialog(final FileChooser fileChooser, final Kind kind, final Window window) {
		if (containsValidInitialDirectory(kind)) {
			fileChooser.setInitialDirectory(getInitialDirectory(kind));
		}
		final File file = fileChooser.showOpenDialog(window);
		if (file != null) {
			setInitialDirectory(kind, file.getParentFile());
		}
		return file;
	}

	public File showSaveDialog(final FileChooser fileChooser, final Kind kind, final Window window) {
		if (containsValidInitialDirectory(kind)) {
			fileChooser.setInitialDirectory(getInitialDirectory(kind));
		}
		final File file = fileChooser.showSaveDialog(window);
		if (file != null) {
			setInitialDirectory(kind, file.getParentFile());
		}
		return file;
	}

	public boolean containsValidInitialDirectory(Kind kind) {
		return initialDirectories.containsKey(kind) && initialDirectories.get(kind).exists();
	}

	public File getInitialDirectory(Kind kind) {
		return this.initialDirectories.get(kind);
	}

	public void setInitialDirectory(Kind kind, File dir) {
		if (dir != null && dir.exists()) {
			initialDirectories.put(kind, dir);
		}
	}

	public Map<Kind, File> getInitialDirectories() {
		return initialDirectories;
	}
}
