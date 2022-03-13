package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.animation.tracereplay.TraceSaver;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.simulation.choice.SimulationChoosingStage;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaStage;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.ModelcheckingStage;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingChoosingStage;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SetProperty;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class VOManager {

	private static final Map<Class<? extends IExecutableItem>, ValidationTechnique> classToTechnique = new HashMap<>();

	static {
		classToTechnique.put(ModelCheckingItem.class, ValidationTechnique.MODEL_CHECKING);
		classToTechnique.put(LTLFormulaItem.class, ValidationTechnique.LTL_MODEL_CHECKING);
		classToTechnique.put(SymbolicCheckingFormulaItem.class, ValidationTechnique.SYMBOLIC_MODEL_CHECKING);
		classToTechnique.put(SimulationItem.class, ValidationTechnique.SIMULATION);
		classToTechnique.put(ReplayTrace.class, ValidationTechnique.TRACE_REPLAY);
	}

	private final CurrentProject currentProject;

	private final Injector injector;

	@Inject
	public VOManager(final CurrentProject currentProject, final Injector injector) {
		this.currentProject = currentProject;
		this.injector = injector;
	}


	public void synchronizeMachine(Machine machine) {
		for(ValidationTask validationTask : machine.getValidationTasks()) {
			IExecutableItem executable = lookupExecutable(machine, validationTask);
			validationTask.setExecutable(executable);
			validationTask.checkedProperty().bind(executable.checkedProperty());
		}
	}


	private Stream<? extends IExecutableItem> getExecutableStream(Machine machine, ValidationTechnique validationTechnique) {
		switch (validationTechnique) {
			case MODEL_CHECKING:
				return machine.getModelcheckingItems().stream();
			case LTL_MODEL_CHECKING:
				return machine.getLTLFormulas().stream();
			case SYMBOLIC_MODEL_CHECKING:
				return machine.getSymbolicCheckingFormulas().stream();
			case TRACE_REPLAY:
				return injector.getInstance(TraceViewHandler.class).getTraces().stream();
			case SIMULATION:
				return machine.getSimulations().stream();
			default:
				throw new RuntimeException("Validation technique is not valid: " + validationTechnique);
		}
	}

	private IExecutableItem lookupExecutable(Machine machine, ValidationTask task) {
		Stream<? extends IExecutableItem> stream = getExecutableStream(machine, task.getValidationTechnique());
		return stream.filter(item -> extractParameters(item).equals(task.getParameters()))
				.findAny()
				.orElse(null);
	}

	private ValidationTask createValidationTask(Object item, Machine machine) {
		return new ValidationTask(machine.getName(), classToTechnique.get(item.getClass()), extractParameters(item), item);
	}

	public List<ValidationTask> allTasks(ValidationTechnique validationTechnique) {
		Machine machine = currentProject.getCurrentMachine();
		Stream<? extends IExecutableItem> stream = getExecutableStream(machine, validationTechnique);
		return stream.map(task -> createValidationTask(task, machine)).collect(Collectors.toList());
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
