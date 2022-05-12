package de.prob2.ui.vomanager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceSaver;
import de.prob2.ui.project.Project;
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

import javafx.stage.Window;

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

	private final Injector injector;

	@Inject
	public VOManager(final Injector injector) {
		this.injector = injector;
	}


	public void synchronizeProject(Project project) {
		for(Machine machine : project.getMachines()) {
			for (ValidationTask validationTask : machine.getValidationTasks()) {
				IExecutableItem executable = lookupExecutable(machine, validationTask);
				validationTask.setExecutable(executable);
				validationTask.checkedProperty().bind(executable.checkedProperty());
			}
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
				return injector.getInstance(TraceViewHandler.class).getMachinesToTraces().get(machine).stream();
			case SIMULATION:
				return machine.getSimulations().stream().flatMap(model -> model.getSimulationItems().stream());
			default:
				throw new RuntimeException("Validation technique is not valid: " + validationTechnique);
		}
	}

	public List<IExecutableItem> getExecutables(Machine machine, ValidationTechnique validationTechnique) {
		return this.getExecutableStream(machine, validationTechnique).collect(Collectors.toList());
	}

	private IExecutableItem lookupExecutable(Machine machine, ValidationTask task) {
		Stream<? extends IExecutableItem> stream = getExecutableStream(machine, task.getValidationTechnique());
		return stream.filter(item -> extractParameters(item).equals(task.getParameters()))
				.findAny()
				.orElse(null);
	}

	// Remark: Will eventually be used to create ProB2-UI tasks from UI directly
	@Deprecated
	public ValidationTask openTaskWindow(Window currentWindow, Machine machine, Requirement requirement, ValidationTechnique validationTechnique) {
		IExecutableItem item;
		switch (validationTechnique) {
			case MODEL_CHECKING: {
				ModelcheckingStage stageController = injector.getInstance(ModelcheckingStage.class);
				//stageController.linkRequirement(requirement);
				stageController.showAndWait();
				item = stageController.getLastItem();
				break;
			}
			case LTL_MODEL_CHECKING: {
				LTLFormulaStage formulaStage = injector.getInstance(LTLFormulaStage.class);
				formulaStage.linkRequirement(requirement);
				formulaStage.setHandleItem(new LTLHandleItem<>(LTLHandleItem.HandleType.ADD, null));
				formulaStage.showAndWait();
				item = formulaStage.getLastItem();
				break;
			}
			case SYMBOLIC_MODEL_CHECKING: {
				SymbolicCheckingChoosingStage symbolicStage = injector.getInstance(SymbolicCheckingChoosingStage.class);
				symbolicStage.showAndWait();
				item = symbolicStage.getLastItem();
				break;
			}
			case TRACE_REPLAY: {
				TraceSaver traceSaver = injector.getInstance(TraceSaver.class);
				traceSaver.saveTrace(currentWindow);
				item = injector.getInstance(TraceViewHandler.class).getLastTrace();
				break;
			}
			case SIMULATION: {
				SimulationChoosingStage simulationChoosingStage = injector.getInstance(SimulationChoosingStage.class);
				simulationChoosingStage.showAndWait();
				item = simulationChoosingStage.getLastItem();
				break;
			}
			default:
				throw new RuntimeException("Validation task is not valid");
		}
		if(item == null) {
			return null;
		}
		return new ValidationTask(validationTechnique.getId(), machine.getName(), validationTechnique, extractParameters(item), item);
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

	public boolean requirementIsValid(String name, String text) {
		//isBlank() requires Java version >= 11
		String nameWithoutWhiteSpaces = name.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		String textWithoutWhiteSpaces = text.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		return nameWithoutWhiteSpaces.length() > 0 && textWithoutWhiteSpaces.length() > 0;
	}

	public boolean taskIsValid(String name) {
		//isBlank() requires Java version >= 11
		String nameWithoutWhiteSpaces = name.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		return nameWithoutWhiteSpaces.length() > 0;
	}

	public boolean voIsValid(String name, Requirement requirement) {
		//isBlank() requires Java version >= 11
		if(requirement == null) {
			return false;
		}
		String nameWithoutWhiteSpaces = name.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		return nameWithoutWhiteSpaces.length() > 0;
	}

}
