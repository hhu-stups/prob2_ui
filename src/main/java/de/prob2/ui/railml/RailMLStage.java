package de.prob2.ui.railml;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.*;
import de.prob.scripting.Api;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob2.ui.config.FileChooserManager;
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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static de.prob.animator.domainobjects.DotVisualizationCommand.getByName;

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

	protected enum VisualisationStrategy {D4R, RAIL_OSCOPE, DOT};
	@FXML
	private ChoiceBox<VisualisationStrategy> visualisationStrategyChoiceBox;

	@FXML
	private HBox progressInfo;
	@FXML
	private ProgressBar progressBar;
	private final SimpleDoubleProperty progress = new SimpleDoubleProperty(0.0);
	@FXML
	private Label currentOp;
	private final SimpleStringProperty currentOperation = new SimpleStringProperty("");

	private Path railMLpath, generationPath;

	private final SimpleStringProperty animationFileName = new SimpleStringProperty("");
	private final SimpleStringProperty animationDefsFileName = new SimpleStringProperty("RailML_animation.def");
	private final SimpleStringProperty validationFileName = new SimpleStringProperty("");
	private final SimpleStringProperty svgFileName = new SimpleStringProperty("");
	private final SimpleStringProperty simBFileName = new SimpleStringProperty("railml_simb.json");

	private State currentState;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final Injector injector;

	private final I18n i18n;

	private final FileChooserManager fileChooserManager;
	private final BackgroundUpdater updater;
	private final RailMLFile railMLFile;

	@Inject
	public RailMLStage(final StageManager stageManager, final CurrentProject currentProject,
	                      final Injector injector, final I18n i18n, final FileChooserManager fileChooserManager,
	                      final StopActions stopActions, final RailMLFile railMLFile) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.updater = new BackgroundUpdater("railml import executor");
		stopActions.add(this.updater::shutdownNow);
		this.railMLFile = railMLFile;
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
		visualisationStrategyChoiceBox.getItems().addAll(VisualisationStrategy.values());
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
		visualisationStrategyChoiceBox.setValue(VisualisationStrategy.DOT);
		visualisationCheckbox.selectedProperty()
			.addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				generateFileList.add(svgFileName.getValue());
			} else {
				generateFileList.remove(svgFileName.getValue());
			}
		});

		progressInfo.setManaged(false);
		progressInfo.setVisible(false);
		//progressBar.progressProperty().bind(progress);
		//currentOp.textProperty().bind(currentOperation);
		Path railML = railMLFile.getPath();
		if(railML != null) {
			setFileNames(railML);
		}

		progressInfo.visibleProperty().bind(updater.runningProperty());
		progressInfo.managedProperty().bind(progressInfo.visibleProperty());
		//currentOp.textProperty().bind(currentOperation);

		setOnCloseRequest(e -> {
			this.cancel();
		});
	}

	@FXML
	public void selectRailMLFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("railml.stage.filechooser.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getRailMLFilter());
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.RAILML, stageManager.getCurrent());
		if(path != null) {
			generateFileList.clear();
			animationMachineCheckbox.setSelected(false);
			validationMachineCheckbox.setSelected(false);
			visualisationCheckbox.setSelected(false);
			setFileNames(path);
		}
	}

	private void setFileNames(Path railML) {
		this.railMLpath = railML;
		this.generationPath = railMLpath.getParent().toAbsolutePath();
		animationFileName.setValue(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()) + "_animation.mch");
		validationFileName.setValue(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()) + "_validation.rmch");
		svgFileName.setValue(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()) + ".svg");
		fileLocationField.setText(railMLpath.toAbsolutePath().toString());
		locationField.setText(generationPath.toString());
		railMLFile.setPath(generationPath);
		railMLFile.setName(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()));
	}

	@FXML
	public void selectRailMLDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(i18n.translate("railml.stage.directorychooser.title"));
		Path path = fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.RAILML, stageManager.getCurrent());
		if(path != null) {
			this.generationPath = path.toAbsolutePath();
			locationField.setText(generationPath.toString());
		}
	}

	@FXML
	public void startImport() {

		//progressInfo.setVisible(true);
		//progressInfo.setManaged(true);
		//btStartImport.disableProperty().unbind();
		//btStartImport.setDisable(true);

		this.updater.execute(() -> {
			try {
				generateMachines();
				final Path projectLocation = generationPath;
				final String shortName = animationFileName.getValue();
				final Machine animationMachine = new Machine(animationFileName.getValue(), "Animation machine generated from " + railMLpath.getFileName(), Paths.get(generationPath.toString()).resolve(animationFileName.getValue()));
				final Machine validationMachine = new Machine(validationFileName.getValue(), "Validation machine generated from " + railMLpath.getFileName(), Paths.get(generationPath.toString()).resolve(validationFileName.getValue()));

				if (!Thread.currentThread().isInterrupted()) {
					Platform.runLater(() -> {
						this.close();
						RailMLInspectDotStage railMLInspectDotStage = injector.getInstance(RailMLInspectDotStage.class);
						if (visualisationCheckbox.isSelected()) {
							railMLInspectDotStage.initializeOptionsForStrategy(railMLFile.getVisualisationStrategy());
							railMLInspectDotStage.show();
							railMLInspectDotStage.toFront();
							DotVisualizationCommand customGraph = getByName("custom_graph", currentState);
							try {
								railMLInspectDotStage.visualizeInternal(customGraph, Collections.emptyList());
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							railMLInspectDotStage.setOnHidden(event -> {
								createProject(shortName, projectLocation, animationMachine, validationMachine);
							});
						} else {
							createProject(shortName, projectLocation, animationMachine, validationMachine);
						}
					});
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void createProject(String shortName, Path projectLocation, Machine animationMachine, Machine validationMachine) {
		if (currentProject.getLocation() == null) {
			boolean replacingProject = currentProject.confirmReplacingProject();
			if (replacingProject) {
				currentProject.switchTo(new Project(shortName, "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Project.metadataBuilder().build(), projectLocation), true);
			}
		}
		if (animationMachineCheckbox.isSelected()) {
			try {
				Path defFile = Paths.get(getClass().getResource("RailML_animation.def").toURI());
				File currentDefs = new File(generationPath.resolve("RailML_animation.def").toString());
				if (!currentDefs.exists() || (currentDefs.exists() && !Arrays.equals(Files.readAllBytes(currentDefs.toPath()), Files.readAllBytes(defFile)))) {
					// TODO: file has changed: request replace
					Files.copy(defFile, currentDefs.toPath());
				}
				final Machine animationDefinitions = new Machine("RailML_animation.def", "", Paths.get(generationPath.toString()).resolve("RailML_animation.def"));
				currentProject.addMachine(animationDefinitions);

				Path simBResource = Paths.get(getClass().getResource("railml_simb.json").toURI());
				Path simBPath = generationPath.resolve("railml_simb.json");
				File currentSimB = new File(simBPath.toString());
				if (!currentSimB.exists() || (currentSimB.exists() && !Arrays.equals(Files.readAllBytes(currentSimB.toPath()), Files.readAllBytes(simBResource)))) {
					Files.copy(simBResource, currentSimB.toPath());
				}
				currentProject.addMachine(animationMachine);
				currentProject.startAnimation(animationMachine);
				currentProject.getCurrentMachine().simulationsProperty()
					.add(new SimulationModel(currentProject.getLocation().relativize(simBPath.toAbsolutePath()), Collections.emptyList()));
			} catch (IOException | URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
		if (validationMachineCheckbox.isSelected()) {
			currentProject.addMachine(validationMachine);
		}
	}

	public void generateMachines() throws Exception {

		VisualisationStrategy visualisationStrategy = visualisationStrategyChoiceBox.getValue();
		railMLFile.setVisualisationStrategy(visualisationStrategy);
		Path graphMachine;
		if (visualisationStrategy == VisualisationStrategy.D4R) {
			graphMachine = Paths.get(getClass().getResource("RailML3_D4R_CustomGraph.mch").toURI());
		} else if (visualisationStrategy == VisualisationStrategy.RAIL_OSCOPE) {
			graphMachine = Paths.get(getClass().getResource("RailML3_NOR_CustomGraph.mch").toURI());
		} else {
			graphMachine = Paths.get(getClass().getResource("RailML3_DOT_CustomGraph.mch").toURI());
		}

		Api api = injector.getInstance(Api.class);
		StateSpace stateSpace = api.b_load(graphMachine.toAbsolutePath().toString());
		ClassicalB no_error = new ClassicalB("no_error = TRUE");
		stateSpace.subscribe(this, no_error);

		// TODO: Adapt invalid machine names

		currentState = stateSpace.getRoot()
			.perform("$setup_constants", "file = \"" + railMLpath + "\"" +
				"  & outputAnimationFile = \"" + generationPath.resolve(animationFileName.getValue()) + "\"\n" +
				"  & outputValidationFile = \"" + generationPath.resolve(validationFileName.getValue()) + "\"\n" +
				"  & svgFile = \"" + svgFileName.getValue() + "\"\n" +
				"  & animationMachineName = \"" + animationFileName.getValue().split(".mch")[0] + "\"" +
				"  & validationMachineName = \"" + validationFileName.getValue().split(".rmch")[0] + "\"")
			.perform("$initialise_machine");

		// state.getStateErrors();
		boolean inv_ok = currentState.isInvariantOk();
		boolean import_success = currentState.getValues().get(no_error).toString().equals("TRUE");

		if (animationMachineCheckbox.isSelected() && inv_ok && import_success) {
			File animationMachine = new File(Paths.get(generationPath.toString()).resolve(animationFileName.getValue()).toString());
			animationMachine.delete(); // TODO: Confirm
			animationMachine.createNewFile();
			currentState.perform("triggerPrintAnimation").perform("printAnimationMachine");
		}
		if (validationMachineCheckbox.isSelected() && inv_ok && import_success) {
			File validationMachine = new File(Paths.get(generationPath.toString()).resolve(validationFileName.getValue()).toString());
			validationMachine.delete(); // TODO: Confirm
			validationMachine.createNewFile();
			currentState.perform("triggerPrintValidation").perform("printValidationMachine");
		}
		railMLFile.setState(currentState);
	}

	@FXML
	public void cancel() {
		updater.cancel(true);
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
