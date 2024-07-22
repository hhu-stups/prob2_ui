package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Inject;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingSearchStrategy;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.TranslatableAdapter;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Set;

@FXMLInjected
public class ProBModelCheckingTab extends Tab {

	private static final int INITIAL_NODES_LIMIT = 500000;

	private static final int INITIAL_NODES_STEP = 1000;

	private static final int INITIAL_TIME_LIMIT = 1;

	private static final int INITIAL_TIME_STEP = 1;

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
	private CheckBox findGoal;
	@FXML
	private TextField tfFindGoal;

	private final I18n i18n;

	private final StageManager stageManager;

	@Inject
	private ProBModelCheckingTab(final StageManager stageManager, final I18n i18n) {
		this.i18n = i18n;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "prob_modelchecking_tab.fxml");
	}

	@FXML
	private void initialize() {
		this.selectSearchStrategy.getItems().setAll(
			ModelCheckingSearchStrategy.MIXED_BF_DF,
			ModelCheckingSearchStrategy.BREADTH_FIRST,
			ModelCheckingSearchStrategy.DEPTH_FIRST
		);
		this.selectSearchStrategy.setValue(ModelCheckingSearchStrategy.MIXED_BF_DF);
		this.selectSearchStrategy.setConverter(i18n.translateConverter(TranslatableAdapter.adapter(ProBModelCheckingTab::getSearchStrategyNameKey)));

		this.nodesLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1 , Integer.MAX_VALUE, INITIAL_NODES_LIMIT, INITIAL_NODES_STEP));
		this.nodesLimit.visibleProperty().bind(chooseNodesLimit.selectedProperty());
		this.timeLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1 , Integer.MAX_VALUE, INITIAL_TIME_LIMIT, INITIAL_TIME_STEP));
		this.timeLimit.visibleProperty().bind(chooseTimeLimit.selectedProperty());

		this.nodesLimit.getEditor().textProperty().addListener((observable, from, to) -> {
			try {
				nodesLimit.getValueFactory().setValue(Integer.parseInt(to));
			} catch (NumberFormatException e) {
				final Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, "", "verifications.modelchecking.modelcheckingStage.invalidInput");
				alert.initOwner(stageManager.getCurrent());
				alert.showAndWait();
			}
		});
		this.timeLimit.getEditor().textProperty().addListener((observable, from, to) -> {
			try {
				timeLimit.getValueFactory().setValue(Integer.parseInt(to));
			} catch (NumberFormatException e) {
				final Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, "", "verifications.modelchecking.modelcheckingStage.invalidInput");
				alert.initOwner(stageManager.getCurrent());
				alert.showAndWait();
			}
		});
		this.tfFindGoal.visibleProperty().bind(findGoal.selectedProperty());
	}

	public static String getSearchStrategyNameKey(final ModelCheckingSearchStrategy searchStrategy) {
		return switch (searchStrategy) {
			case MIXED_BF_DF -> "verifications.modelchecking.modelcheckingStage.strategy.mixedBfDf";
			case BREADTH_FIRST -> "verifications.modelchecking.modelcheckingStage.strategy.breadthFirst";
			case DEPTH_FIRST -> "verifications.modelchecking.modelcheckingStage.strategy.depthFirst";
			default -> null;
		};
	}

	ModelCheckingItem startModelCheck(final String id) {
		ModelCheckingSearchStrategy searchStrategy = selectSearchStrategy.getValue();
		Integer nLimit = chooseNodesLimit.isSelected() ? nodesLimit.getValue() : null;
		Integer tLimit = chooseTimeLimit.isSelected() ? timeLimit.getValue() : null;
		String goal = findGoal.isSelected() ? tfFindGoal.getText() : null;
		return new ProBModelCheckingItem(id, searchStrategy, nLimit, tLimit, goal, getOptions("GOAL".equals(goal)));
	}

	private Set<ModelCheckingOptions.Options> getOptions(boolean goal) {
		ModelCheckingOptions options = new ModelCheckingOptions();
		options = options.checkDeadlocks(findDeadlocks.isSelected());
		options = options.checkInvariantViolations(findInvViolations.isSelected());
		options = options.checkAssertions(findBAViolations.isSelected());
		options = options.checkOtherErrors(findOtherErrors.isSelected());
		options = options.checkGoal(goal);
		options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());
		return options.getPrologOptions();
	}

	public void setData(final ProBModelCheckingItem item) {
		selectSearchStrategy.setValue(item.getSearchStrategy());

		findDeadlocks.setSelected(item.getOptions().contains(ModelCheckingOptions.Options.FIND_DEADLOCKS));
		findInvViolations.setSelected(item.getOptions().contains(ModelCheckingOptions.Options.FIND_INVARIANT_VIOLATIONS));
		findBAViolations.setSelected(item.getOptions().contains(ModelCheckingOptions.Options.FIND_ASSERTION_VIOLATIONS));
		stopAtFullCoverage.setSelected(item.getOptions().contains(ModelCheckingOptions.Options.STOP_AT_FULL_COVERAGE));

		findOtherErrors.setSelected(!item.getOptions().contains(ModelCheckingOptions.Options.IGNORE_OTHER_ERRORS));

		if (item.getNodesLimit() != null){
			chooseNodesLimit.setSelected(true);
			nodesLimit.getValueFactory().setValue(item.getNodesLimit());
		} else {
			chooseNodesLimit.setSelected(false);
		}

		if (item.getTimeLimit() != null){
			chooseTimeLimit.setSelected(true);
			timeLimit.getValueFactory().setValue(item.getTimeLimit());
		} else {
			chooseTimeLimit.setSelected(false);
		}

		if (item.getGoal() != null){
			findGoal.setSelected(true);
			tfFindGoal.setText(item.getGoal());
		} else {
			findGoal.setSelected(false);
		}
	}
}
