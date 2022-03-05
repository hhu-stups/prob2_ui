package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SetProperty;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class VOManager {

	private final CurrentProject currentProject;

	private final Injector injector;

	private final VOChecker voChecker;

	private final VOTaskCreator voTaskCreator;

	@Inject
	public VOManager(final CurrentProject currentProject, final Injector injector, final VOChecker voChecker, final VOTaskCreator voTaskCreator) {
		this.currentProject = currentProject;
		this.injector = injector;
		this.voChecker = voChecker;
		this.voTaskCreator = voTaskCreator;
	}


	public void synchronizeMachine(Machine machine) {
		for(Requirement requirement : machine.getRequirements()) {
			for(ValidationObligation validationObligation : requirement.validationObligationsProperty()) {
				ValidationTask validationTask = validationObligation.getTask();
				IExecutableItem executable = lookupExecutable(machine, validationTask, validationTask.getItem());
				validationTask.setExecutable(executable);
				validationObligation.checkedProperty().addListener((observable, from, to) -> requirement.updateChecked());
			}
			requirement.updateChecked();
		}
	}

	private IExecutableItem lookupExecutable(Machine machine, ValidationTask task, Object executableItem) {
		switch (task.getValidationTechnique()) {
			case MODEL_CHECKING:
				return machine.getModelcheckingItems().stream()
						.filter(item -> item.getOptions().equals(((ModelCheckingItem) executableItem).getOptions()))
						.findAny()
						.orElse(null);
			case LTL_MODEL_CHECKING:
				return machine.getLTLFormulas().stream()
						.filter(item -> item.settingsEqual((LTLFormulaItem) executableItem))
						.findAny()
						.orElse(null);
			case SYMBOLIC_MODEL_CHECKING:
				return machine.getSymbolicCheckingFormulas().stream()
						.filter(item -> item.settingsEqual((SymbolicCheckingFormulaItem) executableItem))
						.findAny()
						.orElse(null);
			case TRACE_REPLAY:
				return injector.getInstance(TraceViewHandler.class).getTraces().stream()
						.filter(item -> item.getLocation().toString().equals(executableItem))
						.findAny()
						.orElse(null);
			case SIMULATION:
				return machine.getSimulations().stream()
						.filter(item -> item.equals(executableItem))
						.findAny()
						.orElse(null);
			default:
				throw new RuntimeException("Validation task is not valid: " + task);
		}
	}

	private ValidationTask createValidationTask(Object item) {
		ValidationTask validationTask;
		if(item instanceof ModelCheckingItem) {
			validationTask = new ValidationTask("MC", "machine", ValidationTechnique.MODEL_CHECKING, voTaskCreator.extractParameters(item), item);
		} else if(item instanceof LTLFormulaItem) {
			validationTask = new ValidationTask("LTL", "machine", ValidationTechnique.LTL_MODEL_CHECKING, voTaskCreator.extractParameters(item), item);
		} else if(item instanceof SymbolicCheckingFormulaItem) {
			validationTask = new ValidationTask("SMC", "machine", ValidationTechnique.SYMBOLIC_MODEL_CHECKING, voTaskCreator.extractParameters(item), item);
		} else if(item instanceof SimulationItem) {
			validationTask = new ValidationTask("SIM", "machine", ValidationTechnique.SIMULATION, voTaskCreator.extractParameters(item), item);
		} else if(item instanceof ReplayTrace) {
			validationTask = new ValidationTask("TR", "machine", ValidationTechnique.TRACE_REPLAY, voTaskCreator.extractParameters(item), item);
		} else {
			throw new RuntimeException("Validation item is not valid. Class is: " + item.getClass());
		}
		return validationTask;
	}

	private void updateExecutableInVO(ValidationObligation validationObligation) {
		ValidationTask validationTask = validationObligation.getTask();
		switch (validationTask.getValidationTechnique()) {
			case MODEL_CHECKING:
			case LTL_MODEL_CHECKING:
			case SYMBOLIC_MODEL_CHECKING:
			case SIMULATION:
				validationTask.setExecutable((IExecutableItem) validationTask.getItem());
				break;
			case TRACE_REPLAY:
				validationTask.setExecutable(injector.getInstance(TraceViewHandler.class).getTraces().stream()
						.filter(item -> item.getLocation().toString().equals(((ReplayTrace) validationTask.getItem()).getLocation().toString()))
						.findAny()
						.orElse(null));
				break;
			default:
				throw new RuntimeException("Validation task is invalid: " + validationTask.getValidationTechnique());
		}
	}

	/*private String generateVOName(Object item) {
		if(item instanceof ModelCheckingItem) {
			return String.format("MC(%s)", extractConfiguration((IExecutableItem) item));
		} else if(item instanceof LTLFormulaItem) {
			return String.format("LTL(%s)", extractConfiguration((IExecutableItem) item));
		} else if(item instanceof SymbolicCheckingFormulaItem) {
			return String.format("SMC(%s)", extractConfiguration((IExecutableItem) item));
		} else if(item instanceof ReplayTrace) {
			return String.format("TR(%s)", extractConfiguration((IExecutableItem) item));
		} else if(item instanceof SimulationItem) {
			return String.format("SIM(%s)", extractConfiguration((IExecutableItem) item));
		} else {
			throw new RuntimeException("Validation item is not valid. Class is: " + item.getClass());
		}
	}*/

	private List<Observable> allValidationTasks() {
		List<Observable> lists = new ArrayList<>();
		Machine machine = currentProject.getCurrentMachine();
		lists.add(machine.modelcheckingItemsProperty());
		lists.add(machine.ltlFormulasProperty());
		lists.add(machine.symbolicCheckingFormulasProperty());
		lists.add(injector.getInstance(TraceViewHandler.class).getTraces());
		lists.add(machine.simulationItemsProperty());
		return lists;
	}

	public List<ValidationTask> allTasks(ValidationTechnique validationTechnique) {
		List<ValidationTask> executableItems = new ArrayList<>();
		Machine machine = currentProject.getCurrentMachine();
		switch (validationTechnique) {
			case MODEL_CHECKING:
				executableItems.addAll(machine.getModelcheckingItems().stream().map(this::createValidationTask).collect(Collectors.toList()));
				break;
			case LTL_MODEL_CHECKING:
				executableItems.addAll(machine.getLTLFormulas().stream().map(this::createValidationTask).collect(Collectors.toList()));
				break;
			case SYMBOLIC_MODEL_CHECKING:
				executableItems.addAll(machine.getSymbolicCheckingFormulas().stream().map(this::createValidationTask).collect(Collectors.toList()));
				break;
			case TRACE_REPLAY:
				executableItems.addAll(injector.getInstance(TraceViewHandler.class).getTraces().stream().map(this::createValidationTask).collect(Collectors.toList()));
				break;
			case SIMULATION:
				executableItems.addAll(machine.getSimulations().stream().map(this::createValidationTask).collect(Collectors.toList()));
				break;
			case PARALLEL:
			case SEQUENTIAL:
				break;
			default:
				throw new RuntimeException("Validation technique not valid: " + validationTechnique);
		}
		return executableItems;
	}
}
