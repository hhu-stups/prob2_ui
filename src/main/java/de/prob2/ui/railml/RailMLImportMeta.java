package de.prob2.ui.railml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;

import java.nio.file.Path;
import java.util.List;

@Singleton
public class RailMLImportMeta {

	protected enum VisualisationStrategy {
		D4R("D4R_customGraph"), RAIL_OSCOPE("NOR_customGraph"), DOT("DOT_customGraph");
		private final String customGraphDefinition;
		VisualisationStrategy(final String customGraphDefinition) {
			this.customGraphDefinition = customGraphDefinition;
		}
		public String getCustomGraphDefinition() {
			return this.customGraphDefinition;
		}
	};
	private Path path;
	private State state;
	private String name;
	private VisualisationStrategy visualisationStrategy;
	private List<IEvalElement> customGraphFormula;

	@Inject
	public RailMLImportMeta() {
		this.path = null;
		this.state = null;
		this.name = null;
		this.visualisationStrategy = null;
		this.customGraphFormula = null;
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
	protected List<IEvalElement> getCustomGraphFormula() {
		return customGraphFormula;
	}
	protected void setCustomGraphFormula(List<IEvalElement> customGraphFormula) {
		this.customGraphFormula = customGraphFormula;
	}
}
