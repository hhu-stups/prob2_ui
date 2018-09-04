package de.prob2.ui.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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

	private final Map<Kind, Path> initialDirectories = new EnumMap<>(Kind.class);

	public Path showOpenDialog(final FileChooser fileChooser, final Kind kind, final Window window) {
		if (containsValidInitialDirectory(kind)) {
			fileChooser.setInitialDirectory(getInitialDirectory(kind).toFile());
		}
		final File file = fileChooser.showOpenDialog(window);
		final Path path = file == null ? null : file.toPath();
		if (path != null) {
			setInitialDirectory(kind, path.getParent());
		}
		return path;
	}

	public Path showSaveDialog(final FileChooser fileChooser, final Kind kind, final Window window) {
		if (containsValidInitialDirectory(kind)) {
			fileChooser.setInitialDirectory(getInitialDirectory(kind).toFile());
		}
		final File file = fileChooser.showSaveDialog(window);
		final Path path = file == null ? null : file.toPath();
		if (path != null) {
			setInitialDirectory(kind, path.getParent());
		}
		return path;
	}

	public boolean containsValidInitialDirectory(Kind kind) {
		return initialDirectories.containsKey(kind) && Files.exists(initialDirectories.get(kind));
	}

	public Path getInitialDirectory(Kind kind) {
		return this.initialDirectories.get(kind);
	}

	public void setInitialDirectory(Kind kind, Path dir) {
		if (dir != null && Files.exists(dir)) {
			initialDirectories.put(kind, dir);
		}
	}

	public Map<Kind, Path> getInitialDirectories() {
		return initialDirectories;
	}
}
