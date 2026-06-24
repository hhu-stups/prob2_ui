package de.prob2.ui.railml;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.hhu.stups.railml2b.RailML2B;
import de.hhu.stups.railml2b.exceptions.RailML2BException;
import de.hhu.stups.railml2b.exceptions.RailML2BIOException;
import de.hhu.stups.railml2b.exceptions.RailML2BVisualisationException;
import de.hhu.stups.railml2b.load.ImportArguments;
import de.prob.animator.domainobjects.DotOutputFormat;
import de.prob.exception.ProBError;
import de.prob.model.brules.RuleResults;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.model.SimulationModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static de.hhu.stups.railml2b.output.MachinePrinter.*;

@FXMLInjected
@Singleton
public class RailMLStage extends Stage {

	@FXML
	private VBox mainPane, validationResults, machineOptions;
	@FXML
	private Label rulesLabel, notCheckedLabel, successLabel, failLabel, disabledLabel, validationInfoMessage;
	@FXML
	private HBox finishBox;
	@FXML
	private Button btGenerateAndFinish, btCancelImport, btReload, btStartImport;
	@FXML
	private TextField fileLocationField, locationField;
	@FXML
	private Tooltip fileLocationTooltip, locationTooltip;
	@FXML
	private VBox generatedFiles;
	@FXML
	private ListView<String> generateFileListView;
	private final ObservableList<String> generateFileList = FXCollections.observableArrayList();
	@FXML
	private CheckBox onlyTranslation, semanticChecks, animationMachineCheckbox, translatedMachineCheckbox, dataMachineCheckbox,
			validationMachineCheckbox, visualisationCheckbox, closeAfterGeneration;
	@FXML
	public VBox progressBox;
	@FXML
	public Label progressDescription, progressLabel, progressOperation;
	@FXML
	public ProgressBar progressBar;
	@FXML
	private ButtonBar generateButtonBar;

	private final ImportArguments importArguments;
	private Path outputPath = null;
	private String modelName = null;
	private final BooleanProperty importSuccess = new SimpleBooleanProperty(false);
	private final BooleanProperty importForVisualisation = new SimpleBooleanProperty(false);
	private final BooleanProperty generationRunning = new SimpleBooleanProperty(false);

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final Injector injector;
	private final I18n i18n;

	private final FileChooserManager fileChooserManager;
	private final BackgroundUpdater updater;
	private RailML2B railML2B;
	private UIProgressListener listener;

	@Inject
	public RailMLStage(final StageManager stageManager, final CurrentProject currentProject,
	                   final Injector injector, final I18n i18n, final FileChooserManager fileChooserManager,
	                   final StopActions stopActions, final ImportArguments importArguments) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		this.importArguments = importArguments;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.updater = new BackgroundUpdater("railml2b");
		stopActions.add(this::cancel);
		stageManager.loadFXML(this, "railml_stage.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		btCancelImport.visibleProperty().bind(importSuccess.not().or(updater.runningProperty()));
		btReload.visibleProperty().bind(importSuccess.and(updater.runningProperty().not()));
		btReload.managedProperty().bind(btReload.visibleProperty());
		btStartImport.disableProperty().bind(
				fileLocationField.lengthProperty().lessThanOrEqualTo(0)
				.or(updater.runningProperty())
				.or(importSuccess));
		btGenerateAndFinish.disableProperty().bind(
				translatedMachineCheckbox.selectedProperty()
				.or(dataMachineCheckbox.selectedProperty()
				.or(animationMachineCheckbox.selectedProperty())
				.or(validationMachineCheckbox.selectedProperty())
				.or(visualisationCheckbox.selectedProperty())
				.or(generationRunning))
				.not());
		finishBox.visibleProperty().bind(importSuccess);
		finishBox.managedProperty().bind(finishBox.visibleProperty());
		fileLocationField.setText("");
		fileLocationTooltip.textProperty().bind(fileLocationField.textProperty());
		locationField.setText("");
		locationTooltip.textProperty().bind(locationField.textProperty());

		onlyTranslation.disableProperty().bind(visualisationCheckbox.selectedProperty().or(semanticChecks.selectedProperty()).or(updater.runningProperty()).or(importSuccess));
		semanticChecks.disableProperty().bind(onlyTranslation.selectedProperty().or(updater.runningProperty()).or(importSuccess));
		visualisationCheckbox.disableProperty().bind(onlyTranslation.selectedProperty().or(updater.runningProperty()).or(importSuccess));
		visualisationCheckbox.selectedProperty().addListener((obs, o, n) -> {
			if (n) {
				generateFileList.add(modelName + ".svg");
			} else {
				generateFileList.remove(modelName + ".svg");
			}
		});

		generatedFiles.visibleProperty().bind(importSuccess.and(Bindings.isEmpty(generateFileList).not()));
		generatedFiles.managedProperty().bind(generatedFiles.visibleProperty());
		generateFileListView.setItems(generateFileList);
		generateFileListView.prefHeightProperty().bind(Bindings.size(generateFileList).multiply(generateFileListView.getFixedCellSize()));

		validationResults.visibleProperty().bind(importSuccess.and(semanticChecks.selectedProperty()));
		validationResults.managedProperty().bind(validationResults.visibleProperty());

		machineOptions.visibleProperty().bind(importSuccess);
		machineOptions.managedProperty().bind(machineOptions.visibleProperty());
		translatedMachineCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				generateFileList.add(modelName + ".mch");
			} else {
				generateFileList.remove(modelName + ".mch");
			}
		});
		dataMachineCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				translatedMachineCheckbox.setSelected(true);
				translatedMachineCheckbox.setDisable(true);
				generateFileList.add(modelName + DATA_MCH);
			} else {
				translatedMachineCheckbox.setDisable(false);
				generateFileList.remove(modelName + DATA_MCH);
			}
		});
		animationMachineCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				dataMachineCheckbox.setSelected(true);
				dataMachineCheckbox.setDisable(true);
				generateFileList.addAll(modelName + ANIMATION_MCH, VISB_DEF, SIMB_JSON);
			} else {
				if (!validationMachineCheckbox.isSelected())
					dataMachineCheckbox.setDisable(false);
				generateFileList.removeAll(modelName + ANIMATION_MCH, VISB_DEF, SIMB_JSON);
			}
		});
		validationMachineCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				dataMachineCheckbox.setSelected(true);
				dataMachineCheckbox.setDisable(true);
				generateFileList.add(modelName + VALIDATION_MCH);
			} else {
				if (!animationMachineCheckbox.isSelected())
					dataMachineCheckbox.setDisable(false);
				generateFileList.remove(modelName + VALIDATION_MCH);
			}
		});

		progressBox.visibleProperty().bind(updater.runningProperty());
		progressBox.managedProperty().bind(progressBox.visibleProperty());
		progressBar.visibleProperty().bind(updater.runningProperty());
		progressBar.managedProperty().bind(progressBar.visibleProperty());

		ChangeListener<Boolean> sizeToSceneListener = (obs, o, n) -> this.sizeToScene();
		rulesLabel.fontProperty().addListener((obs, o, n) -> this.sizeToScene());
		importSuccess.addListener(sizeToSceneListener);
		progressBar.visibleProperty().addListener(sizeToSceneListener);
		translatedMachineCheckbox.selectedProperty().addListener(sizeToSceneListener);
		dataMachineCheckbox.selectedProperty().addListener(sizeToSceneListener);
		animationMachineCheckbox.selectedProperty().addListener(sizeToSceneListener);
		validationMachineCheckbox.selectedProperty().addListener(sizeToSceneListener);

		this.setOnShown(e -> this.sizeToScene());
		setOnCloseRequest(e -> this.cancel());
	}

	@FXML
	private void selectRailMLFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("railml.stage.filechooser.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getRailMLFilter());
		initializeForPath(fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.DATA_IMPORT, stageManager.getCurrent()));
	}

	public void initializeForPath(Path path) {
		if (path != null) {
			this.resetUI();
			outputPath = path.getParent().toAbsolutePath();
			importArguments.file(path.toFile()).output(outputPath).modelName(MoreFiles.getNameWithoutExtension(path));
			modelName = importArguments.modelName();
			fileLocationField.setText(path.toAbsolutePath().toString());
			locationField.setText(outputPath.toString());
		}
	}

	@FXML
	private void selectRailMLDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(i18n.translate("railml.stage.directorychooser.title"));
		Path path = fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.DATA_IMPORT, stageManager.getCurrent());
		if (path != null) {
			outputPath = path.toAbsolutePath();
			importArguments.output(outputPath);
			locationField.setText(outputPath.toString());
		}
	}

	@FXML
	private void startImport() {
		clearProgressWithMessage("Initialise import");
		this.sizeToScene();
		updateArguments();
		updater.execute(() -> {
			try {
				railML2B = injector.getInstance(RailML2B.class);
				if (importArguments.onlyTranslation()) {
					Platform.runLater(this::createMachinesAndProject);
					return;
				}

				listener = new UIProgressListener(progressBar, progressOperation, progressLabel, progressDescription,
					railML2B.getMachineLoader().getNumberOfOperations());
				railML2B.setCustomProgressListener(listener);
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
				RuleResults results = railML2B.loadAndValidate();
				RuleResults.ResultSummary summary = results.getSummary();

				Platform.runLater(() -> {
					clearProgressWithMessage("Import successful.");
					rulesLabel.setText(String.valueOf(summary.numberOfRules));
					notCheckedLabel.setText(String.valueOf(summary.numberOfRulesNotChecked));
					successLabel.setText(String.valueOf(summary.numberOfRulesSucceeded));
					failLabel.setText(String.valueOf(summary.numberOfRulesFailed));
					disabledLabel.setText(String.valueOf(summary.numberOfRulesDisabled));
				});
				importSuccess.set(true);
				importForVisualisation.set(importArguments.generateVisualisation() != null);
			} catch (ProBError e) {
				Platform.runLater(() -> {
					boolean isRailMLError = e.getErrors().stream().allMatch(error -> error.getMessage().startsWith("RailML"));
					if (isRailMLError) {
						stageManager.makeExceptionAlert(e, "railml.stage.import.error.header", "railml.stage.import.error.content").showAndWait();
					} else {
						stageManager.makeExceptionAlert(e, "error.errorTable.type.INTERNAL_ERROR").showAndWait();
					}
					this.toFront();
				});
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@FXML
	private void generateAndFinish() {
		generationRunning.set(true);
		updateArguments();
		RailMLInspectDotStage railMLInspectDotStage = injector.getInstance(RailMLInspectDotStage.class);
		if (importArguments.generateVisualisation() != null) {
			railMLInspectDotStage.initializeForArguments(importArguments, railML2B.getMachineLoader().getCurrentTrace());
			railMLInspectDotStage.show();
			railMLInspectDotStage.toFront();
			if (importArguments.saveGeneratedDataMachine() || importArguments.generateAnimationMachine() || importArguments.generateValidationMachine()) {
				railMLInspectDotStage.setOnHidden(event -> createMachinesAndProject());
			}
		} else {
			createMachinesAndProject();
		}
		generationRunning.set(false);
	}

	private void createMachinesAndProject() {
		if (currentProject.confirmReplacingProject()) {
			try {
				currentProject.switchTo(new Project(modelName, "", Collections.emptyList(), Collections.emptyList(),
					Collections.emptyList(), Project.metadataBuilder().build(), outputPath), true);

				if (importArguments.saveTranslatedDataMachine()) {
					railML2B.generateTranslatedDataMachine();
					currentProject.addMachine(new Machine(modelName,
							i18n.translate("railml.stage.machineChoice.translatedData.tooltip"),
							outputPath.relativize(outputPath.resolve(modelName + ".mch"))));
				}
				if (importArguments.saveGeneratedDataMachine()) {
					railML2B.generateDataMachine();
					currentProject.addMachine(new Machine(modelName + DATA,
						i18n.translate("railml.stage.machineChoice.data.tooltip"),
							outputPath.relativize(outputPath.resolve(modelName + DATA_MCH))));
				}
				if (importArguments.generateValidationMachine()) {
					railML2B.generateValidationMachine();
					currentProject.addMachine(new Machine(modelName + VALIDATION,
						i18n.translate("railml.stage.machineChoice.validation.tooltip"),
						outputPath.relativize(outputPath.resolve(modelName + VALIDATION_MCH))));
				}
				if (importArguments.generateAnimationMachine()) {
					railML2B.generateAnimationMachine();
					final Machine animationMachine = new Machine(modelName + ANIMATION,
						i18n.translate("railml.stage.machineChoice.animation.tooltip"),
							outputPath.relativize(outputPath.resolve(modelName + ANIMATION_MCH)));
					animationMachine.getSimulations()
						.add(new SimulationModel(outputPath.relativize(outputPath.resolve("railML3_SimB.json"))));
					currentProject.addMachine(animationMachine);
				}

				List<Machine> createdMachines = currentProject.getMachines();
				if (!createdMachines.isEmpty()) {
					currentProject.loadMachineWithConfirmation(createdMachines.getLast());
				}
			} catch (RailML2BIOException e) {
				// TODO: exception handling
				stageManager.makeExceptionAlert(e, "", "");
			} catch (RailML2BException e) {
				throw new RuntimeException(e);
			}
		}
		if (closeAfterGeneration.isSelected())
			this.close();
	}

	@FXML
	private void saveValidationReport() throws RailML2BIOException {
		if (railML2B != null && importSuccess.get()) {
			railML2B.saveValidationReport("HTML"); // TODO: XML
			validationInfoMessage.setText("Saved report at output location.");
		} else {
			validationInfoMessage.setText("Report could NOT be saved!");
		}
	}

	@FXML
	private void visualizeCompleteDependencyGraph() throws RailML2BVisualisationException {
		if (railML2B != null && importSuccess.get()) {
			railML2B.saveRuleDependencyGraph(DotOutputFormat.PDF); // TODO other formats
			validationInfoMessage.setText("Saved graph at output location.");
		} else {
			validationInfoMessage.setText("Graph could NOT be saved!");
		}
	}

	private void clearProgressWithMessage(String message) {
		progressDescription.setText(message);
		progressLabel.setText("");
		progressBar.setProgress(-1);
		progressOperation.setText("");
	}

	@FXML
	private void cancel() {
		if (updater.isRunning()) {
			if (confirmAbortImport()) {
				updater.cancel(true);
				if (railML2B != null) railML2B.finish();
			} else {
				this.toFront();
				return;
			}
		}
		this.close();
	}

	private void updateArguments() {
		importArguments.doValidation(semanticChecks.isSelected())
				.onlyTranslation(onlyTranslation.isSelected())
				.saveTranslatedDataMachine(onlyTranslation.isSelected() || translatedMachineCheckbox.isSelected())
				.saveGeneratedDataMachine(dataMachineCheckbox.isSelected())
				.generateAnimationMachine(animationMachineCheckbox.isSelected())
				.generateValidationMachine(validationMachineCheckbox.isSelected())
				.generateVisualisation(visualisationCheckbox.isSelected() ? DotOutputFormat.SVG : null)
				.visualisationStrategy(visualisationCheckbox.isSelected() ? ImportArguments.VisualisationStrategy.DOT : null);
	}

	private boolean confirmAbortImport() {
		final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
			"railml.inspectDot.alerts.confirmAbortImport.header",
			"railml.inspectDot.alerts.confirmAbortImport.content");
		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && ButtonType.OK.equals(result.get());
	}

	@Override
	public void sizeToScene() {
		Platform.runLater(() -> {
			this.setMinWidth(this.getWidth());
			this.setMinHeight(0);
			this.setMaxHeight(Double.MAX_VALUE);
			super.sizeToScene();
			this.setWidth(this.getMinWidth());
			this.setMinWidth(0);
			this.setMinHeight(this.getHeight());
			this.setMaxHeight(this.getHeight());
		});
	}

	@FXML
	private void resetUI() {
		importSuccess.set(false);
		importForVisualisation.set(false);
		clearProgressWithMessage("");
		animationMachineCheckbox.setSelected(false);
		validationMachineCheckbox.setSelected(false);
		validationInfoMessage.setText("");
		generateFileList.clear();
		closeAfterGeneration.setSelected(true);
		generationRunning.set(false);
	}
}
