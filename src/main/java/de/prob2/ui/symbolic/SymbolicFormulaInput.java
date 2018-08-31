package de.prob2.ui.symbolic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.statespace.LoadedMachine;
import de.prob2.ui.internal.PredicateBuilderView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public abstract class SymbolicFormulaInput extends VBox {
	
	protected final CurrentProject currentProject;
	
	@FXML
	protected Button btAdd;
	
	@FXML
	protected Button btCheck;
	
	@FXML
	protected TextField tfFormula;
	
	@FXML
	protected ChoiceBox<String> cbOperations;
	
	@FXML
	protected PredicateBuilderView predicateBuilderView;
	
	protected final Injector injector;
	
	protected final ResourceBundle bundle;
	
	protected final CurrentTrace currentTrace;
	
	protected ArrayList<String> events;
	
	@Inject
	public SymbolicFormulaInput(final StageManager stageManager, final CurrentProject currentProject, 
								final Injector injector, final ResourceBundle bundle,
								final CurrentTrace currentTrace) {
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.events = new ArrayList<>();
		this.injector = injector;
		this.bundle = bundle;
	}

	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
		setCheckListeners();
	}
	
	protected void update() {
		events.clear();
		final Map<String, String> items = new LinkedHashMap<>();
		if (currentTrace.get() != null) {
			final LoadedMachine loadedMachine = currentTrace.getStateSpace().getLoadedMachine();
			if (loadedMachine != null) {
				events.addAll(loadedMachine.getOperationNames());
				loadedMachine.getConstantNames().forEach(s -> items.put(s, ""));
				loadedMachine.getVariableNames().forEach(s -> items.put(s, ""));
			}
		}
		cbOperations.getItems().setAll(events);
		predicateBuilderView.setItems(items);
	}
	
	protected abstract void setCheckListeners();
	
	public void changeGUIType(final SymbolicGUIType guiType) {
		this.getChildren().removeAll(tfFormula, cbOperations, predicateBuilderView);
		switch (guiType) {
			case NONE:
				break;
			case TEXT_FIELD:
				this.getChildren().add(0, tfFormula);
				break;
			case CHOICE_BOX:
				this.getChildren().add(0, cbOperations);
				break;
			
			case PREDICATE:
				this.getChildren().add(0, predicateBuilderView);
				break;
			default:
				throw new AssertionError("Unhandled GUI type: " + guiType);
		}
	}
	
	public void reset() {
		btAdd.setText(bundle.getString("common.buttons.add"));
		btCheck.setText(bundle.getString("verifications.symbolicchecking.formulaInput.buttons.addAndCheck"));
		setCheckListeners();
		tfFormula.clear();
		cbOperations.getSelectionModel().clearSelection();
	}

}
