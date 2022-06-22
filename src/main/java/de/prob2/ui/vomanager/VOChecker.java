package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.voparser.AbstractVOInterpreter;
import de.prob.voparser.VOParseException;
import de.prob.voparser.VOParser;
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
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.Modelchecker;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaHandler;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;


@Singleton
public class VOChecker extends AbstractVOInterpreter {

	private final CurrentProject currentProject;

	private final RequirementHandler requirementHandler;

	private final Modelchecker modelchecker;

	private final LTLFormulaChecker ltlChecker;

	private final SymbolicCheckingFormulaHandler symbolicChecker;

	private final TraceChecker traceChecker;

	private final SimulationItemHandler simulationItemHandler;

	@Inject
	public VOChecker(final CurrentProject currentProject, final RequirementHandler requirementHandler, final Modelchecker modelchecker,
					 final LTLFormulaChecker ltlChecker, final SymbolicCheckingFormulaHandler symbolicChecker,
					 final TraceChecker traceChecker, final SimulationItemHandler simulationItemHandler) {
		super();
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

	public void interpretVOExpression(String VO) throws VOParseException {
		super.interpretVOExpression(VO);
		Start ast = voParser.parseFormula(VO);
		interpretVOExpression(ast.getPVo());
	}

	public void interpretVOExpression(PVo VO) {
		if(VO instanceof AIdentifierVo) {
			interpretAtomicExpression((AIdentifierVo) VO);
		} else if(VO instanceof ANotVo) {
			interpretNotExpression((ANotVo) VO);
		} else if(VO instanceof AAndVo) {
			interpretAndExpression((AAndVo) VO);
		} else if(VO instanceof AOrVo) {
			interpretOrExpression((AOrVo) VO);
		} else if(VO instanceof AImpliesVo) {
			interpretImpliesExpression((AImpliesVo) VO);
		} else if(VO instanceof AEquivalentVo) {
			interpretEquivalentExpression((AEquivalentVo) VO);
		} else if(VO instanceof ASequentialVo) {
			interpretSequentialExpression((ASequentialVo) VO);
		} else {
			throw new RuntimeException("VO expression type is unknown: " + VO.getClass());
		}
	}

	public void interpretAtomicExpression(AIdentifierVo VO) {

	}

	public void interpretNotExpression(ANotVo VO) {

	}

	public void interpretAndExpression(AAndVo VO) {

	}

	public void interpretOrExpression(AOrVo VO) {

	}

	public void interpretImpliesExpression(AImpliesVo VO) {

	}

	public void interpretEquivalentExpression(AEquivalentVo VO) {

	}

	public void interpretSequentialExpression(ASequentialVo VO) {

	}


	public void checkVO(ValidationObligation validationObligation) {
		// checkVOExpression(validationObligation.getExpression());
		// TODO Implement full validation task syntax (not just conjunctions)
		for (IValidationTask validationTask : validationObligation.getTasks()) {
			if (validationTask != null && validationTask.getChecked() != Checked.SUCCESS) {
				checkVT(validationTask);
			}
		}
	}

	public void checkVT(IValidationTask validationTask) {
		if (validationTask instanceof ModelCheckingItem) {
			modelchecker.checkItem((ModelCheckingItem)validationTask, false, false);
		} else if (validationTask instanceof LTLFormulaItem) {
			ltlChecker.checkFormula((LTLFormulaItem)validationTask);
		} else if (validationTask instanceof SymbolicCheckingFormulaItem) {
			symbolicChecker.handleItem((SymbolicCheckingFormulaItem)validationTask, false);
		} else if (validationTask instanceof ReplayTrace) {
			traceChecker.check((ReplayTrace)validationTask, true);
		} else if (validationTask instanceof SimulationItem) {
			simulationItemHandler.checkItem((SimulationItem)validationTask, false);
		} else {
			throw new AssertionError("Unhandled validation task type: " + validationTask.getClass());
		}
	}

}
