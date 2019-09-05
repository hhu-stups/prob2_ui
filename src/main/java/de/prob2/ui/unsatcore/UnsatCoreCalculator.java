package de.prob2.ui.unsatcore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.animator.command.GetMachineStructureCommand;
import de.prob.animator.command.UnsatRegularCoreCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.animator.domainobjects.Join;
import de.prob.animator.prologast.ASTCategory;
import de.prob.animator.prologast.ASTFormula;
import de.prob.animator.prologast.PrologASTNode;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class UnsatCoreCalculator {

	private final CurrentTrace currentTrace;

	private ObjectProperty<IBEvalElement> unsatCore;

	@Inject
	private UnsatCoreCalculator(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		this.unsatCore = new SimpleObjectProperty<>(null);
		this.currentTrace.addListener((observable, from, to) -> {
			if(to != null) {
				final Set<Transition> operations = to.getNextTransitions(true, FormulaExpand.TRUNCATE);
				if (!to.getCurrentState().isInitialised() && operations.isEmpty()) {
					calculate();
				}
			}
		});
	}

	private void calculate() {
		ClassicalBModel bModel = (ClassicalBModel) currentTrace.getModel();
		IBEvalElement properties = extractProperties(bModel);
		if(properties == null) {
			return;
		}
		UnsatRegularCoreCommand unsatCoreCmd = new UnsatRegularCoreCommand(properties, new ArrayList<>());
		currentTrace.getStateSpace().execute(unsatCoreCmd);
		this.unsatCore.set(unsatCoreCmd.getCore());
	}

	private IBEvalElement extractProperties(ClassicalBModel bModel) {
		final GetMachineStructureCommand machineStructureCmd = new GetMachineStructureCommand();
		currentTrace.getStateSpace().execute(machineStructureCmd);
		List<PrologASTNode> properties = machineStructureCmd.getPrologASTList().stream()
				.filter(astNode -> astNode instanceof ASTCategory)
				.filter(astNode -> "PROPERTIES".equals(((ASTCategory) astNode).getName()))
				.collect(Collectors.toList());
		if(properties.isEmpty()) {
			return null;
		}
		return (IBEvalElement) Join.conjunctWithStrings(bModel, properties.get(0).getSubnodes().stream()
				.map(prop -> ((ASTFormula) prop).getPrettyPrint())
				.collect(Collectors.toList()));
	}

	public ObjectProperty<IBEvalElement> unsatCoreProperty() {
		return unsatCore;
	}
}
