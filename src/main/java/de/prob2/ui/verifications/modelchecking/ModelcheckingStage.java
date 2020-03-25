package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.ModelCheckingOptions;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public class ModelcheckingStage extends Stage {
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
	
	private final ResourceBundle bundle;
	
	private final StageManager stageManager;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final Injector injector;

	@Inject
	private ModelcheckingStage(final StageManager stageManager, final ResourceBundle bundle, 
							final CurrentTrace currentTrace, final CurrentProject currentProject, final Injector injector) {
		this.bundle = bundle;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		stageManager.loadFXML(this, "modelchecking_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.initModality(Modality.APPLICATION_MODAL);
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
	}

	@FXML
	private void startModelCheck() {
		if (currentTrace.exists()) {
			ModelCheckingItem modelcheckingItem = new ModelCheckingItem(getOptions());
			if(!currentProject.getCurrentMachine().getModelcheckingItems().contains(modelcheckingItem)) {
				this.hide();
				injector.getInstance(Modelchecker.class).checkItem(modelcheckingItem, false);
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

	public void setDisableStart(final boolean disableStart) {
		Platform.runLater(() -> this.startButton.setDisable(disableStart));
	}
	
}
