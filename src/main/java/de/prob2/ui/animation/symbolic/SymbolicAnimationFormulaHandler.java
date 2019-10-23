package de.prob2.ui.animation.symbolic;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.animator.command.ConstraintBasedSequenceCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.StateSpace;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGeneratorCreator;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaHandler;
import de.prob2.ui.verifications.AbstractResultHandler;

@Singleton
public class SymbolicAnimationFormulaHandler implements SymbolicFormulaHandler<SymbolicAnimationFormulaItem> {

	private final CurrentTrace currentTrace;

	private final SymbolicAnimationChecker symbolicChecker;
	
	private final TestCaseGeneratorCreator testCaseGeneratorCreator;

	private final SymbolicAnimationResultHandler resultHandler;

	private final CurrentProject currentProject;

	@Inject
	private SymbolicAnimationFormulaHandler(final CurrentTrace currentTrace, final CurrentProject currentProject,
										   final SymbolicAnimationChecker symbolicChecker,
										   final TestCaseGeneratorCreator testCaseGeneratorCreator,
										   final SymbolicAnimationResultHandler resultHandler) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.symbolicChecker = symbolicChecker;
		this.testCaseGeneratorCreator = testCaseGeneratorCreator;
		this.resultHandler = resultHandler;
	}

	public void addFormula(String name, SymbolicExecutionType type, boolean checking) {
		SymbolicAnimationFormulaItem formula = new SymbolicAnimationFormulaItem(name, type);
		addFormula(formula,checking);
	}

	public void addFormula(int depth, int level, boolean checking) {
		SymbolicAnimationFormulaItem formula = new SymbolicAnimationFormulaItem(depth, level);
		addFormula(formula,checking);
	}

	public void addFormula(int depth, List<String> operations, boolean checking) {
		SymbolicAnimationFormulaItem formula = new SymbolicAnimationFormulaItem(depth, operations);
		addFormula(formula,checking);
	}

	public void addFormula(SymbolicAnimationFormulaItem formula, boolean checking) {
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			if(!currentMachine.getSymbolicAnimationFormulas().contains(formula)) {
				currentMachine.addSymbolicAnimationFormula(formula);
			} else if(!checking) {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
		}
	}

	public void handleSequence(SymbolicAnimationFormulaItem item, boolean checkAll) {
		List<String> events = Arrays.asList(item.getCode().replace(" ", "").split(";"));
		ConstraintBasedSequenceCheckCommand cmd = new ConstraintBasedSequenceCheckCommand(currentTrace.getStateSpace(), events, new EventB("true", FormulaExpand.EXPAND));
		symbolicChecker.checkItem(item, cmd, currentTrace.getStateSpace(), checkAll);
	}

	public void findValidState(SymbolicAnimationFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		FindStateCommand cmd = new FindStateCommand(stateSpace, new EventB(item.getCode(), FormulaExpand.EXPAND), true);
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}

	public void generateTestCases(SymbolicAnimationFormulaItem item, boolean checkAll) {
		AbstractModel model = currentTrace.getModel();
		if(!(model instanceof ClassicalBModel)) {
			return;
		}
		ClassicalBModel bModel = (ClassicalBModel) model;
		ConstraintBasedTestCaseGenerator testCaseGenerator = testCaseGeneratorCreator.getTestCaseGenerator(bModel, item);
		symbolicChecker.checkItem(item, testCaseGenerator, checkAll);
	}


	public void handleItem(SymbolicAnimationFormulaItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}
		SymbolicExecutionType type = item.getType();
		switch(type) {
			case SEQUENCE:
				handleSequence(item, checkAll);
				break;
			case FIND_VALID_STATE:
				findValidState(item, checkAll);
				break;
			case MCDC:
				generateTestCases(item, checkAll);
				break;
			case COVERED_OPERATIONS:
				generateTestCases(item, checkAll);
				break;
			default:
				break;
		}
	}
	
	public void handleMachine(Machine machine) {
		machine.getSymbolicAnimationFormulas().forEach(item -> handleItem(item, true));
	}
	
}
