package de.prob2.ui.dataimport;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.hhu.stups.railml2b.RailML2B;
import de.hhu.stups.railml2b.exceptions.RailML2BException;
import de.hhu.stups.railml2b.exceptions.RailML2BVisualisationException;
import de.hhu.stups.railml2b.internal.ProgressListener;
import de.hhu.stups.railml2b.load.ImportArguments;
import de.prob.animator.domainobjects.DotOutputFormat;
import de.prob.exception.ProBError;
import de.prob.model.brules.RuleResults;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.layout.BindableGlyph;
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
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static de.hhu.stups.railml2b.output.MachinePrinter.*;

@Singleton
public class RailML2BDataImportOptions extends VBox {

	public static class UIProgressListener implements ProgressListener {

		private final ProgressBar progressBar;
		private final Label operation, progressLabel, progressDescription;
		private final int max;

		public UIProgressListener(ProgressBar progressBar, Label operation, Label progressLabel, Label progressDescription, int max) {
			this.progressBar = progressBar;
			this.operation = operation;
			this.progressLabel = progressLabel;
			this.progressDescription = progressDescription;
			this.max = max;
		}

		@Override
		public void updateProgress(int step, String message) {
			Platform.runLater(() -> {
				this.progressBar.setProgress((double) step / max);
				this.operation.setText(message);
				this.progressLabel.setText(" (" + step + "/" + max + ")");
			});
		}

		@Override
		public void updateDescription(String description) {
			Platform.runLater(() -> {
				progressDescription.setText(description);
				progressLabel.setText("");
				progressBar.setProgress(-1);
				operation.setText("");
			});
		}
	}

	@FXML
	private VBox validationResults, machineOptions;
	@FXML
	private Label rulesLabel, notCheckedLabel, successLabel, failLabel, disabledLabel, validationInfoMessage;
	@FXML
	public MenuButton visualizeGraphButton, validationReportButton;
	@FXML
	public MenuItem btGraphPdf, btGraphSvg, btGraphPng, btGraphDot, btReportHtml, btReportXml;
	@FXML
	private Button btReload;
	@FXML
	private VBox generatedFiles;
	@FXML
	private ListView<String> generateFileListView;
	private final ObservableList<String> generateFileList = FXCollections.observableArrayList();
	@FXML
	private CheckBox onlyTranslation, semanticChecks, invariants, animationMachineCheckbox, translatedMachineCheckbox, dataMachineCheckbox,
			validationMachineCheckbox, visualisationCheckbox;
	@FXML
	public VBox progressBox;
	@FXML
	public Label progressDescription, progressLabel, progressOperation;
	@FXML
	public ProgressBar progressBar;

	private final ImportArguments importArguments;
	private Path outputPath;
	private String modelName;
	private final BooleanProperty invalidFile = new SimpleBooleanProperty(false);
	private final BooleanProperty importSuccess = new SimpleBooleanProperty(false);
	private final BooleanProperty generationRunning = new SimpleBooleanProperty(false);

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final Injector injector;
	private final I18n i18n;

	private final BackgroundUpdater updater;
	private RailML2B railML2B;

	private RailML2BDataImportDialog controller;

	@Inject
	public RailML2BDataImportOptions(final StageManager stageManager, final CurrentProject currentProject, final Injector injector,
	                                 final I18n i18n, final StopActions stopActions, final ImportArguments importArguments) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		this.importArguments = importArguments;
		this.i18n = i18n;
		this.updater = new BackgroundUpdater("railml2b");
		stopActions.add(this::cancel);
		stageManager.loadFXML(this, "railml_import_options.fxml");
	}

	@FXML
	public void initialize() {
		btReload.visibleProperty().bind(importSuccess.and(updater.runningProperty().not()));
		btReload.managedProperty().bind(btReload.visibleProperty());
		setupButtonForLoading();

		onlyTranslation.disableProperty().bind(visualisationCheckbox.selectedProperty().or(semanticChecks.selectedProperty()).or(updater.runningProperty()).or(importSuccess));
		semanticChecks.disableProperty().bind(onlyTranslation.selectedProperty().or(updater.runningProperty()).or(importSuccess));
		semanticChecks.selectedProperty().addListener((obs, o, n) -> invariants.setSelected(false));
		invariants.disableProperty().bind(onlyTranslation.selectedProperty().or(semanticChecks.selectedProperty().not()).or(updater.runningProperty()).or(importSuccess));
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
		btGraphPdf.setOnAction(e -> this.visualizeCompleteDependencyGraph(DotOutputFormat.PDF));
		btGraphSvg.setOnAction(e -> this.visualizeCompleteDependencyGraph(DotOutputFormat.SVG));
		btGraphPng.setOnAction(e -> this.visualizeCompleteDependencyGraph(DotOutputFormat.PNG));
		btGraphDot.setOnAction(e -> this.visualizeCompleteDependencyGraph(DotOutputFormat.DOT));
		btReportHtml.setOnAction(e -> this.saveValidationReport("HTML"));
		btReportXml.setOnAction(e -> this.saveValidationReport("XML"));

		machineOptions.visibleProperty().bind(importSuccess);
		machineOptions.managedProperty().bind(machineOptions.visibleProperty());

		// TODO: remove list of generated files
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
	}

	void setController(RailML2BDataImportDialog controller) {
		this.controller = controller;
		setupButtonForLoading();
	}

	public void initializeForPath(Path path) {
		if (path != null) {
			this.resetUI();
			outputPath = path.getParent().toAbsolutePath();
			try {
				importArguments.file(path.toFile()).output(outputPath).modelName(MoreFiles.getNameWithoutExtension(path));
			} catch (RailML2BException e) {
				invalidFile.set(true);
				showError(e, "railml.stage.filename.error.header");
			}
			modelName = importArguments.modelName();
		}
	}

	void setDirectory(Path path) {
		outputPath = path.toAbsolutePath();
		importArguments.output(outputPath);
	}

	void startImport() {
		clearProgressWithMessage(i18n.translate("railml.stage.messages.importInit"));
		this.sizeToScene();
		updateArguments();
		updater.execute(() -> {
			try {
				railML2B = injector.getInstance(RailML2B.class);
				if (importArguments.onlyTranslation()) {
					Platform.runLater(this::createMachinesAndProject);
					return;
				}

				railML2B.setCustomProgressListener(
						new UIProgressListener(progressBar,
								progressOperation,
								progressLabel,
								progressDescription,
								railML2B.getMachineLoader().getNumberOfOperations())
				);

				if (Thread.currentThread().isInterrupted()) {
					return;
				}
				RuleResults results = railML2B.loadAndValidate();
				RuleResults.ResultSummary summary = results.getSummary();

				Platform.runLater(() -> {
					setupButtonForSaving();
					clearProgressWithMessage(i18n.translate("railml.stage.messages.importSuccessful"));
					rulesLabel.setText(String.valueOf(summary.numberOfRules));
					notCheckedLabel.setText(String.valueOf(summary.numberOfRulesNotChecked));
					successLabel.setText(String.valueOf(summary.numberOfRulesSucceeded));
					failLabel.setText(String.valueOf(summary.numberOfRulesFailed));
					disabledLabel.setText(String.valueOf(summary.numberOfRulesDisabled));

					List<BException> violatedInvariants = railML2B.getMachineLoader().getViolatedInvariants();
					if (!violatedInvariants.isEmpty()) {
						showError(new ProBError(new BCompoundException(violatedInvariants)), "The following invariants are violated");
					}
				});
				importSuccess.set(true);
			} catch (ProBError e) {
				Platform.runLater(() -> {
					boolean isRailMLError = e.getErrors().stream().allMatch(error -> error.getMessage().startsWith("RailML"));
					if (isRailMLError) {
						showError(e, "railml.stage.import.error.header", "railml.stage.import.error.content");
					} else {
						showError(e, "error.errorTable.type.INTERNAL_ERROR");
					}
					this.toFront();
				});
			} catch (Exception e) {
				showError(e, "error.errorTable.type.INTERNAL_ERROR");
			}
		});
	}

	void generateAndFinish() {
		generationRunning.set(true);
		updateArguments();
		if (importArguments.generateVisualisation() != null) {
			Platform.runLater(() -> {
				RailML2BVisualisationDialog visualisationDialog = injector.getInstance(RailML2BVisualisationDialog.class);
				visualisationDialog.initializeForArguments(importArguments, railML2B.getMachineLoader().getCurrentTrace());
				visualisationDialog.showAndWait();
				if (importArguments.saveGeneratedDataMachine() || importArguments.generateAnimationMachine() || importArguments.generateValidationMachine()) {
					visualisationDialog.setOnHidden(event -> {
						createMachinesAndProject();
						if (controller != null) controller.close();
					});
				}
			});
		} else {
			createMachinesAndProject();
		}
		generationRunning.set(false);
	}

	private void createMachinesAndProject() {
		Platform.runLater(() -> {
			if (currentProject.confirmReplacingProject()) {
				updateModelName();
				String customName = importArguments.modelName();
				currentProject.switchTo(new Project(customName, i18n.translate("railml.stage.project.description"),
						Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
						Project.metadataBuilder().build(), outputPath), true);

				updater.execute(() -> {
					try {
						if (importArguments.saveTranslatedDataMachine()) {
							railML2B.generateTranslatedDataMachine();
							Platform.runLater(() -> currentProject.addMachine(new Machine(customName,
									i18n.translate("railml.stage.machineChoice.translatedData.tooltip"),
									outputPath.relativize(outputPath.resolve(customName + ".mch")))));
						}
						if (importArguments.saveGeneratedDataMachine()) {
							railML2B.generateDataMachine();
							Platform.runLater(() -> currentProject.addMachine(new Machine(customName + DATA,
									i18n.translate("railml.stage.machineChoice.data.tooltip"),
									outputPath.relativize(outputPath.resolve(customName + DATA_MCH)))));
						}
						if (importArguments.generateValidationMachine()) {
							railML2B.generateValidationMachine();
							Platform.runLater(() -> currentProject.addMachine(new Machine(customName + VALIDATION,
									i18n.translate("railml.stage.machineChoice.validation.tooltip"),
									outputPath.relativize(outputPath.resolve(customName + VALIDATION_MCH)))));
						}
						if (importArguments.generateAnimationMachine()) {
							railML2B.generateAnimationMachine();
							final Machine animationMachine = new Machine(customName + ANIMATION,
									i18n.translate("railml.stage.machineChoice.animation.tooltip"),
									outputPath.relativize(outputPath.resolve(customName + ANIMATION_MCH)));
							animationMachine.getSimulations()
									.add(new SimulationModel(outputPath.relativize(outputPath.resolve("railML3_SimB.json"))));
							Platform.runLater(() -> currentProject.addMachine(animationMachine));
						}

						List<Machine> createdMachines = currentProject.getMachines();
						if (!createdMachines.isEmpty()) {
							Platform.runLater(() -> currentProject.loadMachineWithConfirmation(createdMachines.getLast()));
						}
					} catch (RailML2BException e) {
						Platform.runLater(() -> showError(e, "error.errorTable.type.INTERNAL_ERROR"));
					}
				});
			}
			if (controller != null)
				controller.close();
		});
	}

	private void saveValidationReport(String outputFormat) {
		if (railML2B != null && importSuccess.get()) {
			try {
				updateModelName();
				railML2B.saveValidationReport(outputFormat);
				validationInfoMessage.setText(i18n.translate("railml.stage.messages.savedReport"));
			} catch (RailML2BException e) {
				showError(e, "error.errorTable.type.INTERNAL_ERROR");
			}
		} else {
			validationInfoMessage.setText(i18n.translate("railml.stage.messages.reportFail"));
		}
	}

	private void visualizeCompleteDependencyGraph(String dotOutputFormat) {
		if (railML2B != null && importSuccess.get()) {
			try {
				updateModelName();
				railML2B.saveRuleDependencyGraph(dotOutputFormat);
				validationInfoMessage.setText(i18n.translate("railml.stage.messages.savedGraph"));
			} catch (RailML2BVisualisationException e) {
				showError(e, "error.errorTable.type.INTERNAL_ERROR");
			}
		} else {
			validationInfoMessage.setText(i18n.translate("railml.stage.messages.graphFail"));
		}
	}

	private void clearProgressWithMessage(String message) {
		Platform.runLater(() -> {
			progressDescription.setText(message);
			progressLabel.setText("");
			progressBar.setProgress(-1);
			progressOperation.setText("");
		});
	}

	private void showError(Throwable e, String headerBundleKey, String contentBundleKey) {
		Platform.runLater(() -> {
			stageManager.makeExceptionAlert(e, headerBundleKey, contentBundleKey).showAndWait();
			this.toFront();
		});
	}

	private void showError(Throwable e, String contentBundleKey) {
		showError(e, null, contentBundleKey);
	}

	@FXML
	public void cancel() {
		Platform.runLater(() -> {
			if (updater.isRunning()) {
				if (confirmAbortImport()) {
					updater.cancel(true);
					if (railML2B != null) railML2B.finish();
				} else {
					this.toFront();
					return;
				}
			}
			if (controller != null)
				controller.close();
		});
	}

	private void updateArguments() {
		updateModelName();
		importArguments.doValidation(semanticChecks.isSelected())
				.onlyTranslation(onlyTranslation.isSelected())
				.saveTranslatedDataMachine(onlyTranslation.isSelected() || translatedMachineCheckbox.isSelected())
				.saveGeneratedDataMachine(dataMachineCheckbox.isSelected())
				.generateAnimationMachine(animationMachineCheckbox.isSelected())
				.generateValidationMachine(validationMachineCheckbox.isSelected())
				.generateVisualisation(visualisationCheckbox.isSelected() ? DotOutputFormat.SVG : null)
				.visualisationStrategy(visualisationCheckbox.isSelected() ? ImportArguments.VisualisationStrategy.DOT : null)
				.checkInvariants(invariants.isSelected());
	}

	private void updateModelName() {
		if (controller != null && controller.cbMachineName.isSelected()) {
			importArguments.modelName(controller.machineName.getValue());
		} else {
			importArguments.modelName(modelName);
		}
	}

	private boolean confirmAbortImport() {
		final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
			"railml.inspectDot.alerts.confirmAbortImport.header",
			"railml.inspectDot.alerts.confirmAbortImport.content");
		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && ButtonType.OK.equals(result.get());
	}

	void sizeToScene() {
		Platform.runLater(() -> {
			if (controller != null) {
				controller.setMinWidth(controller.getWidth());
				controller.setMinHeight(0);
				controller.setMaxHeight(Double.MAX_VALUE);
				Platform.runLater(() -> {
					controller.sizeToScene();
					controller.setWidth(controller.getMinWidth());
					controller.setMinWidth(0);
					controller.setMinHeight(controller.getHeight());
					controller.setMaxHeight(controller.getHeight());
					controller.getScene().getRoot().applyCss();
					controller.getScene().getRoot().layout();
				});
			}
		});
	}

	@FXML
	void resetUI() {
		Platform.runLater(() -> {
			invalidFile.set(false);
			importSuccess.set(false);
			clearProgressWithMessage("");
			animationMachineCheckbox.setSelected(false);
			validationMachineCheckbox.setSelected(false);
			validationInfoMessage.setText("");
			generateFileList.clear();
			generationRunning.set(false);
			setupButtonForLoading();
		});
	}

	private void setupButtonForLoading() {
		if (controller != null) {
			controller.btImportAndOpen.disableProperty().unbind();
			controller.btImportAndOpen.disableProperty().bind(
					updater.runningProperty()
							.or(invalidFile) // FIXME does not work
							.or(importSuccess));
			controller.btImportAndOpen.setText(i18n.translate("railml.stage.button.import"));
			controller.btImportAndOpen.setGraphic(null);

			controller.progressIndicator.visibleProperty().unbind();
		}
	}

	private void setupButtonForSaving() {
		if (controller != null) {
			controller.btImportAndOpen.disableProperty().unbind();
			controller.btImportAndOpen.disableProperty().bind(
					translatedMachineCheckbox.selectedProperty()
							.or(dataMachineCheckbox.selectedProperty()
									.or(animationMachineCheckbox.selectedProperty())
									.or(validationMachineCheckbox.selectedProperty())
									.or(visualisationCheckbox.selectedProperty())
									.or(generationRunning))
							.not());
			controller.btImportAndOpen.setText(i18n.translate("railml.stage.generateAndFinish"));
			BindableGlyph saveIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.SAVE);
			saveIcon.getStyleClass().add("icon-dark");
			controller.btImportAndOpen.setGraphic(saveIcon);

			controller.progressIndicator.visibleProperty().bind(updater.runningProperty());
		}
	}

	boolean importFinished() {
		return importSuccess.getValue();
	}

}
