package de.prob2.ui.dotty;

import de.prob.animator.command.GetSvgForVisualizationCommand;
import de.prob.animator.command.GetSvgForVisualizationCommand.Option;

public enum DotVisualisationType {
	
	DFA_MERGE("DFA Merge", Option.DFA_MERGE), 
	STATE_AS_GRAPH("State as Graph", Option.STATE_AS_GRAPH), 
	INVARIANT("Invariant", Option.INVARIANT), 
	PROPERTIES("Properties", Option.PROPERTIES), 
	ASSERTIONS("Assertions", Option.ASSERTIONS), 
	DEADLOCK("Deadlock", Option.DEADLOCK), 
	GOAL("Goal", Option.GOAL), 
	LAST_ERROR("Last error", Option.LAST_ERROR), 
	ENABLE_GRAPH("Enable Graph", Option.ENABLE_GRAPH), 
	DEPENDENCE_GRAPH("Dependence Graph", Option.DEPENDENCE_GRAPH),
	DEFINITIONS("Definitions", Option.DEFINITIONS),
	EXPR_AS_GRAPH("Expression as graph", Option.EXPR_AS_GRAPH), 
	FORMULA_TREE("Formula tree", Option.FORMULA_TREE), 
	TRANSITION_DIAGRAM("Transition diagram", Option.TRANSITION_DIAGRAM), 
	PREDICATE_DEPENDENCY("Predicate Dependency", Option.PREDICATE_DEPENDENCY),
	STATE_SPACE("State Space", Option.STATE_SPACE);
	
	
	private final String name;
	
	private final Option option;
	
	private DotVisualisationType(final String name, final GetSvgForVisualizationCommand.Option option) {
		this.name = name;
		this.option = option;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Option getOption() {
		return option;
	}
	
	
	

}
