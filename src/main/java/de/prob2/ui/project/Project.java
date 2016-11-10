package de.prob2.ui.project;

import java.io.File;
import java.util.List;

public class Project {
	private String name;
	private List<File> files;

	public Project(String name, List<File> files) {
		this.name = name;
		this.files = files;
	}

	public String getName() {
		return name;
	}
	
	public List<File> getFiles() {
		return files;
	}
}
