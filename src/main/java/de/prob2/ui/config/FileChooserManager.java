package de.prob2.ui.config;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
		if(file != null) {
			setInitialDirectory(kind, file.getParentFile());
		}
		return file;
	}
	
	public File showSaveDialog(final FileChooser fileChooser, final Kind kind, final Window window) {
		if (containsValidInitialDirectory(kind)) {
			fileChooser.setInitialDirectory(getInitialDirectory(kind));
		}
		final File file = fileChooser.showSaveDialog(window);
		if(file != null) {
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

	public void setInitialDirectories(FileChooserInitialDirectories dirs) {
		if (dirs != null) {
			for (Entry<String, String> entry : dirs.directories.entrySet()) {
				final String name = entry.getKey();
				final File dir = new File(entry.getValue());
				if (contains(name) && dir.exists() && dir.isDirectory()) {
					Kind kind = Kind.valueOf(name);
					this.initialDirectories.put(kind, dir);
				}
			}
		}
	}

	private static boolean contains(String name) {
		for (Kind c : Kind.values()) {
			if (c.name().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public FileChooserInitialDirectories getFileChooserInitialDirectories() {
		return new FileChooserInitialDirectories(initialDirectories);
	}

	// gson format
	public static class FileChooserInitialDirectories {
		final Map<String, String> directories = new HashMap<>();
		FileChooserInitialDirectories(Map<Kind, File> initialDirectories) {
			for (Entry<Kind, File> entry : initialDirectories.entrySet()) {
				directories.put(entry.getKey().toString(), entry.getValue().getAbsolutePath());
			}
		}

	}

}
