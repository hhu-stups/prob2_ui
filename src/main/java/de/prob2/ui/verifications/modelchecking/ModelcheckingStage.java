package de.prob2.ui.verifications.modelchecking;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.check.ModelCheckingOptions;
import de.prob2.ui.internal.StageManager;
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

@Singleton
public class ModelcheckingStage extends Stage {
	
	public static enum SearchStrategy {
		MIXED_BF_DF("verifications.modelchecking.stage.strategy.items.mixedBfDf"),
		BREADTH_FIRST("verifications.modelchecking.stage.strategy.items.breadthFirst"),
		DEPTH_FIRST("verifications.modelchecking.stage.strategy.items.depthFirst"),
		//HEURISTIC_FUNCTION("verifications.modelchecking.stage.strategy.items.heuristicFunction"),
		//HASH_RANDOM("verifications.modelchecking.stage.strategy.items.hashRandom"),
		//RANDOM("verifications.modelchecking.stage.strategy.items.random"),
		//OUT_DEGREE("verifications.modelchecking.stage.strategy.items.outDegree"),
		//DISABLED_TRANSITIONS("verifications.modelchecking.stage.strategy.items.disabledTransitions"),
		;
		
		private final String name;
		
		SearchStrategy(final String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
	}
	
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
	private CheckBox findGoal;
	@FXML
	private CheckBox stopAtFullCoverage;
	
	private final ResourceBundle bundle;
	
	private final StageManager stageManager;
	
	private final CurrentTrace currentTrace;
	
	private final Injector injector;

	@Inject
	private ModelcheckingStage(final StageManager stageManager, final ResourceBundle bundle, 
							final CurrentTrace currentTrace, final Injector injector) {
		this.bundle = bundle;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
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
			injector.getInstance(ModelcheckingView.class).checkItem(getOptions(), selectSearchStrategy.getConverter(), selectSearchStrategy.getValue());
		} else {
			stageManager.makeAlert(Alert.AlertType.ERROR, bundle.getString("verifications.modelchecking.stage.noMachineLoaded"))
					.showAndWait();
			this.hide();
		}
	}
	

	
	private ModelCheckingOptions getOptions() {
		ModelCheckingOptions options = new ModelCheckingOptions();
		
		switch (selectSearchStrategy.getValue()) {
			case MIXED_BF_DF:
				break;
			case BREADTH_FIRST:
				options = options.breadthFirst(true);
				break;
			case DEPTH_FIRST:
				options = options.depthFirst(true);
				break;
			default:
				throw new IllegalArgumentException("Unhandled search strategy: " + selectSearchStrategy.getValue());
		}
		
		options = options.checkDeadlocks(findDeadlocks.isSelected());
		options = options.checkInvariantViolations(findInvViolations.isSelected());
		options = options.checkAssertions(findBAViolations.isSelected());
		options = options.checkGoal(findGoal.isSelected());
		options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());
		options = options.recheckExisting(true);
		return options;
	}

	@FXML
	private void cancel() {
		injector.getInstance(ModelcheckingView.class).cancelModelcheck();
		this.hide();
	}

	public void setDisableStart(final boolean disableStart) {
		Platform.runLater(() -> this.startButton.setDisable(disableStart));
	}
}