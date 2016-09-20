package de.prob2.ui.modelchecking;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.check.ModelCheckingOptions;
import de.prob2.ui.prob2fx.CurrentStage;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

public class ModelcheckingStage extends Stage {

	@FXML
	private CheckBox findDeadlocks;
	@FXML
	private CheckBox findInvViolations;
	@FXML
	private CheckBox findBAViolations;
	@FXML
	private CheckBox findGoal;
	@FXML
	private CheckBox stopAtFullCoverage;
	@FXML
	private CheckBox searchForNewErrors;

	private CurrentTrace currentTrace;
	private ModelcheckingController modelcheckController;

	private Logger logger = LoggerFactory.getLogger(ModelcheckingStage.class);

	@Inject
	public ModelcheckingStage(
			CurrentTrace currentTrace,
			CurrentStage currentStage,
			FXMLLoader loader,
			ModelcheckingController modelcheckController) {
		this.currentTrace = currentTrace;
		this.modelcheckController = modelcheckController;
		try {
			loader.setLocation(getClass().getResource("modelchecking_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}

		currentStage.register(this);
	}

	@FXML
	void startModelCheck(ActionEvent event) {
		if (!currentTrace.exists()) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Specification file missing");
			alert.setHeaderText("No specification file loaded. Cannot run model checker.");
			alert.getDialogPane().getStylesheets().add("prob.css");
			alert.showAndWait();
			return;
		}
		modelcheckController.startModelchecking(getOptions(), currentTrace.getStateSpace());
	}

	private ModelCheckingOptions getOptions() {
		ModelCheckingOptions options = new ModelCheckingOptions();
		options = options.breadthFirst(true);
		options = options.checkDeadlocks(findDeadlocks.isSelected());
		options = options.checkInvariantViolations(findInvViolations.isSelected());
		options = options.checkAssertions(findBAViolations.isSelected());
		options = options.checkGoal(findGoal.isSelected());
		options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());
		options = options.recheckExisting(!searchForNewErrors.isSelected());

		return options;
	}

	@FXML
	void cancel(ActionEvent event) {
		modelcheckController.cancelModelchecking();
		Stage stage = (Stage) this.getScene().getWindow();
		stage.close();
	}

}
