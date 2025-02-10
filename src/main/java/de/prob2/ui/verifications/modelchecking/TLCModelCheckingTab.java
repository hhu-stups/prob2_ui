package de.prob2.ui.verifications.modelchecking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.command.GetInternalRepresentationCommand;
import de.prob.animator.domainobjects.FormulaTranslationMode;
import de.prob.check.ModelCheckingSearchStrategy;
import de.prob.check.TLCModelCheckingOptions;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.FormalismType;
import de.prob.statespace.StateSpace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.TranslatableAdapter;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.tlc4b.TLC4B;
import de.tlc4b.TLC4BOption;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private boolean checkedTlcApplicable;
	private final StringProperty tlcApplicableError;

	private ModelCheckingItem result;

	@Inject
	private TLCModelCheckingTab(final StageManager stageManager, final I18n i18n, final FileChooserManager fileChooserManager,
			final CurrentProject currentProject, final CurrentTrace currentTrace) {
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;

		this.checkedTlcApplicable = false;
		this.tlcApplicableError = new SimpleStringProperty(this, "tlcApplicableError", null);
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

		errorMessageBox.visibleProperty().bind(this.tlcApplicableErrorProperty().isNotNull());
		errorMessage.textProperty().bind(this.tlcApplicableErrorProperty());

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

	private Map<TLC4BOption, String> getOptions() {
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

	public ReadOnlyStringProperty tlcApplicableErrorProperty() {
		return this.tlcApplicableError;
	}

	public String getTlcApplicableError() {
		return this.tlcApplicableErrorProperty().get();
	}

	void checkTlcApplicable() {
		if (this.checkedTlcApplicable) {
			return;
		}

		this.checkedTlcApplicable = true;

		Thread thread = new Thread(() -> {
			try {
				Path machinePath = getClassicalBMachine(currentProject, currentTrace.getStateSpace(), i18n);
				// TODO: show info when internal representation is used
				TLC4B.checkTLC4BIsApplicable(machinePath.toString());
			} catch (BCompoundException | RuntimeException exc) {
				LOGGER.warn("TLC4B is not applicable to this machine", exc);
				Platform.runLater(() -> this.tlcApplicableError.set(exc.getMessage()));
				return;
			}

			Platform.runLater(() -> this.tlcApplicableError.set(null));
		}, "TLC4B applicability checker");
		// Don't let this thread keep the JVM alive if it's still running when the UI exits.
		thread.setDaemon(true);
		thread.start();
	}

	public ModelCheckingItem getResult() {
		return result;
	}

	public void setData(final TLCModelCheckingItem item) {
		setData(item.getOptions());
		result = item;
	}
	
	private void setData(final Map<TLC4BOption, String> options) {
		checkedTlcApplicable = false;
		tlcApplicableError.set(null);
		if (options.containsKey(TLC4BOption.DFID)) {
			selectSearchStrategy.getSelectionModel().select(ModelCheckingSearchStrategy.DEPTH_FIRST);
			dfidInitialDepth.getValueFactory().setValue(Integer.parseInt(options.get(TLC4BOption.DFID)));
		}
		findDeadlocks.setSelected(!options.containsKey(TLC4BOption.NODEAD));
		findInvViolations.setSelected(!options.containsKey(TLC4BOption.NOINV));
		findBAViolations.setSelected(!options.containsKey(TLC4BOption.NOASS));
		checkWelldefinedness.setSelected(options.containsKey(TLC4BOption.WDCHECK));
		checkLTL.setSelected(!options.containsKey(TLC4BOption.NOLTL));
		checkGoal.setSelected(!options.containsKey(TLC4BOption.NOGOAL));
		String ltlFormula = options.getOrDefault(TLC4BOption.LTLFORMULA, null);
		if (ltlFormula != null) {
			addLTLFormula.setSelected(true);
			tfAddLTL.setText(ltlFormula);
		} else {
			addLTLFormula.setSelected(false);
		}
		setupConstantsUsingProB.setSelected(options.containsKey(TLC4BOption.CONSTANTSSETUP));
		nrWorkers.getValueFactory().setValue(Integer.parseInt(options.get(TLC4BOption.WORKERS)));
		proofGuidedMC.setSelected(options.containsKey(TLC4BOption.PARINVEVAL));
		useSymmetry.setSelected(options.containsKey(TLC4BOption.SYMMETRY));
		saveGeneratedFiles.setSelected(!options.containsKey(TLC4BOption.TMP));
		if (options.containsKey(TLC4BOption.OUTPUT)) {
			tfSaveLocation.setText(options.get(TLC4BOption.OUTPUT));
		}
	}

	public static Path getClassicalBMachine(CurrentProject currentProject, StateSpace stateSpace, I18n i18n) {
		return getClassicalBMachine(currentProject.get(), currentProject.getCurrentMachine(), stateSpace, i18n);
	}

	public static Path getClassicalBMachine(Project project, Machine machine, StateSpace stateSpace, I18n i18n) {
		Path machinePath;
		AbstractModel model = stateSpace.getModel();
		if (model instanceof ClassicalBModel) {
			machinePath = project.getLocation().resolve(machine.getLocation());
		} else if (model.getFormalismType() == FormalismType.B) { // if not classical B: use internal representation
			if (model instanceof EventBModel && !stateSpace.getCurrentPreference("NUMBER_OF_ANIMATED_ABSTRACTIONS").equals("0")) {
				throw new RuntimeException(i18n.translate("verifications.modelchecking.modelcheckingStage.tlcTab.eventBAnimatedAbstractionsError"));
			}
			try { // translate Event-B/Alloy etc. to internal classical B representation
				machinePath = Files.createTempFile("b_internal_rep_",".mch");
				GetInternalRepresentationCommand cmd = new GetInternalRepresentationCommand();
				cmd.setTranslationMode(FormulaTranslationMode.ATELIERB);
				cmd.setTypeInfos(GetInternalRepresentationCommand.TypeInfos.NEEDED);
				stateSpace.execute(cmd);
				Files.writeString(machinePath, cmd.getPrettyPrint());
				machinePath.toFile().deleteOnExit();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else { // model has no classical B internal representation
			throw new RuntimeException(i18n.translate("verifications.modelchecking.modelcheckingStage.tlcTab.onlyClassicalB"));
		}
		return machinePath;
	}
}

