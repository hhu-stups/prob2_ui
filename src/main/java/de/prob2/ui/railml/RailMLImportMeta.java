package de.prob2.ui.railml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.State;

import java.nio.file.Path;

@Singleton
public class RailMLImportMeta {

	protected enum VisualisationStrategy {D4R, RAIL_OSCOPE, DOT};
	private Path path;
	private State state;
	private String name;
	private VisualisationStrategy visualisationStrategy;

	@Inject
	public RailMLImportMeta() {
		this.path = null;
		this.state = null;
		this.name = null;
		this.visualisationStrategy = null;
	}

	public void setPath(Path path) {
		this.path = path;
	}
	protected Path getPath() {
		return path;
	}
	protected void setState(State state) {
		this.state = state;
	}
	protected State getState() {
		return state;
	}
	protected void perform(String operation) {
		state = state.perform(operation);
	}
	protected void perform(String operation, String predicates) {
		state = state.perform(operation, predicates);
	}
	protected String getName() {
		return name;
	}
	protected void setName(String name) {
		this.name = name;
	}
	protected VisualisationStrategy getVisualisationStrategy() {
		return visualisationStrategy;
	}
	protected void setVisualisationStrategy(VisualisationStrategy visualisationStrategy) {
		this.visualisationStrategy = visualisationStrategy;
	}
}
