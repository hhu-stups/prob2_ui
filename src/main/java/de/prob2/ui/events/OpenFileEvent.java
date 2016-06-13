package de.prob2.ui.events;

import java.io.File;

public class OpenFileEvent {

	private File file;
	private String extensionFilter;

	public OpenFileEvent(File selectedFile, String extensionFilter) {
		this.file = selectedFile;
		this.extensionFilter = extensionFilter;
	}

	public String getFileName() {
		return file.getAbsolutePath();
	}

	public String getNormalizedExtension() {
		return extensionFilter;
	}

}
