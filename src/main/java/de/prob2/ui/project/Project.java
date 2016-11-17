package de.prob2.ui.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Project {
	private String name;
	private List<File> files = new ArrayList<File>();
	private File location;
	private final boolean singleFile;

	public Project(String name, List<File> files, File location) {
		this.name = name;
		this.files = files;
		this.singleFile = false;
		this.location = location;
	}
	
	public Project(File file) {
		this.name = file.getName();
		this.files = new ArrayList<File>();
		files.add(file);
		this.singleFile = true;
	}

	public String getName() {
		return name;
	}
	
	public List<File> getFiles() {
		return files;
	}
	
	public boolean isSingleFile() {
		return singleFile;
	}

	public void addFile(File file) {
		this.files.add(file);
	}
	
	public File getLocation() {
		return location;
	}
}
