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

	private final VOTaskCreator voTaskCreator;

	@Inject
	public VOManager(final CurrentProject currentProject, final Injector injector, final VOTaskCreator voTaskCreator) {
		this.currentProject = currentProject;
		this.injector = injector;
		this.voTaskCreator = voTaskCreator;
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
		return stream.filter(item -> voTaskCreator.extractParameters(item).equals(task.getParameters()))
				.findAny()
				.orElse(null);
	}

	private ValidationTask createValidationTask(Object item, Machine machine) {
		return new ValidationTask(machine.getName(), classToTechnique.get(item.getClass()), voTaskCreator.extractParameters(item), item);
	}

	public List<ValidationTask> allTasks(ValidationTechnique validationTechnique) {
		Machine machine = currentProject.getCurrentMachine();
		Stream<? extends IExecutableItem> stream = getExecutableStream(machine, validationTechnique);
		return stream.map(task -> createValidationTask(task, machine)).collect(Collectors.toList());
	}
}
