package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Inject;
import de.prob.check.ModelCheckingSearchStrategy;
import de.prob.check.TLCModelChecker;
import de.prob.check.TLCModelCheckingOptions;
import de.prob.scripting.ClassicalBFactory;
import de.prob.statespace.Language;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.TranslatableAdapter;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import de.tlc4b.TLC4BCliOptions.TLCOption;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

import static de.tlc4b.TLC4BCliOptions.TLCOption.*;

@FXMLInjected
public class TLCModelCheckingTab extends Tab {
	private static final Logger LOGGER = LoggerFactory.getLogger(TLCModelCheckingTab.class);

	@FXML
	private VBox errorMessageBox;
	@FXML
	private Label errorMessage;
	@FXML
	private ChoiceBox<ModelCheckingSearchStrategy> selectSearchStrategy;
	@FXML
	private HBox dfidDepthBox;
	@FXML
	private Spinner<Integer> dfidInitialDepth;
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
	@FXML
	private CheckBox proofGuidedMC;
	@FXML
	private CheckBox useSymmetry;
	@FXML
	private HBox saveLocationBox;
	@FXML
	private CheckBox saveGeneratedFiles;
	@FXML
	private TextField tfSaveLocation;
	@FXML
	private Button changeLocationButton;

	private int oldNrWorkers = 1;

	private final I18n i18n;

	private final FileChooserManager fileChooserManager;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private ModelCheckingItem result;

	@Inject
	private TLCModelCheckingTab(final StageManager stageManager, final I18n i18n, final FileChooserManager fileChooserManager,
	                            final CurrentProject currentProject, final CurrentTrace currentTrace) {
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
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

		this.dfidInitialDepth.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1));
		this.selectSearchStrategy.getSelectionModel().selectedItemProperty().addListener((obs, from, to) -> {
			if (to == ModelCheckingSearchStrategy.DEPTH_FIRST) {
				this.dfidDepthBox.setVisible(true);
				this.nrWorkers.setDisable(true);
				this.oldNrWorkers = nrWorkers.getValue();
				this.nrWorkers.getValueFactory().setValue(1);
			} else {
				this.dfidDepthBox.setVisible(false);
				this.nrWorkers.getValueFactory().setValue(oldNrWorkers);
				this.nrWorkers.setDisable(false);
			}
		});

		this.nrWorkers.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1));
		this.nrWorkers.getEditor().textProperty().addListener((observable, from, to) -> {
			try {
				nrWorkers.getValueFactory().setValue(Integer.parseInt(to));
			} catch (NumberFormatException e) {
				final Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, "", "verifications.modelchecking.modelcheckingStage.invalidInput");
				alert.initOwner(stageManager.getCurrent());
				alert.showAndWait();
			}
		});

		this.tfAddLTL.visibleProperty().bind(addLTLFormula.selectedProperty());

		this.saveLocationBox.visibleProperty().bind(saveGeneratedFiles.selectedProperty());
		this.tfSaveLocation.setText(currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation()).getParent().resolve(currentProject.getCurrentMachine().getName()).toString());
		this.changeLocationButton.setOnAction(e -> {
			final DirectoryChooser chooser = new DirectoryChooser();
			chooser.setInitialDirectory(currentProject.getLocation().toFile());
			Path result = fileChooserManager.showDirectoryChooser(chooser, FileChooserManager.Kind.NEW_MACHINE, stageManager.getCurrent());
			if (result != null) {
				this.tfSaveLocation.setText(result.toString());
			}
		});

		// initialize with current preferences, e.g. WORKERS
		// FIXME This runs a ProB command on the UI thread to get the current preference values. This should be made lazy and/or moved to the CliTaskExecutor to avoid blocking the UI thread.
		this.setData(TLCModelCheckingOptions.fromPreferences(currentTrace.getStateSpace()).getOptions());
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
		return new TLCModelCheckingItem(id, searchStrategy, getOptions());
	}

	private Map<TLCOption, String> getOptions() {
		// FIXME This runs a ProB command on the UI thread to get the current preference values. This should be changed to use the existing options from setData as defaults, instead of calculating them again from the ProB preferences.
		return TLCModelCheckingOptions.fromPreferences(currentTrace.getStateSpace())
			.useDepthFirstSearch(selectSearchStrategy.getSelectionModel().getSelectedItem() == ModelCheckingSearchStrategy.DEPTH_FIRST ?
				String.valueOf(dfidInitialDepth.getValue()) : null)
			.checkDeadlocks(findDeadlocks.isSelected())
			.checkInvariantViolations(findInvViolations.isSelected())
			.checkAssertions(findBAViolations.isSelected())
			.checkWelldefinedness(checkWelldefinedness.isSelected())
			.checkLTLAssertions(checkLTL.isSelected())
			.checkGoal(checkGoal.isSelected())
			.checkLTLFormula(addLTLFormula.isSelected() ? tfAddLTL.getText() : null)
			.setupConstantsUsingProB(setupConstantsUsingProB.isSelected())
			.setNumberOfWorkers(nrWorkers.getValueFactory().getValue().toString())
			.proofGuidedModelChecking(proofGuidedMC.isSelected())
			.useSymmetry(useSymmetry.isSelected())
			.saveGeneratedFiles(saveGeneratedFiles.isSelected())
			.outputDir(saveGeneratedFiles.isSelected() ? tfSaveLocation.getText() : null)
			.getOptions();
	}

	boolean tlcCheck() {
		// TODO: support other languages by pretty printing internal representation (Event-B)?
		//  (with current internal repr. not automatically possible)
		if (currentProject.getCurrentMachine().getModelFactoryClass() == ClassicalBFactory.class) {
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
		} else {
			errorMessageBox.setVisible(true);
			errorMessage.setText(i18n.translate("verifications.modelchecking.modelcheckingStage.tlcTab.onlyClassicalB"));
			return false;
		}
	}

	public ModelCheckingItem getResult() {
		return result;
	}

	public void setData(final TLCModelCheckingItem item) {
		setData(item.getOptions());
		result = item;
	}
	
	private void setData(final Map<TLCOption, String> options) {
		if (options.containsKey(DFID)) {
			selectSearchStrategy.getSelectionModel().select(ModelCheckingSearchStrategy.DEPTH_FIRST);
			dfidInitialDepth.getValueFactory().setValue(Integer.parseInt(options.get(DFID)));
		}
		findDeadlocks.setSelected(!options.containsKey(NODEAD));
		findInvViolations.setSelected(!options.containsKey(NOINV));
		findBAViolations.setSelected(!options.containsKey(NOASS));
		checkWelldefinedness.setSelected(options.containsKey(WDCHECK));
		checkLTL.setSelected(!options.containsKey(NOLTL));
		checkGoal.setSelected(!options.containsKey(NOGOAL));
		String ltlFormula = options.getOrDefault(LTLFORMULA, null);
		if (ltlFormula != null) {
			addLTLFormula.setSelected(true);
			tfAddLTL.setText(ltlFormula);
		} else {
			addLTLFormula.setSelected(false);
		}
		setupConstantsUsingProB.setSelected(options.containsKey(CONSTANTSSETUP));
		nrWorkers.getValueFactory().setValue(Integer.parseInt(options.get(WORKERS)));
		proofGuidedMC.setSelected(options.containsKey(PARINVEVAL));
		useSymmetry.setSelected(options.containsKey(SYMMETRY));
		saveGeneratedFiles.setSelected(!options.containsKey(TMP));
		if (options.containsKey(OUTPUT)) {
			tfSaveLocation.setText(options.get(OUTPUT));
		}
	}
}

