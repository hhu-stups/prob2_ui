package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.check.ModelCheckingOptions;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public class ModelcheckingStage extends Stage {

	private static final int INITIAL_NODES_LIMIT = 500000;

	private static final int INITIAL_NODES_STEP = 1000;

	private static final int INITIAL_TIME_LIMIT = 1;

	private static final int INITIAL_TIME_STEP = 1;

	@FXML
	private Button startButton;
	@FXML
	private ChoiceBox<SearchStrategy> selectSearchStrategy;
	@FXML
	private CheckBox findDeadlocks;
	@FXML
	private CheckBox findInvViolations;
	@FXML
	private CheckBox findBAViolations;
	@FXML
	private CheckBox findOtherErrors;
	@FXML
	private CheckBox findGoal;
	@FXML
	private CheckBox stopAtFullCoverage;
	@FXML
	private CheckBox chooseNodesLimit;
	@FXML
	private CheckBox chooseTimeLimit;
	@FXML
	private Spinner<Integer> nodesLimit;
	@FXML
	private Spinner<Integer> timeLimit;
	
	private final ResourceBundle bundle;
	
	private final StageManager stageManager;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final Modelchecker modelchecker;

	@Inject
	private ModelcheckingStage(final StageManager stageManager, final ResourceBundle bundle, 
							final CurrentTrace currentTrace, final CurrentProject currentProject, final Modelchecker modelchecker) {
		this.bundle = bundle;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.modelchecker = modelchecker;
		stageManager.loadFXML(this, "modelchecking_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.initModality(Modality.APPLICATION_MODAL);
		this.startButton.disableProperty().bind(modelchecker.runningProperty());
		this.selectSearchStrategy.getItems().setAll(SearchStrategy.values());
		this.selectSearchStrategy.setValue(SearchStrategy.MIXED_BF_DF);
		this.selectSearchStrategy.setConverter(new StringConverter<SearchStrategy>() {
			@Override
			public String toString(final SearchStrategy object) {
				return bundle.getString(object.getName());
			}
			
			@Override
			public SearchStrategy fromString(final String string) {
				throw new UnsupportedOperationException("Conversion from String to SearchStrategy not supported");
			}
		});
		this.nodesLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1 , Integer.MAX_VALUE, INITIAL_NODES_LIMIT, INITIAL_NODES_STEP));
		this.nodesLimit.visibleProperty().bind(chooseNodesLimit.selectedProperty());
		this.timeLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1 , Integer.MAX_VALUE, INITIAL_TIME_LIMIT, INITIAL_TIME_STEP));
		this.timeLimit.visibleProperty().bind(chooseTimeLimit.selectedProperty());
	}

	@FXML
	private void startModelCheck() {
		if (currentTrace.get() != null) {
			String nLimit = chooseNodesLimit.isSelected() ? String.valueOf(nodesLimit.getValue()) : "-";
			String tLimit = chooseTimeLimit.isSelected() ? String.valueOf(timeLimit.getValue()) : "-";
			ModelCheckingItem modelcheckingItem = new ModelCheckingItem(nLimit, tLimit, getOptions());
			if(currentProject.getCurrentMachine().getModelcheckingItems().stream().noneMatch(modelcheckingItem::settingsEqual)) {
				this.hide();
				modelchecker.checkItem(modelcheckingItem, true, false);
				currentProject.getCurrentMachine().getModelcheckingItems().add(modelcheckingItem);
			} else {
				stageManager.makeAlert(Alert.AlertType.WARNING, "", "verifications.modelchecking.modelcheckingStage.strategy.alreadyChecked").showAndWait();
				this.hide();
			}
		} else {
			stageManager.makeAlert(Alert.AlertType.ERROR, "",
					"verifications.modelchecking.modelcheckingStage.alerts.noMachineLoaded.content")
					.showAndWait();
			this.hide();
		}
	}
	

	
	private ModelCheckingOptions getOptions() {
		ModelCheckingOptions options = new ModelCheckingOptions();
		options = selectSearchStrategy.getValue().toOptions(options);
		options = options.checkDeadlocks(findDeadlocks.isSelected());
		options = options.checkInvariantViolations(findInvViolations.isSelected());
		options = options.checkAssertions(findBAViolations.isSelected());
		options = options.checkOtherErrors(findOtherErrors.isSelected());
		options = options.checkGoal(findGoal.isSelected());
		options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());
		options = options.recheckExisting(true); // TO DO: set to false if we want new problems; currently seems to have no influence on prob2_interface calls; currently the first call always contains inspect_existing_nodes as an option, e.g., do_modelchecking(500,[find_deadlocks,find_invariant_violations,inspect_existing_nodes],Result,Stats)
		return options;
	}

	@FXML
	private void cancel() {
		this.hide();
	}
}
