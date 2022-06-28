package de.prob2.ui.verifications.modelchecking;

import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingSearchStrategy;
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
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
public class ModelcheckingStage extends Stage {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelcheckingStage.class);

	private static final int INITIAL_NODES_LIMIT = 500000;

	private static final int INITIAL_NODES_STEP = 1000;

	private static final int INITIAL_TIME_LIMIT = 1;

	private static final int INITIAL_TIME_STEP = 1;

	@FXML
	private Button startButton;
	@FXML
	private ChoiceBox<ModelCheckingSearchStrategy> selectSearchStrategy;
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
	@FXML
	private TextField idTextField;
	@FXML
	private CheckBox additionalGoal;
	@FXML
	private TextField tfAdditionalGoal;
	
	private final ResourceBundle bundle;
	
	private final StageManager stageManager;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final Modelchecker modelchecker;

	private ModelCheckingItem lastItem;

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
		this.setResizable(true);
		this.initModality(Modality.APPLICATION_MODAL);
		this.startButton.disableProperty().bind(modelchecker.runningProperty());
		this.selectSearchStrategy.getItems().setAll(
			ModelCheckingSearchStrategy.MIXED_BF_DF,
			ModelCheckingSearchStrategy.BREADTH_FIRST,
			ModelCheckingSearchStrategy.DEPTH_FIRST
		);
		this.selectSearchStrategy.setValue(ModelCheckingSearchStrategy.MIXED_BF_DF);
		this.selectSearchStrategy.setConverter(new StringConverter<ModelCheckingSearchStrategy>() {
			@Override
			public String toString(final ModelCheckingSearchStrategy object) {
				final String key = getSearchStrategyNameKey(object);
				if (key != null) {
					return bundle.getString(key);
				} else {
					return object.toString();
				}
			}
			
			@Override
			public ModelCheckingSearchStrategy fromString(final String string) {
				throw new UnsupportedOperationException("Conversion from String to ModelCheckingSearchStrategy not supported");
			}
		});
		this.nodesLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1 , Integer.MAX_VALUE, INITIAL_NODES_LIMIT, INITIAL_NODES_STEP));
		this.nodesLimit.visibleProperty().bind(chooseNodesLimit.selectedProperty());
		this.timeLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1 , Integer.MAX_VALUE, INITIAL_TIME_LIMIT, INITIAL_TIME_STEP));
		this.timeLimit.visibleProperty().bind(chooseTimeLimit.selectedProperty());

		this.nodesLimit.getEditor().textProperty().addListener((observable, from, to) -> {
			try {
				nodesLimit.getValueFactory().setValue(Integer.parseInt(to));
			} catch (NumberFormatException e) {
				final Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, "", "verifications.modelchecking.modelcheckingStage.invalidInput");
				alert.initOwner(this);
				alert.showAndWait();
			}
		});
		this.timeLimit.getEditor().textProperty().addListener((observable, from, to) -> {
			try {
				timeLimit.getValueFactory().setValue(Integer.parseInt(to));
			} catch (NumberFormatException e) {
				final Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, "", "verifications.modelchecking.modelcheckingStage.invalidInput");
				alert.initOwner(this);
				alert.showAndWait();;
			}
		});
		this.tfAdditionalGoal.visibleProperty().bind(additionalGoal.selectedProperty());
	}

	public static String getSearchStrategyNameKey(final ModelCheckingSearchStrategy searchStrategy) {
		switch (searchStrategy) {
			case MIXED_BF_DF:
				return "verifications.modelchecking.modelcheckingStage.strategy.mixedBfDf";
			
			case BREADTH_FIRST:
				return "verifications.modelchecking.modelcheckingStage.strategy.breadthFirst";
			
			case DEPTH_FIRST:
				return "verifications.modelchecking.modelcheckingStage.strategy.depthFirst";
			
			default:
				return null;
		}
	}

	@FXML
	private void startModelCheck() {
		lastItem = null;
		if (currentTrace.get() != null) {
			final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
			final ModelCheckingSearchStrategy searchStrategy = selectSearchStrategy.getValue();
			Integer nLimit = chooseNodesLimit.isSelected() ? nodesLimit.getValue() : null;
			Integer tLimit = chooseTimeLimit.isSelected() ? timeLimit.getValue() : null;
			String goal = additionalGoal.isSelected() ? tfAdditionalGoal.getText() : null;
			ModelCheckingItem modelcheckingItem = new ModelCheckingItem(id, searchStrategy, nLimit, tLimit, goal, getOptions());
			if(currentProject.getCurrentMachine().getModelcheckingItems().stream().noneMatch(modelcheckingItem::settingsEqual)) {
				this.hide();
				modelchecker.checkItem(modelcheckingItem, true, false);
				currentProject.getCurrentMachine().getModelcheckingItems().add(modelcheckingItem);
				lastItem = modelcheckingItem;
			} else {
				ModelCheckingItem checkedItem = currentProject.getCurrentMachine().getModelcheckingItems().stream().filter(modelcheckingItem::settingsEqual).collect(Collectors.toList()).get(0);
				modelchecker.checkItem(checkedItem, true, false);
				lastItem = checkedItem;
				this.hide();
			}
		} else {
			final Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR, "",
					"verifications.modelchecking.modelcheckingStage.alerts.noMachineLoaded.content");
			alert.initOwner(this);
			alert.showAndWait();
			this.hide();
		}
	}
	
	private Set<ModelCheckingOptions.Options> getOptions() {
		ModelCheckingOptions options = new ModelCheckingOptions();
		options = options.checkDeadlocks(findDeadlocks.isSelected());
		options = options.checkInvariantViolations(findInvViolations.isSelected());
		options = options.checkAssertions(findBAViolations.isSelected());
		options = options.checkOtherErrors(findOtherErrors.isSelected());
		options = options.checkGoal(findGoal.isSelected());
		options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());
		return options.getPrologOptions();
	}

	@FXML
	private void cancel() {
		this.hide();
	}

	public ModelCheckingItem getLastItem() {
		return lastItem;
	}
	
}
