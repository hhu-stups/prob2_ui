package de.prob2.ui.railml;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.FunctionOperation;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.exception.ProBError;
import de.prob.model.brules.RulesChecker;
import de.prob.model.brules.RulesModel;
import de.prob.scripting.Api;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.error.WarningAlert;
import de.prob2.ui.internal.*;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.model.SimulationModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.prob2.ui.railml.RailMLHelper.replaceOldFile;
import static de.prob2.ui.railml.RailMLHelper.replaceOldResourceFile;

@FXMLInjected
@Singleton
public class RailMLStage extends Stage {

	@FXML
	private Button btStartImport;
	@FXML
	private TextField fileLocationField;
	@FXML
	private TextField locationField;
	@FXML
	private Label visualisationStrategyField;
	@FXML
	private AnchorPane generatedFiles;
	@FXML
	private ListView<String> generateFileListView;
	private final ObservableList<String> generateFileList = FXCollections.observableArrayList();
	@FXML
	private CheckBox visualisationCheckbox;
	@FXML
	private CheckBox animationMachineCheckbox;
	@FXML
	private CheckBox validationMachineCheckbox;
	private boolean generateAnimation, generateValidation, generateSVG;
	@FXML
	public VBox progressBox;
	@FXML
	public Label progressDescription;
	@FXML
	public Label progressLabel;
	@FXML
	public Label progressOperation;
	@FXML
	public ProgressBar progressBar;


	@FXML
	private ChoiceBox<RailMLImportMeta.VisualisationStrategy> visualisationStrategyChoiceBox;
	private RailMLImportMeta.VisualisationStrategy visualisationStrategy;

	private Path railMLpath, generationPath;

	private final SimpleStringProperty dataFileName = new SimpleStringProperty("");
	private final SimpleStringProperty animationFileName = new SimpleStringProperty("");
	private final SimpleStringProperty animationDefsFileName = new SimpleStringProperty("RailML3_VisB.def");
	private final SimpleStringProperty validationFileName = new SimpleStringProperty("");
	private final SimpleStringProperty svgFileName = new SimpleStringProperty("");
	private final SimpleStringProperty simBFileName = new SimpleStringProperty("RailML3_SimB.json");

	private StateSpace stateSpace;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final Injector injector;

	private final I18n i18n;
	private final ErrorDisplayFilter errorDisplayFilter;

	private final FileChooserManager fileChooserManager;
	private final BackgroundUpdater updater;
	private final RailMLImportMeta railMLImportMeta;

	@Inject
	public RailMLStage(final StageManager stageManager, final CurrentProject currentProject,
	                   final Injector injector, final I18n i18n, final ErrorDisplayFilter errorDisplayFilter,
	                   final FileChooserManager fileChooserManager,
	                   final StopActions stopActions, final RailMLImportMeta railMLImportMeta) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		this.i18n = i18n;
		this.errorDisplayFilter = errorDisplayFilter;
		this.fileChooserManager = fileChooserManager;
		this.updater = new BackgroundUpdater("railml import executor");
		stopActions.add(this::cancel);
		this.railMLImportMeta = railMLImportMeta;
		stageManager.loadFXML(this, "railml_stage.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		//this.sizeToScene();
		//this.getScene().setOnMouseClicked(event -> this.sizeToScene());
		btStartImport.disableProperty()
			.bind(fileLocationField.lengthProperty().lessThanOrEqualTo(0)
			.or(animationMachineCheckbox.selectedProperty().not()
				.and(validationMachineCheckbox.selectedProperty().not()
				.and(visualisationCheckbox.selectedProperty().not())))
			.or(visualisationCheckbox.selectedProperty()
				.and(visualisationStrategyChoiceBox.valueProperty().isNull()))
			.or(updater.runningProperty()));
		fileLocationField.setText("");
		locationField.setText("");
		visualisationStrategyField.visibleProperty()
			.bind(visualisationCheckbox.selectedProperty());
		visualisationStrategyChoiceBox.getItems().addAll(RailMLImportMeta.VisualisationStrategy.values());
		visualisationStrategyChoiceBox.visibleProperty().bind(visualisationCheckbox.selectedProperty());

		generateFileListView.setItems(generateFileList);
		generatedFiles.visibleProperty().bind(Bindings.isEmpty(generateFileList).not());
		generatedFiles.managedProperty().bind(generatedFiles.visibleProperty());
		generateFileListView.setFixedCellSize(24);
		generateFileListView.prefHeightProperty().bind(Bindings.size(generateFileList).multiply(generateFileListView.getFixedCellSize()).add(2));
		animationMachineCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				generateFileList.addAll(animationFileName.getValue(), animationDefsFileName.getValue(), simBFileName.getValue());
			} else {
				generateFileList.removeAll(animationFileName.getValue(), animationDefsFileName.getValue(), simBFileName.getValue());
			}
		});
		validationMachineCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				generateFileList.add(validationFileName.getValue());
			} else {
				generateFileList.remove(validationFileName.getValue());
			}
		});
		animationMachineCheckbox.selectedProperty().or(validationMachineCheckbox.selectedProperty()).addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				generateFileList.add(dataFileName.getValue());
			} else {
				generateFileList.remove(dataFileName.getValue());
			}
		});
		visualisationStrategyChoiceBox.setValue(RailMLImportMeta.VisualisationStrategy.DOT);
		visualisationCheckbox.selectedProperty()
			.addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				generateFileList.add(svgFileName.getValue());
			} else {
				generateFileList.remove(svgFileName.getValue());
			}
		});

		//progressBar.setManaged(false);
		//progressBar.setVisible(false);
		//progressBar.progressProperty().bind(progress);
		//currentOp.textProperty().bind(currentOperation);
		progressBox.visibleProperty().bind(updater.runningProperty());
		progressBox.managedProperty().bind(progressBox.visibleProperty());

		Path railML = railMLImportMeta.getPath();
		if(railML != null) {
			setFileNames(railML, MoreFiles.getNameWithoutExtension(railML));
		}

		progressBar.visibleProperty().bind(updater.runningProperty());
		progressBar.managedProperty().bind(progressBar.visibleProperty());
		//currentOp.textProperty().bind(currentOperation);

		setOnCloseRequest(e -> this.cancel());
	}

	@FXML
	public void selectRailMLFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("railml.stage.filechooser.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getRailMLFilter());
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.RAILML, stageManager.getCurrent());

		if(path != null) {
			String fileName = MoreFiles.getNameWithoutExtension(path);
			//if (fileName.contains("-") || (Character.isAlphabetic(fileName.charAt(0)) && !Character.isUpperCase(fileName.charAt(0)))) {
			//	stageManager.makeAlert(Alert.AlertType.ERROR, "railml.stage.filename.error.header", "railml.stage.filename.error.content").showAndWait();
			//	this.requestFocus();
			//} else {
				generateFileList.clear();
				animationMachineCheckbox.setSelected(false);
				validationMachineCheckbox.setSelected(false);
				visualisationCheckbox.setSelected(false);
				setFileNames(path, fileName);
			//}
		}
	}

	private void setFileNames(Path railML, String fileName) {
		railMLpath = railML.toAbsolutePath();
		generationPath = railMLpath.getParent().toAbsolutePath();
		dataFileName.setValue(fileName + "_data.mch");
		animationFileName.setValue(fileName + "_animation.mch");
		validationFileName.setValue(fileName + "_validation.rmch");
		svgFileName.setValue(fileName + ".svg");
		fileLocationField.setText(railMLpath.toAbsolutePath().toString());
		locationField.setText(generationPath.toString());
		railMLImportMeta.setPath(generationPath);
		railMLImportMeta.setName(fileName);
	}

	@FXML
	public void selectRailMLDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(i18n.translate("railml.stage.directorychooser.title"));
		Path path = fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.RAILML, stageManager.getCurrent());
		if(path != null) {
			generationPath = path.toAbsolutePath();
			locationField.setText(generationPath.toString());
			railMLImportMeta.setPath(generationPath);
		}
	}

	@FXML
	public void startImport() {

		//progressInfo.setVisible(true);
		//progressInfo.setManaged(true);
		//btStartImport.disableProperty().unbind();
		//btStartImport.setDisable(true);
		generateAnimation = animationMachineCheckbox.isSelected();
		generateValidation = validationMachineCheckbox.isSelected();
		generateSVG = visualisationCheckbox.isSelected();
		visualisationStrategy = visualisationStrategyChoiceBox.getValue();
		railMLImportMeta.setVisualisationStrategy(visualisationStrategy);

		clearProgressWithMessage("Initialize import");

		updater.execute(() -> {
			try {
				generateMachines();
				final String shortName = MoreFiles.getNameWithoutExtension(railMLpath);
				final Machine dataMachine = new Machine(dataFileName.getValue(), "Data machine generated from " + railMLpath.getFileName(), generationPath.relativize(generationPath.resolve(dataFileName.getValue())));
				final Machine animationMachine = new Machine(animationFileName.getValue(), "Animation machine generated from " + railMLpath.getFileName(), generationPath.relativize(generationPath.resolve(animationFileName.getValue())));
				final Machine validationMachine = new Machine(validationFileName.getValue(), "Validation machine generated from " + railMLpath.getFileName(), generationPath.relativize(generationPath.resolve(validationFileName.getValue())));

				if (!Thread.currentThread().isInterrupted()) {
					Platform.runLater(() -> {
						RailMLInspectDotStage railMLInspectDotStage = injector.getInstance(RailMLInspectDotStage.class);
						if (generateSVG) {
							clearProgressWithMessage("Create visualization");
							railMLInspectDotStage.initializeOptionsForStrategy(visualisationStrategy);
							railMLInspectDotStage.show();
							railMLInspectDotStage.toFront();
							try {
								railMLInspectDotStage.visualizeCustomGraph();
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							if (generateAnimation || generateValidation) {
								railMLInspectDotStage.setOnHidden(event -> createProject(shortName, generationPath, dataMachine, animationMachine, validationMachine));
							}
						} else {
							createProject(shortName, generationPath, dataMachine, animationMachine, validationMachine);
						}
						this.close();
					});
				}
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

	private void clearProgressWithMessage(String message) {
		progressDescription.setText(message);
		progressLabel.setText("");
		progressBar.setProgress(-1);
		progressOperation.setText("");
	}

	public void generateMachines() throws Exception {

		String graphMachineName = "rmch/RailML3_import.rmch";
		URI graphMachine = Objects.requireNonNull(getClass().getResource(graphMachineName)).toURI();
		Api api = injector.getInstance(Api.class);

		if ("jar".equals(graphMachine.getScheme())) {
			Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
			Path tempGraphMachine = tempDir.resolve(graphMachineName);
			Path tempGraphDefs = tempDir.resolve("RailML3_CustomGraphs.def");
			Path tempPrintMachine = tempDir.resolve("RailML3_printMachines.mch");
			Path tempImportMachine = tempDir.resolve("RailML3_import.mch");
			Path tempValidationMachine = tempDir.resolve("RailML3_validation_flat.mch");

			Files.copy(Objects.requireNonNull(getClass().getResourceAsStream(graphMachineName)), tempGraphMachine, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("RailML3_CustomGraphs.def")), tempGraphDefs, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("RailML3_printMachines.mch")), tempPrintMachine, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("RailML3_import.mch")), tempImportMachine, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("RailML3_validation_flat.mch")), tempValidationMachine, StandardCopyOption.REPLACE_EXISTING);

			stateSpace = api.b_load(tempGraphMachine.toString());

			Files.delete(tempGraphMachine);
			Files.delete(tempGraphDefs);
			Files.delete(tempPrintMachine);
			Files.delete(tempImportMachine);
			Files.delete(tempValidationMachine);
		} else {
			stateSpace = api.brules_load(Paths.get(graphMachine).toString());
		}

		stateSpace.addWarningListener(warnings -> {
			final List<ErrorItem> filteredWarnings = this.errorDisplayFilter.filterErrors(warnings);
			if (!filteredWarnings.isEmpty()) {
				Platform.runLater(() -> {
					final WarningAlert alert = injector.getInstance(WarningAlert.class);
					alert.getWarnings().setAll(filteredWarnings);
					alert.show();
				});
			}
		});

		if (!Thread.currentThread().isInterrupted()) {
			State currentState = stateSpace.getRoot()
				.perform("$setup_constants", "file = \"" + railMLpath + "\"")
				.perform("$initialise_machine");

			Platform.runLater(() -> clearProgressWithMessage("Executing:"));

			RulesChecker rulesChecker = new RulesChecker(stateSpace.getTrace(currentState.getId()));
			rulesChecker.init();
			RulesModel rulesModel = (RulesModel) stateSpace.getModel();
			int totalNrOfOperations = rulesModel.getRulesProject().getOperationsMap().values().
				stream().filter(op -> !(op instanceof FunctionOperation)).toList().size();
			int nrExecutedOperations = 0;
			// determine all operations that can be executed in this state
			Set<AbstractOperation> executableOperations = rulesChecker.getExecutableOperations();
			while (!executableOperations.isEmpty()) {
				for (AbstractOperation op : executableOperations) {
					rulesChecker.executeOperation(op);
					nrExecutedOperations++;
					updateProgress(nrExecutedOperations, totalNrOfOperations, op.getName());
				}
				executableOperations = rulesChecker.getExecutableOperations();
			}
			currentState = rulesChecker.getCurrentTrace().getCurrentState();

			boolean inv_ok = currentState.isInvariantOk();
			boolean import_success = currentState.eval("no_error = TRUE").toString().equals("TRUE");

			if (inv_ok && import_success) {
				Path dataPath = generationPath.resolve(dataFileName.getValue());
				if (generateAnimation || generateValidation) {
					clearProgressWithMessage("Generate Machines");

					String VARS_AS_TYPED_STRING_railML_identifiers = stateSpace.getLoadedMachine().getVariableNames().
						stream().filter(v -> v.startsWith("RailML3_")).collect(Collectors.joining(",\n    "));
					String VARS_AS_TYPED_STRING_railML_contents = currentState.eval("VARS_AS_TYPED_STRING(\"RailML3_\")", FormulaExpand.EXPAND)
						.toString().replace(" &", "\n    &").translateEscapes();
					VARS_AS_TYPED_STRING_railML_contents = VARS_AS_TYPED_STRING_railML_contents.substring(1, VARS_AS_TYPED_STRING_railML_contents.length() - 1);
					String all_ids = currentState.eval("all_ids", FormulaExpand.EXPAND).toString();
					String data = ""; // currentState.eval("data", FormulaExpand.EXPAND).toString();
					List<String> sets = stateSpace.getLoadedMachine().getSetNames().
						stream().filter(v -> v.startsWith("RailML3_")).toList();
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < sets.size(); i++) {
						sb.append(sets.get(i)).append(" = ").append(currentState.eval(sets.get(i)));
						if (i < sets.size() - 1) {
							sb.append(";\n    ");
						}
					}
					String SETS_railML = sb.toString();

					replaceOldFile(dataPath);
					RailMLMachinePrinter.printDataMachine(dataPath, MoreFiles.getNameWithoutExtension(dataPath), VARS_AS_TYPED_STRING_railML_identifiers,
						VARS_AS_TYPED_STRING_railML_contents, all_ids, data, SETS_railML);
				}
				if (generateAnimation) {
					Path path = generationPath.resolve(animationFileName.getValue());
					replaceOldFile(path);
					RailMLMachinePrinter.printAnimationMachine(path, MoreFiles.getNameWithoutExtension(path),
						MoreFiles.getNameWithoutExtension(dataPath), generateSVG, svgFileName.getValue());
				}
				if (generateValidation) {
					Path path = generationPath.resolve(validationFileName.getValue());
					replaceOldFile(path);
					RailMLMachinePrinter.printValidationMachine(path, MoreFiles.getNameWithoutExtension(path),
						MoreFiles.getNameWithoutExtension(dataPath));
				}
			}
			railMLImportMeta.setState(currentState);
		}
	}

	private void createProject(String shortName, Path projectLocation, Machine dataMachine, Machine animationMachine, Machine validationMachine) {
		boolean replacingProject = currentProject.confirmReplacingProject();
		if (replacingProject) {
			currentProject.switchTo(new Project(shortName, "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Project.metadataBuilder().build(), projectLocation), true);
			if (generateAnimation || generateValidation) {
				currentProject.addMachine(dataMachine);
			}
			if (generateAnimation) {
				try {
					replaceOldResourceFile(generationPath, "RailML3_VisB.def");
					replaceOldResourceFile(generationPath, "RailML3_SimB.json");
					final Machine animationDefinitions = new Machine("RailML3_VisB.def", "", generationPath.relativize(generationPath.resolve("RailML3_VisB.def")));
					Path simbPath = generationPath.resolve("RailML3_SimB.json");

					currentProject.addMachine(animationDefinitions);
					currentProject.addMachine(animationMachine);
					currentProject.startAnimation(animationMachine);
					currentProject.getCurrentMachine().simulationsProperty()
						.add(new SimulationModel(currentProject.getLocation().relativize(simbPath), Collections.emptyList()));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			if (generateValidation) {
				currentProject.addMachine(validationMachine);
				if (!generateAnimation) {
					currentProject.startAnimation(validationMachine);
				}
			}
		}
	}

	@FXML
	public void cancel() {
		updater.cancel(true);
		if (stateSpace != null) stateSpace.kill();
		this.close();
	}

	private void updateProgress(final int nrExecutedOperations, final int totalNrOfOperations, final String opName) {
		Platform.runLater(() -> {
			ProgressBar progressBar = this.progressBar;
			progressBar.setProgress((double) nrExecutedOperations / totalNrOfOperations);
			this.progressOperation.setText(opName);
			this.progressLabel.setText(" (" + nrExecutedOperations + "/" + totalNrOfOperations + ")");
		});
	}
}
