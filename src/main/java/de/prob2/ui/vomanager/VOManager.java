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

	@Inject
	public VOManager(final CurrentProject currentProject, final Injector injector, final VOChecker voChecker) {
		this.currentProject = currentProject;
		this.injector = injector;
		this.voChecker = voChecker;
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

	private ValidationObligation linkValidationObligation(Object item) {
		ValidationTask validationTask;

		// TODO
		if(item instanceof ModelCheckingItem) {
			validationTask = new ValidationTask("MC", "machine", ValidationTechnique.MODEL_CHECKING, Arrays.asList(""), item);
		} else if(item instanceof LTLFormulaItem) {
			validationTask = new ValidationTask("LTL", "machine", ValidationTechnique.LTL_MODEL_CHECKING, Arrays.asList(""), item);
		} else if(item instanceof SymbolicCheckingFormulaItem) {
			validationTask = new ValidationTask("SMC", "machine", ValidationTechnique.SYMBOLIC_MODEL_CHECKING, Arrays.asList(""), item);
		} else if(item instanceof SimulationItem) {
			validationTask = new ValidationTask("SIM", "machine", ValidationTechnique.SIMULATION, Arrays.asList(""), item);
		} else if(item instanceof ReplayTrace) {
			validationTask = new ValidationTask("TR", "machine", ValidationTechnique.TRACE_REPLAY, Arrays.asList(""), item);
		} else {
			throw new RuntimeException("Validation item is not valid. Class is: " + item.getClass());
		}
		ValidationObligation validationObligation = new ValidationObligation(validationTask, extractConfiguration((IExecutableItem) item));
		updateExecutableInVO(validationObligation);
		return validationObligation;
	}

	public static String extractConfiguration(IExecutableItem item) {
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

	public void showPossibleLinkings(Menu linkItem, Requirement requirement) {
		linkItem.getItems().clear();
		List<Observable> dependentProperties = allValidationTasks();
		for(Observable observable : dependentProperties) {
			if(observable instanceof SetProperty) {
				((SetProperty<?>) observable).forEach(obj -> createLinkingItem(linkItem, requirement, obj));
			} else if(observable instanceof ListProperty) {
				((ListProperty<?>) observable).forEach(obj -> createLinkingItem(linkItem, requirement, obj));
			}
		}
	}

	private void createLinkingItem(Menu linkItem, Requirement requirement, Object voExecutable) {
		MenuItem voItem = new MenuItem(generateVOName(voExecutable));
		voItem.setMnemonicParsing(false);
		voItem.setOnAction(e -> {
			ValidationObligation validationObligation = linkValidationObligation(voExecutable);
			requirement.addValidationObligation(validationObligation);
			validationObligation.checkedProperty().addListener((o, from, to) -> requirement.updateChecked());
			voChecker.check(validationObligation);
		});
		linkItem.getItems().add(voItem);
	}

	private String generateVOName(Object item) {
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
	}

	/*public void updateLinkingListener(Menu linkItem, Requirement from, Requirement to, InvalidationListener linkingListener) {
		if (from != null) {
			List<Observable> dependentProperties = dependentPropertiesFromRequirement(from);
			for (Observable observable : dependentProperties) {
				observable.removeListener(linkingListener);
			}
		}

		if(to != null) {
			List<Observable> dependentProperties = dependentPropertiesFromRequirement(to);
			Observable[] dependentPropertiesAsArray = dependentProperties.toArray(new Observable[0]);
			BooleanBinding emptyProperty = Bindings.createBooleanBinding(() -> {
				boolean result = true;
				for(Observable observable : dependentPropertiesAsArray) {
					if(observable instanceof SetProperty) {
						result = result && ((SetProperty<?>) observable).emptyProperty().get();
					} else if(observable instanceof ListProperty) {
						result = result && ((ListProperty<?>) observable).emptyProperty().get();
					}
				}
				return result;
			}, dependentPropertiesAsArray);

			for (Observable observable : dependentProperties) {
				linkItem.disableProperty().bind(emptyProperty);
				observable.addListener(linkingListener);
			}

			linkingListener.invalidated(null);
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
}
