package de.prob2.ui.railml;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.*;
import de.prob.exception.ProBError;
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
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

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
	private ChoiceBox<RailMLImportMeta.VisualisationStrategy> visualisationStrategyChoiceBox;
	private RailMLImportMeta.VisualisationStrategy visualisationStrategy;

	@FXML
	private HBox progressInfo;
	@FXML
	private ProgressBar progressBar;
	private final SimpleDoubleProperty progress = new SimpleDoubleProperty(0.0);
	@FXML
	private Label currentOp;
	private final SimpleStringProperty currentOperation = new SimpleStringProperty("");

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

		updater.execute(() -> {
			try {
				generateMachines();
				final Path projectLocation = generationPath;
				final String shortName = MoreFiles.getNameWithoutExtension(railMLpath);
				final Machine dataMachine = new Machine(dataFileName.getValue(), "Data machine generated from " + railMLpath.getFileName(), Paths.get(generationPath.toString()).resolve(dataFileName.getValue()));
				final Machine animationMachine = new Machine(animationFileName.getValue(), "Animation machine generated from " + railMLpath.getFileName(), Paths.get(generationPath.toString()).resolve(animationFileName.getValue()));
				final Machine validationMachine = new Machine(validationFileName.getValue(), "Validation machine generated from " + railMLpath.getFileName(), Paths.get(generationPath.toString()).resolve(validationFileName.getValue()));

				if (!Thread.currentThread().isInterrupted()) {
					Platform.runLater(() -> {
						RailMLInspectDotStage railMLInspectDotStage = injector.getInstance(RailMLInspectDotStage.class);
						if (generateSVG) {
							railMLInspectDotStage.initializeOptionsForStrategy(visualisationStrategy);
							railMLInspectDotStage.show();
							railMLInspectDotStage.toFront();
							try {
								railMLInspectDotStage.visualizeCustomGraph(Collections.emptyList());
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							if (generateAnimation || generateValidation) {
								railMLInspectDotStage.setOnHidden(event -> createProject(shortName, projectLocation, dataMachine, animationMachine, validationMachine));
							}
						} else {
							createProject(shortName, projectLocation, dataMachine, animationMachine, validationMachine);
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

	public void generateMachines() throws Exception {

		String graphMachineName;
		if (visualisationStrategy == RailMLImportMeta.VisualisationStrategy.D4R) {
			graphMachineName = "RailML3_D4R_CustomGraph.mch";
		} else if (visualisationStrategy == RailMLImportMeta.VisualisationStrategy.RAIL_OSCOPE) {
			graphMachineName = "RailML3_NOR_CustomGraph.mch";
		} else {
			graphMachineName = "RailML3_DOT_CustomGraph.mch";
		}
		URI graphMachine = getClass().getResource(graphMachineName).toURI();

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
			stateSpace = api.b_load(Paths.get(graphMachine).toString());
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

		String linkSvg, fullImport;
		if (generateSVG) {
			linkSvg = "TRUE";
			if (!generateAnimation && !generateValidation) {
				fullImport = "FALSE";
			} else {
				fullImport = "TRUE";
			}
		} else {
			linkSvg = "FALSE";
			fullImport = "TRUE";
		}

		if (!Thread.currentThread().isInterrupted()) {
			State currentState = stateSpace.getRoot()
				.perform("$setup_constants", "file = \"" + railMLpath + "\"" +
					"  & outputDataFile = \"" + generationPath.resolve(dataFileName.getValue()) + "\"\n" +
					"  & outputAnimationFile = \"" + generationPath.resolve(animationFileName.getValue()) + "\"\n" +
					"  & outputValidationFile = \"" + generationPath.resolve(validationFileName.getValue()) + "\"\n" +
					"  & svgFile = \"" + svgFileName.getValue() + "\"\n" +
					"  & LINK_SVG = " + linkSvg + "\n" +
					"  & FULL_IMPORT = " + fullImport + "\n" +
					"  & dataMachineName = \"" + dataFileName.getValue().split(".mch")[0] + "\"" +
					"  & animationMachineName = \"" + animationFileName.getValue().split(".mch")[0] + "\"" +
					"  & validationMachineName = \"" + validationFileName.getValue().split(".rmch")[0] + "\"")
				.perform("$initialise_machine");

			boolean inv_ok = currentState.isInvariantOk();
			boolean import_success = currentState.eval("no_error = TRUE").toString().equals("TRUE");

			if (inv_ok && import_success) {
				if (generateAnimation || generateValidation) {
					replaceOldFile(generationPath.resolve(dataFileName.getValue()));
					currentState.perform("triggerPrintData").perform("printDataMachine");
				}
				if (generateAnimation) {
					replaceOldFile(generationPath.resolve(animationFileName.getValue()));
					currentState.perform("triggerPrintAnimation").perform("printAnimationMachine");
				}
				if (generateValidation) {
					replaceOldFile(generationPath.resolve(validationFileName.getValue()));
					currentState.perform("triggerPrintValidation").perform("printValidationMachine");
				}
			}
			railMLImportMeta.setState(currentState);
		}
	}

	private void createProject(String shortName, Path projectLocation, Machine dataMachine, Machine animationMachine, Machine validationMachine) {
		if (currentProject.getLocation() == null) {
			boolean replacingProject = currentProject.confirmReplacingProject();
			if (replacingProject) {
				currentProject.switchTo(new Project(shortName, "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Project.metadataBuilder().build(), projectLocation), true);
			}
		}
		if (generateAnimation || generateValidation) {
			currentProject.addMachine(dataMachine);
		}
		if (generateAnimation) {
			try {
				replaceOldResourceFile(generationPath, "RailML3_VisB.def");
				replaceOldResourceFile(generationPath, "RailML3_SimB.json");
				Path simbPath = generationPath.resolve("RailML3_SimB.json");

				final Machine animationDefinitions = new Machine("RailML3_VisB.def", "", generationPath.resolve("RailML3_VisB.def"));
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

	@FXML
	public void cancel() {
		updater.cancel(true);
		if (stateSpace != null) stateSpace.kill();
		this.close();
	}
}

/*private void updateProgress(final String op) {
		Platform.runLater(() -> {
			progress.setValue(progress.getValue() + 1.0);
			progressBar.setProgress(progress.getValue() / 3.0);
			currentOperation.setValue(op);
		});
	}
 */
