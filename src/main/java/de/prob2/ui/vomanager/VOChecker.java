package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.voparser.VOParseException;
import de.prob.voparser.VOParser;
import de.prob.voparser.VTType;
import de.prob.voparser.node.AAndVo;
import de.prob.voparser.node.AEquivalentVo;
import de.prob.voparser.node.AIdentifierVo;
import de.prob.voparser.node.AImpliesVo;
import de.prob.voparser.node.ANotVo;
import de.prob.voparser.node.AOrVo;
import de.prob.voparser.node.ASequentialVo;
import de.prob.voparser.node.PVo;
import de.prob.voparser.node.Start;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.SimulationItemHandler;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.Modelchecker;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaHandler;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;


@Singleton
public class VOChecker {

	private final CurrentProject currentProject;

	private final RequirementHandler requirementHandler;

	private final Modelchecker modelchecker;

	private final LTLFormulaChecker ltlChecker;

	private final SymbolicCheckingFormulaHandler symbolicChecker;

	private final TraceChecker traceChecker;

	private final SimulationItemHandler simulationItemHandler;

	private final VOParser voParser;

	@Inject
	public VOChecker(final CurrentProject currentProject, final RequirementHandler requirementHandler, final Modelchecker modelchecker,
					 final LTLFormulaChecker ltlChecker, final SymbolicCheckingFormulaHandler symbolicChecker,
					 final TraceChecker traceChecker, final SimulationItemHandler simulationItemHandler) {
		this.voParser = new VOParser();
		this.currentProject = currentProject;
		this.requirementHandler = requirementHandler;
		this.modelchecker = modelchecker;
		this.ltlChecker = ltlChecker;
		this.symbolicChecker = symbolicChecker;
		this.traceChecker = traceChecker;
		this.simulationItemHandler = simulationItemHandler;
	}

	public void checkRequirement(Requirement requirement, Machine machine, VOManagerSetting setting) {
		if(setting == VOManagerSetting.REQUIREMENT) {
			checkRequirementOnRequirementView(requirement);
		} else if(setting == VOManagerSetting.MACHINE) {
			checkRequirementOnMachineView(requirement, machine);
		}
		requirementHandler.updateChecked(currentProject.get(), machine, requirement, setting);
	}

	private void checkRequirementOnRequirementView(Requirement requirement) {
		for(Machine machine : currentProject.getMachines()) {
			for (ValidationObligation validationObligation : machine.getValidationObligations()) {
				if(validationObligation.getRequirement().equals(requirement.getName())) {
					this.checkVO(validationObligation);
				}
			}
		}
	}

	private void checkRequirementOnMachineView(Requirement requirement, Machine machine) {
		for (ValidationObligation validationObligation : machine.getValidationObligations()) {
			if(validationObligation.getRequirement().equals(requirement.getName())) {
				this.checkVO(validationObligation);
			}
		}
	}

	public void registerTask(String id, VTType type) {
		voParser.registerTask(id, type);
	}

	public void deregisterTask(String id) {
		voParser.deregisterTask(id);
	}

	public void checkVOExpression(ValidationObligation VO) throws VOParseException {
		String expression = VO.getExpression();
		Start ast = voParser.parseFormula(expression);
		voParser.semanticCheck(ast);
		boolean check = checkVOExpression(ast.getPVo(), VO);
	}

	public boolean checkVOExpression(PVo ast, ValidationObligation VO) {
		VO.getTasks().clear();
		if(ast instanceof AIdentifierVo) {
			return checkAtomicExpression((AIdentifierVo) ast, VO);
		} else if(ast instanceof ANotVo) {
			return checkNotExpression((ANotVo) ast, VO);
		} else if(ast instanceof AAndVo) {
			return checkAndExpression((AAndVo) ast, VO);
		} else if(ast instanceof AOrVo) {
			return checkOrExpression((AOrVo) ast, VO);
		} else if(ast instanceof AImpliesVo) {
			return checkImpliesExpression((AImpliesVo) ast, VO);
		} else if(ast instanceof AEquivalentVo) {
			return checkEquivalentExpression((AEquivalentVo) ast, VO);
		} else if(ast instanceof ASequentialVo) {
			return checkSequentialExpression((ASequentialVo) ast, VO);
		} else {
			throw new RuntimeException("VO expression type is unknown: " + ast.getClass());
		}
	}

	public boolean checkAtomicExpression(AIdentifierVo ast, ValidationObligation VO) {
		Machine machine = currentProject.getCurrentMachine();
		IValidationTask validationTask = machine.getValidationTasks().get(ast.getIdentifierLiteral().getText());
		VO.getTasks().add(validationTask);
		return checkVT(validationTask);
	}

	public boolean checkNotExpression(ANotVo ast, ValidationObligation VO) {
		// TODO
		return false;
	}

	public boolean checkAndExpression(AAndVo ast, ValidationObligation VO) {
		boolean leftRes = checkVOExpression(ast.getLeft(), VO);
		boolean rightRes = checkVOExpression(ast.getRight(), VO);
		return leftRes && rightRes;
	}

	public boolean checkOrExpression(AOrVo ast, ValidationObligation VO) {
		// TODO
		return false;
	}

	public boolean checkImpliesExpression(AImpliesVo ast, ValidationObligation VO) {
		// TODO
		return false;
	}

	public boolean checkEquivalentExpression(AEquivalentVo ast, ValidationObligation VO) {
		// TODO
		return false;
	}

	public boolean checkSequentialExpression(ASequentialVo ast, ValidationObligation VO) {
		// TODO
		return false;
	}


	public void checkVO(ValidationObligation validationObligation) {
		try {
			checkVOExpression(validationObligation);
		} catch (VOParseException e) {
			e.printStackTrace();
		}
		// TODO Implement full validation task syntax (not just conjunctions)
		//for (IValidationTask validationTask : validationObligation.getTasks()) {
		//	if (validationTask != null && validationTask.getChecked() != Checked.SUCCESS) {
		//		checkVT(validationTask);
		//	}
		//}
	}

	public boolean checkVT(IValidationTask validationTask) {
		if (validationTask instanceof ModelCheckingItem) {
			modelchecker.checkItem((ModelCheckingItem) validationTask, false, false);
		} else if (validationTask instanceof LTLFormulaItem) {
			ltlChecker.checkFormula((LTLFormulaItem) validationTask);
		} else if (validationTask instanceof SymbolicCheckingFormulaItem) {
			symbolicChecker.handleItem((SymbolicCheckingFormulaItem) validationTask, false);
		} else if (validationTask instanceof ReplayTrace) {
			traceChecker.check((ReplayTrace) validationTask, true);
		} else if (validationTask instanceof SimulationItem) {
			simulationItemHandler.checkItem((SimulationItem) validationTask, false);
		} else {
			throw new AssertionError("Unhandled validation task type: " + validationTask.getClass());
		}
		return false;
	}

}
