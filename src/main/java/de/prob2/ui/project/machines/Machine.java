package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

public class Machine {
	private String name;
	private String description;
	private String location;

	public Machine(String name, String description, Path location) {
		this.name = name;
		this.description = description;
		this.location = location.toString();
	}

	public String getName() {
		return name;
	}

	public String getFileName() {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] splittedFileName = location.split(pattern);
		return splittedFileName[splittedFileName.length - 1];
	}

	public String getDescription() {
		return description;
	}

	public Path getPath() {
		return Paths.get(location);
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Machine)) {
			return false;
		}
		Machine otherMachine = (Machine) other;
		return otherMachine.location.equals(this.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(location);
	}
}
