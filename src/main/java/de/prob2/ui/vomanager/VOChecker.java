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
import de.prob2.ui.verifications.Checked;
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

	public void checkRequirement(Requirement requirement, Machine machine, VOManagerSetting setting) throws VOParseException{
		if(setting == VOManagerSetting.REQUIREMENT) {
			checkRequirementOnRequirementView(requirement);
		} else if(setting == VOManagerSetting.MACHINE) {
			checkRequirementOnMachineView(requirement, machine);
		}
		requirementHandler.updateChecked(currentProject.get(), machine, requirement, setting);
	}

	private void checkRequirementOnRequirementView(Requirement requirement) throws VOParseException {
		for(Machine machine : currentProject.getMachines()) {
			for (ValidationObligation validationObligation : machine.getValidationObligations()) {
				if(validationObligation.getRequirement().equals(requirement.getName())) {
					this.checkVO(validationObligation);
				}
			}
		}
	}

	private void checkRequirementOnMachineView(Requirement requirement, Machine machine) throws VOParseException {
		for (ValidationObligation validationObligation : machine.getValidationObligations()) {
			if(validationObligation.getRequirement().equals(requirement.getName())) {
				this.checkVO(validationObligation);
			}
		}
	}

	public void registerTask(String id, VTType type) {
		voParser.registerTask(id, type);
	}

	public void deregisterAllTasks() {
		voParser.getTasks().clear();
	}

	public void deregisterTask(String id) {
		voParser.deregisterTask(id);
	}

	public void parseAndCheckVOExpression(ValidationObligation vo, boolean check) throws VOParseException {
		vo.getTasks().clear();
		String expression = vo.getExpression();
		Start ast = voParser.parseFormula(expression);
		vo.setExpressionAst(ast.getPVo(), this);
		//voParser.semanticCheck(ast); // TODO
		parseAndCheckVOExpression(ast.getPVo(), vo, check);
	}

	private void parseAndCheckVOExpression(PVo ast, ValidationObligation vo, boolean check) {
		if(ast instanceof AIdentifierVo) {
			parseAndCheckAtomicExpression((AIdentifierVo) ast, vo, check);
		} else if(ast instanceof ANotVo) {
			parseAndCheckNotExpression((ANotVo) ast, vo, check);
		} else if(ast instanceof AAndVo) {
			parseAndCheckAndExpression((AAndVo) ast, vo, check);
		} else if(ast instanceof AOrVo) {
			parseAndCheckOrExpression((AOrVo) ast, vo, check);
		} else if(ast instanceof AImpliesVo) {
			parseAndCheckImpliesExpression((AImpliesVo) ast, vo, check);
		} else if(ast instanceof AEquivalentVo) {
			parseAndCheckEquivalentExpression((AEquivalentVo) ast, vo, check);
		} else if(ast instanceof ASequentialVo) {
			parseAndCheckSequentialExpression((ASequentialVo) ast, vo, check);
		} else {
			throw new RuntimeException("VO expression type is unknown: " + ast.getClass());
		}
	}

	private void parseAndCheckAtomicExpression(AIdentifierVo ast, ValidationObligation vo, boolean check) {
		Machine machine = currentProject.getCurrentMachine();
		IValidationTask validationTask;
		if (machine.getValidationTasks().containsKey(ast.getIdentifierLiteral().getText())) {
			validationTask = machine.getValidationTasks().get(ast.getIdentifierLiteral().getText());
		} else {
			validationTask = new ValidationTaskNotFound(ast.getIdentifierLiteral().getText());
		}
		vo.getTasks().add(validationTask);
		if(check) {
			checkVT(validationTask);
		}
	}

	private void parseAndCheckNotExpression(ANotVo ast, ValidationObligation vo, boolean check) {
		parseAndCheckVOExpression(ast.getVo(), vo, check);
	}

	private void parseAndCheckAndExpression(AAndVo ast, ValidationObligation vo, boolean check) {
		parseAndCheckVOExpression(ast.getLeft(), vo, check);
		parseAndCheckVOExpression(ast.getRight(), vo, check);
		// TODO: Implement short circuiting
	}

	private void parseAndCheckOrExpression(AOrVo ast, ValidationObligation vo, boolean check) {
		parseAndCheckVOExpression(ast.getLeft(), vo, check);
		parseAndCheckVOExpression(ast.getRight(), vo, check);
		// TODO: Implement short circuiting
	}

	private void parseAndCheckImpliesExpression(AImpliesVo ast, ValidationObligation vo, boolean check) {
		parseAndCheckOrExpression(new AOrVo(new ANotVo(ast.getLeft().clone()), ast.getRight().clone()), vo, check);
	}

	private void parseAndCheckEquivalentExpression(AEquivalentVo ast, ValidationObligation vo, boolean check) {
		parseAndCheckAndExpression(new AAndVo(new AImpliesVo(ast.getLeft().clone(), ast.getRight().clone()), new AImpliesVo(ast.getRight().clone(), ast.getLeft().clone())), vo, check);
	}

	private void parseAndCheckSequentialExpression(ASequentialVo ast, ValidationObligation vo, boolean check) {
		// TODO
	}


	public void checkVO(ValidationObligation validationObligation) throws VOParseException {
		parseAndCheckVOExpression(validationObligation, true);
	}


	public Checked updateVOExpression(PVo ast, ValidationObligation vo) {
		if(ast instanceof AIdentifierVo) {
			return updateAtomicExpression((AIdentifierVo) ast, vo);
		} else if(ast instanceof ANotVo) {
			return updateNotExpression((ANotVo) ast, vo);
		} else if(ast instanceof AAndVo) {
			return updateAndExpression((AAndVo) ast, vo);
		} else if(ast instanceof AOrVo) {
			return updateOrExpression((AOrVo) ast, vo);
		} else if(ast instanceof AImpliesVo) {
			return updateImpliesExpression((AImpliesVo) ast, vo);
		} else if(ast instanceof AEquivalentVo) {
			return updateEquivalentExpression((AEquivalentVo) ast, vo);
		} else if(ast instanceof ASequentialVo) {
			return updateSequentialExpression((ASequentialVo) ast, vo);
		} else {
			throw new RuntimeException("VO expression type is unknown: " + ast.getClass());
		}
	}

	public Checked updateAtomicExpression(AIdentifierVo ast, ValidationObligation vo) {
		Machine machine = currentProject.getCurrentMachine();
		if (machine.getValidationTasks().containsKey(ast.getIdentifierLiteral().getText())) {
			return machine.getValidationTasks().get(ast.getIdentifierLiteral().getText()).getChecked();
		} else {
			return Checked.PARSE_ERROR;
		}
	}

	public Checked updateNotExpression(ANotVo ast, ValidationObligation vo) {
		Checked exprRes = updateVOExpression(ast.getVo(), vo);
		if (exprRes == Checked.PARSE_ERROR) {
			return Checked.PARSE_ERROR;
		} else if (exprRes == Checked.SUCCESS) {
			return Checked.FAIL;
		} else if(exprRes == Checked.FAIL) {
			return Checked.SUCCESS;
		}
		return Checked.UNKNOWN;
	}

	public Checked updateAndExpression(AAndVo ast, ValidationObligation vo) {
		Checked leftRes = updateVOExpression(ast.getLeft(), vo);
		if (leftRes == Checked.PARSE_ERROR) {
			return Checked.PARSE_ERROR;
		} else if (leftRes == Checked.FAIL) {
			return Checked.FAIL;
		}
		Checked rightRes = updateVOExpression(ast.getRight(), vo);
		if (rightRes == Checked.PARSE_ERROR) {
			return Checked.PARSE_ERROR;
		} else if (rightRes == Checked.FAIL) {
			return Checked.FAIL;
		}
		if(leftRes == Checked.SUCCESS && rightRes == Checked.SUCCESS) {
			return Checked.SUCCESS;
		}
		return Checked.UNKNOWN;
	}

	public Checked updateOrExpression(AOrVo ast, ValidationObligation vo) {
		Checked leftRes = updateVOExpression(ast.getLeft(), vo);
		if (leftRes == Checked.PARSE_ERROR) {
			return Checked.PARSE_ERROR;
		} else if (leftRes == Checked.SUCCESS) {
			return Checked.SUCCESS;
		}
		Checked rightRes = updateVOExpression(ast.getRight(), vo);
		if (rightRes == Checked.PARSE_ERROR) {
			return Checked.PARSE_ERROR;
		} else if (rightRes == Checked.SUCCESS) {
			return Checked.SUCCESS;
		}
		if(leftRes == Checked.FAIL && rightRes == Checked.FAIL) {
			return Checked.FAIL;
		}
		return Checked.UNKNOWN;
	}

	public Checked updateImpliesExpression(AImpliesVo ast, ValidationObligation vo) {
		return updateOrExpression(new AOrVo(new ANotVo(ast.getLeft().clone()), ast.getRight().clone()), vo);
	}

	public Checked updateEquivalentExpression(AEquivalentVo ast, ValidationObligation vo) {
		return updateAndExpression(new AAndVo(new AImpliesVo(ast.getLeft().clone(), ast.getRight().clone()), new AImpliesVo(ast.getRight().clone(), ast.getLeft().clone())), vo);
	}

	public Checked updateSequentialExpression(ASequentialVo ast, ValidationObligation vo) {
		// TODO
		return Checked.UNKNOWN;
	}

	public void checkVT(IValidationTask validationTask) {
		// FIXME Currently ignores exceptions from CompletableFutures!
		// (Those normally should never happen, but still...)
		if (validationTask instanceof ValidationTaskNotFound) {
			// Nothing to be done - it already shows an error status
		} else if (validationTask instanceof ModelCheckingItem) {
			modelchecker.startCheckIfNeeded((ModelCheckingItem) validationTask);
		} else if (validationTask instanceof LTLFormulaItem) {
			ltlChecker.checkFormulaNoninteractive((LTLFormulaItem) validationTask);
		} else if (validationTask instanceof SymbolicCheckingFormulaItem) {
			symbolicChecker.handleItemNoninteractive((SymbolicCheckingFormulaItem) validationTask);
		} else if (validationTask instanceof ReplayTrace) {
			traceChecker.checkNoninteractive((ReplayTrace) validationTask);
		} else if (validationTask instanceof SimulationItem) {
			simulationItemHandler.checkItem((SimulationItem) validationTask);
		} else {
			throw new AssertionError("Unhandled validation task type: " + validationTask.getClass());
		}
	}


}
