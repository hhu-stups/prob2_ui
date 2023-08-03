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
	private String name;
	private RailMLStage.VisualisationStrategy visualisationStrategy;

	@Inject
	public RailMLFile() {
		this.path = null;
		this.state = null;
		this.name = null;
		this.visualisationStrategy = null;
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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public RailMLStage.VisualisationStrategy getVisualisationStrategy() {
		return visualisationStrategy;
	}
	public void setVisualisationStrategy(RailMLStage.VisualisationStrategy visualisationStrategy) {
		this.visualisationStrategy = visualisationStrategy;
	}
}
