package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Inject;
import de.prob.check.ModelCheckingSearchStrategy;
import de.prob.check.TLCModelChecker;
import de.prob.check.TLCModelCheckingOptions;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.TranslatableAdapter;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import de.tlc4b.TLC4BCliOptions.TLCOption;

import java.util.Map;

import static de.tlc4b.TLC4BCliOptions.TLCOption.*;

@FXMLInjected
public class TLCModelCheckingTab extends Tab {

	@FXML
	private VBox errorMessageBox;
	@FXML
	private Label errorMessage;
	@FXML
	private ChoiceBox<ModelCheckingSearchStrategy> selectSearchStrategy;
	@FXML
	private CheckBox findDeadlocks;
	@FXML
	private CheckBox findInvViolations;
	@FXML
	private CheckBox findBAViolations;
	@FXML
	private CheckBox checkWelldefinedness;
	@FXML
	private CheckBox checkGoal;
	@FXML
	private CheckBox checkLTL;
	@FXML
	private CheckBox addLTLFormula;
	@FXML
	private TextField tfAddLTL;

	@FXML
	private CheckBox setupConstantsUsingProB;
	@FXML
	private Spinner<Integer> nrWorkers;

	private final I18n i18n;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private ModelCheckingItem result;

	@Inject
	private TLCModelCheckingTab(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject, final CurrentTrace currentTrace) {
		this.i18n = i18n;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.result = null;
		stageManager.loadFXML(this, "tlc_modelchecking_tab.fxml");
	}

	@FXML
	private void initialize() {
		this.selectSearchStrategy.getItems().setAll(
			ModelCheckingSearchStrategy.BREADTH_FIRST,
			ModelCheckingSearchStrategy.DEPTH_FIRST
		);
		this.selectSearchStrategy.setValue(ModelCheckingSearchStrategy.BREADTH_FIRST);
		this.selectSearchStrategy.setConverter(i18n.translateConverter(TranslatableAdapter.adapter(TLCModelCheckingTab::getSearchStrategyNameKey)));

		this.nrWorkers.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1));
		this.nrWorkers.getValueFactory().setValue(Integer.valueOf(currentTrace.getStateSpace().getCurrentPreference("TLC_WORKERS")));
		this.nrWorkers.getEditor().textProperty().addListener((observable, from, to) -> {
			try {
				nrWorkers.getValueFactory().setValue(Integer.parseInt(to));
			} catch (NumberFormatException e) {
				final Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, "", "verifications.modelchecking.modelcheckingStage.invalidInput");
				//alert.initOwner(this);
				alert.showAndWait();
			}
		});

		this.tfAddLTL.visibleProperty().bind(checkLTL.selectedProperty());
	}

	public static String getSearchStrategyNameKey(final ModelCheckingSearchStrategy searchStrategy) {
		return switch (searchStrategy) {
			case BREADTH_FIRST -> "verifications.modelchecking.modelcheckingStage.strategy.breadthFirst";
			case DEPTH_FIRST -> "verifications.modelchecking.modelcheckingStage.strategy.depthFirst";
			default -> null;
		};
	}

	ModelCheckingItem startModelCheck(String id) {
		ModelCheckingSearchStrategy searchStrategy = selectSearchStrategy.getValue();
		// provide/open/load TLA generated file for inspection (in UI), independently from MC
		return new TLCModelCheckingItem(id, searchStrategy, getOptions());
	}

	private Map<TLCOption, String> getOptions() {
		return new TLCModelCheckingOptions(currentTrace.getStateSpace())
			.checkDeadlocks(findDeadlocks.isSelected())
			.checkInvariantViolations(findInvViolations.isSelected())
			.checkAssertions(findBAViolations.isSelected())
			.checkWelldefinedness(checkWelldefinedness.isSelected())
			.checkLTLAssertions(checkLTL.isSelected())
			.checkGoal(checkGoal.isSelected())
			.checkLTLFormula(addLTLFormula.isSelected() ? tfAddLTL.getText() : null)
			.setupConstantsUsingProB(setupConstantsUsingProB.isSelected())
			.setNumberOfWorkers(nrWorkers.getValueFactory().getValue().toString())
			// TODO: -dfid
			.getOptions();
	}

	boolean tlcCheck() {
		Exception exception = TLCModelChecker.checkTLCApplicable(currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation()).toString(), 5);
		if (exception != null) {
			errorMessageBox.setVisible(true);
			errorMessage.setText(exception.getMessage());
			return false;
		} else {
			errorMessageBox.setVisible(false);
			errorMessage.setText("");
			return true;
		}
	}

	public ModelCheckingItem getResult() {
		return result;
	}

	public void setData(final TLCModelCheckingItem item) {
		findDeadlocks.setSelected(!item.getOptions().containsKey(NODEAD));
		findInvViolations.setSelected(!item.getOptions().containsKey(NOINV));
		findBAViolations.setSelected(!item.getOptions().containsKey(NOASS));
		checkWelldefinedness.setSelected(item.getOptions().containsKey(WDCHECK));
		checkLTL.setSelected(!item.getOptions().containsKey(NOLTL));
		checkGoal.setSelected(!item.getOptions().containsKey(NOGOAL));
		String ltlFormula = item.getOptions().getOrDefault(LTLFORMULA, null);
		if (ltlFormula != null) {
			addLTLFormula.setSelected(true);
			tfAddLTL.setText(ltlFormula);
		} else {
			addLTLFormula.setSelected(false);
		}
		setupConstantsUsingProB.setSelected(item.getOptions().containsKey(CONSTANTSSETUP));
		nrWorkers.getValueFactory().setValue(Integer.parseInt(item.getOptions().get(WORKERS)));
		// TODO: -dfid
		result = item;
	}
}

