package de.prob2.ui.modelchecking;

import java.io.IOException;

import com.google.inject.Inject;

import de.prob.check.ConsistencyChecker;
import de.prob.check.ModelChecker;
import de.prob.check.ModelCheckingOptions;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ModelcheckingView extends AnchorPane {
	
	@FXML    private CheckBox findDeadlocks;
    @FXML    private CheckBox findInvViolations;
    @FXML    private CheckBox findBAViolations;
    @FXML    private CheckBox findGoal;
    @FXML    private CheckBox stopAtFullCoverage;
    @FXML    private CheckBox searchForNewErrors;

	private AnimationSelector animations;

	@Inject
	public ModelcheckingView(AnimationSelector ANIMATIONS, FXMLLoader loader) {
		this.animations = ANIMATIONS;
		try {
			loader.setLocation(getClass().getResource("modelchecking_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    @FXML
    void startModelCheck(ActionEvent event) {
    	if(animations.getCurrentTrace() == null) {
    		Alert alert = new Alert(AlertType.ERROR);
    		alert.setTitle("Specification file missing");
    		alert.setHeaderText("No specification file loaded. Cannot run model checker.");
    		alert.showAndWait();
    		return;
    	}
    	ModelCheckingOptions options = getOptions();
    	StateSpace currentStateSpace = animations.getCurrentTrace().getStateSpace();
    	ModelChecker checker = new ModelChecker(new ConsistencyChecker(
				currentStateSpace, options, null));
    	
		//jobs.put(checker.getJobId(), checker);
		checker.start();
//		AbstractElement main = currentStateSpace.getMainComponent();
//		String name = main == null ? "Model Check" : main.toString();
//		List<String> ss = new ArrayList<String>();
//		for (Options opts : options.getPrologOptions()) {
//			ss.add(opts.getDescription());
//		}
//		if (!ss.isEmpty()) {
//			name += " with " + Joiner.on(", ").join(ss);
//		}
    }

    private ModelCheckingOptions getOptions() {
		ModelCheckingOptions options = new ModelCheckingOptions();
		options = options.breadthFirst(true);
		options = options.checkDeadlocks(findDeadlocks.isSelected());
		options = options.checkInvariantViolations(findInvViolations.isSelected());
		options = options.checkAssertions(findBAViolations.isSelected());
		options = options.checkGoal(findGoal.isSelected());
		options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());
	
		return options;
	}

	@FXML
    void cancel(ActionEvent event) {
    	Stage stage = (Stage) this.getScene().getWindow();
    	stage.close();
    }
}
