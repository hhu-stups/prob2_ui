package de.prob2.ui.rulevalidation;

import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.prob.model.brules.ComputationStatus;
import de.prob.model.brules.OperationStatus;
import de.prob.model.brules.RuleStatus;
import de.prob.model.brules.RulesChecker;
import de.prob2.ui.dynamic.dotty.DotView;
import de.prob2.ui.prob2fx.CurrentTrace;

import java.util.*;
public class RulesDependencyGraphCreator {

	public static void visualizeCompleteGraph(DotView formulaStage, CurrentTrace currentTrace, Collection<AbstractOperation> operations) {
		RulesChecker rulesChecker = new RulesChecker(currentTrace.get());
		rulesChecker.init();
		Map<AbstractOperation, OperationStatus> operationStates = rulesChecker.getOperationStates();
		List<String> nodes = new ArrayList<>();
		List<String> edges = new ArrayList<>();
		for (AbstractOperation operation : operations) {
			//if (operation instanceof FunctionOperation) {
			//	break;
			//}
			String shape = "ellipse";
			String statusColor = "transparent";
			boolean notChecked = false;
			if (operationStates.get(operation) instanceof RuleStatus ruleStatus) {
                statusColor = switch (ruleStatus) {
                    case FAIL -> "#cc2f274d";
                    case SUCCESS -> "#4caf504d";
                    case NOT_CHECKED -> "transparent";
                    case DISABLED -> "lightgray";
                };
				if (ruleStatus == RuleStatus.NOT_CHECKED) {
					notChecked = true;
				}
			} else if (operationStates.get(operation) instanceof ComputationStatus computationStatus) {
				statusColor = switch (computationStatus) {
					case EXECUTED -> "#4caf504d";
					case NOT_EXECUTED -> "transparent";
					case DISABLED -> "lightgray";
				};
				if (computationStatus == ComputationStatus.NOT_EXECUTED) {
					notChecked = true;
				}
				shape = "rectangle";
			}
			nodes.add("rec(shape: \"" + shape + "\", style: \"filled\", fillcolor: \"" + statusColor + "\", nodes: \"" + operation.getName() + "\")");
			Set<AbstractOperation> dependencies = operation.getRequiredDependencies();
			for (AbstractOperation dependency : dependencies) {
				String edgeColor = "black";
				if (notChecked && operationStates.get(dependency) instanceof RuleStatus ruleStatus) {
					edgeColor = switch (ruleStatus) {
						case FAIL, DISABLED -> "#cc2f27";
                        case SUCCESS -> "#4caf50";
						case NOT_CHECKED -> "black";
					};
				} else if (notChecked && operationStates.get(dependency) instanceof ComputationStatus computationStatus) {
					edgeColor = switch (computationStatus) {
						case DISABLED -> "#cc2f27";
						default -> "black";
					};
				}
				edges.add("rec(color: \"" + edgeColor + "\", label: \"\", edge: \"" + operation.getName() + "\"|->\"" + dependency.getName() + "\")");
			}
		}
		formulaStage.show();
		formulaStage.toFront();
		formulaStage.visualizeFormulaAsGraph("rec(nodes: {" + String.join(",", nodes) + "}, edges: {" + String.join(",", edges) + "})");
	}
}
