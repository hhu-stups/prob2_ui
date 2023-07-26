package de.prob2.ui.railml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.State;
import javafx.beans.property.SimpleStringProperty;

import java.nio.file.Path;

@Singleton
public class RailMLFile {

	private Path path;
	private State state;

	@Inject
	public RailMLFile() {
		this.path = null;
		this.state = null;
	}

	public void setPath(Path path) {
		this.path = path;
	}
	public Path getPath() {
		return path;
	}
	public void setState(State state) {
		this.state = state;
	}
	public State getState() {
		return state;
	}
}
