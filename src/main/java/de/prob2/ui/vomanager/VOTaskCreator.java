package de.prob2.ui.vomanager;


import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.animation.tracereplay.TraceSaver;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.simulation.choice.SimulationChoosingStage;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaStage;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.ModelcheckingStage;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingChoosingStage;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;

import javafx.stage.Window;

import java.util.Arrays;
import java.util.stream.Collectors;

@Singleton
public class VOTaskCreator {

	private final Injector injector;

	@Inject
	public VOTaskCreator(final Injector injector) {
		this.injector = injector;
	}

	// Remark: Will eventually be used to create ProB2-UI tasks from UI directly
	@Deprecated
	public ValidationTask openTaskWindow(Window currentWindow, Machine machine, Requirement requirement, ValidationTechnique validationTechnique) {
		switch (validationTechnique) {
			case MODEL_CHECKING: {
				ModelcheckingStage stageController = injector.getInstance(ModelcheckingStage.class);
				//stageController.linkRequirement(requirement);
				stageController.showAndWait();
				ModelCheckingItem item = stageController.getLastItem();
				if(item == null) {
					return null;
				}
				ValidationTask validationTask = new ValidationTask("MC", machine.getName(), validationTechnique, extractParameters(item), item);
				validationTask.setExecutable(item);
				return validationTask;
			}
			case LTL_MODEL_CHECKING: {
				LTLFormulaStage formulaStage = injector.getInstance(LTLFormulaStage.class);
				formulaStage.linkRequirement(requirement);
				formulaStage.setHandleItem(new LTLHandleItem<>(LTLHandleItem.HandleType.ADD, null));
				formulaStage.showAndWait();
				LTLFormulaItem item = formulaStage.getLastItem();
				if(item == null) {
					return null;
				}
				ValidationTask validationTask = new ValidationTask("LTL", machine.getName(), validationTechnique, extractParameters(item), item);
				validationTask.setExecutable(item);
				return validationTask;
			}
			case SYMBOLIC_MODEL_CHECKING: {
				SymbolicCheckingChoosingStage symbolicStage = injector.getInstance(SymbolicCheckingChoosingStage.class);
				symbolicStage.showAndWait();
				SymbolicCheckingFormulaItem item = symbolicStage.getLastItem();
				if (item == null) {
					return null;
				}
				ValidationTask validationTask = new ValidationTask("SMC", machine.getName(), validationTechnique, extractParameters(item), item);
				validationTask.setExecutable(item);
				return validationTask;
			}
			case TRACE_REPLAY: {
				TraceSaver traceSaver = injector.getInstance(TraceSaver.class);
				traceSaver.saveTrace(currentWindow, TraceReplayErrorAlert.Trigger.TRIGGER_HISTORY_VIEW);
				ReplayTrace replayTrace = injector.getInstance(TraceViewHandler.class).getLastTrace();
				if (replayTrace == null) {
					return null;
				}
				ValidationTask validationTask = new ValidationTask("TR", machine.getName(), validationTechnique, extractParameters(replayTrace), replayTrace);
				validationTask.setExecutable(replayTrace);
				return validationTask;
			}
			case SIMULATION: {
				SimulationChoosingStage simulationChoosingStage = injector.getInstance(SimulationChoosingStage.class);
				simulationChoosingStage.showAndWait();
				SimulationItem item = simulationChoosingStage.getLastItem();
				if (item == null) {
					return null;
				}
				ValidationTask validationTask = new ValidationTask("SIM", machine.getName(), validationTechnique, extractParameters(item), item);
				validationTask.setExecutable(item);
				return validationTask;
			}
			default:
				throw new RuntimeException("Validation task is not valid");
		}
	}

	public String extractParameters(Object item) {
		if(item instanceof ModelCheckingItem) {
			return ((ModelCheckingItem) item).getOptions().getPrologOptions().stream().map(Enum::toString).collect(Collectors.joining(", "));
		} else if(item instanceof LTLFormulaItem) {
			return ((LTLFormulaItem) item).getCode();
		} else if(item instanceof SymbolicCheckingFormulaItem) {
			SymbolicCheckingFormulaItem symbolicItem = ((SymbolicCheckingFormulaItem) item);
			switch (symbolicItem.getType()) {
				case INVARIANT:
					if (symbolicItem.getCode().isEmpty()) {
						return String.format("%s(%s)", symbolicItem.getType().name(), "all");
					} else {
						return String.format("%s(%s)", symbolicItem.getType().name(), symbolicItem.getCode());
					}
				case DEADLOCK:
					return String.format("%s(%s)", symbolicItem.getType().name(), symbolicItem.getCode());
				case SYMBOLIC_MODEL_CHECK:
					return symbolicItem.getCode();
				case CHECK_DYNAMIC_ASSERTIONS:
				case CHECK_STATIC_ASSERTIONS:
				case CHECK_REFINEMENT:
				case CHECK_WELL_DEFINEDNESS:
				case FIND_REDUNDANT_INVARIANTS:
				default:
					return symbolicItem.getType().name();
			}
		} else if(item instanceof SimulationItem) {
			return ((SimulationItem) item).getConfiguration();
		} else if(item instanceof ReplayTrace) {
			return ((ReplayTrace) item).getName();
		} else {
			throw new RuntimeException("Class for extracting configuration is invalid: " + item.getClass());
		}
	}

}
