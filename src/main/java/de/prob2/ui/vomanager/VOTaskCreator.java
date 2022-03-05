package de.prob2.ui.vomanager;


import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.animation.tracereplay.TraceSaver;
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

@Singleton
public class VOTaskCreator {

	private final Injector injector;

	@Inject
	public VOTaskCreator(final Injector injector) {
		this.injector = injector;
	}

	public ValidationTask openTaskWindow(Window currentWindow, Requirement requirement, ValidationTechnique validationTechnique) {
		switch (validationTechnique) {
			case MODEL_CHECKING: {
				ModelcheckingStage stageController = injector.getInstance(ModelcheckingStage.class);
				//stageController.linkRequirement(requirement);
				stageController.showAndWait();
				ModelCheckingItem item = stageController.getLastItem();
				if(item == null) {
					return null;
				}
				ValidationTask validationTask = new ValidationTask("MC", "machine", validationTechnique, Arrays.asList(""), item);
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
				ValidationTask validationTask = new ValidationTask("LTL", "machine", validationTechnique, Arrays.asList(""), item);
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
				ValidationTask validationTask = new ValidationTask("SMC", "machine", validationTechnique, Arrays.asList(""), item);
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
				ValidationTask validationTask = new ValidationTask("TR", "machine", validationTechnique, Arrays.asList(""), replayTrace);
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
				ValidationTask validationTask = new ValidationTask("SIM", "machine", validationTechnique, Arrays.asList(""), item);
				validationTask.setExecutable(item);
				return validationTask;
			}
			default:
				throw new RuntimeException("Validation task is not valid");
		}
	}

}
