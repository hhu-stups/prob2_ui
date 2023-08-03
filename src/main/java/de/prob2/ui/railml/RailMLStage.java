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
import de.prob2.ui.internal.*;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
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
import java.nio.charset.StandardCharsets;
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
	private boolean importSuccess = true;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final Injector injector;

	private final MachineLoader machineLoader;

	private final I18n i18n;

	private final FileChooserManager fileChooserManager;
	private final BackgroundUpdater updater;
	private final RailMLFile railMLFile;

	@Inject
	public RailMLStage(final StageManager stageManager, final CurrentProject currentProject,
	                      final Injector injector, final MachineLoader machineLoader, final I18n i18n,
	                      final FileChooserManager fileChooserManager, final StopActions stopActions,
	                      final RailMLFile railMLFile) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		this.machineLoader = machineLoader;
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
			this.railMLpath = railML;
			this.generationPath = railMLpath.getParent().toAbsolutePath();
			animationFileName.setValue(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()) + "_animation.mch");
			validationFileName.setValue(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()) + "_validation.rmch");
			svgFileName.setValue(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()) + ".svg");
			fileLocationField.setText(railMLpath.toAbsolutePath().toString());
			locationField.setText(generationPath.toString());
			railMLFile.setPath(generationPath);
		}

		//progressInfo.visibleProperty().bind(new SimpleBooleanProperty(updater.runningProperty());
		//progressInfo.managedProperty().bind(progressInfo.visibleProperty());
		//currentOp.textProperty().bind(currentOperation);
	}

	@FXML
	public void selectRailMLFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("railml.stage.directorychooser.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getRailMLFilter());
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.RAILML, stageManager.getCurrent());
		/*if(path != null) {
			Path resolvedPath = currentProject.getLocation().relativize(path);
			currentProject.getCurrentMachine().simulationsProperty().add(new SimulationModel(resolvedPath, Collections.emptyList()));
		};*/
		if(path != null) {
			generateFileList.clear();
			animationMachineCheckbox.setSelected(false);
			validationMachineCheckbox.setSelected(false);
			visualisationCheckbox.setSelected(false);
			this.railMLpath = path;
			this.generationPath = railMLpath.getParent().toAbsolutePath();
			animationFileName.setValue(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()) + "_animation.mch");
			validationFileName.setValue(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()) + "_validation.rmch");
			svgFileName.setValue(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()) + ".svg");
			fileLocationField.setText(railMLpath.toAbsolutePath().toString());
			locationField.setText(generationPath.toString());
			railMLFile.setPath(generationPath);
			railMLFile.setName(MoreFiles.getNameWithoutExtension(railMLpath.getFileName()));
		}
	}

	@FXML
	public void selectRailMLDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(i18n.translate("railml.stage.directorychooser.title"));
		Path path = fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.RAILML, stageManager.getCurrent());
		/*if(path != null) {
			Path resolvedPath = currentProject.getLocation().relativize(path);
			currentProject.getCurrentMachine().simulationsProperty().add(new SimulationModel(resolvedPath, Collections.emptyList()));
		};*/
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
				final Path relative = projectLocation.relativize(generationPath);
				final String shortName = animationFileName.getValue();
				final Machine animationMachine = new Machine(animationFileName.getValue(), "Animation machine generated from " + railMLpath.getFileName(), Paths.get(generationPath.toString()).resolve(animationFileName.getValue()));
				final Machine validationMachine = new Machine(validationFileName.getValue(), "Validation machine generated from " + railMLpath.getFileName(), Paths.get(generationPath.toString()).resolve(validationFileName.getValue()));

				if (!Thread.currentThread().isInterrupted()) {
					Platform.runLater(() -> {
						this.close();
						RailMLInspectDotStage railMLInspectDotStage = injector.getInstance(RailMLInspectDotStage.class);
						if (visualisationCheckbox.isSelected()) {
							railMLInspectDotStage.initializeCheckboxes();
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
			/*if (animationMachineCheckbox.isSelected()) {
				currentProject.startAnimation(animationMachine);
			}*/
			}
		}
		if (animationMachineCheckbox.isSelected()) {
			currentProject.addMachine(animationMachine);
			currentProject.startAnimation(animationMachine);
			Path defFile, simB;
			try {
				defFile = Paths.get(getClass().getResource("RailML_animation.def").toURI());
				File currentDefs = new File(generationPath.resolve("RailML_animation.def").toString());
				if (!currentDefs.exists() || (currentDefs.exists() && !Arrays.equals(Files.readAllBytes(currentDefs.toPath()), Files.readAllBytes(defFile)))) {
					Files.copy(defFile, currentDefs.toPath());
				}
				final Machine animationDefinitions = new Machine("RailML_animation.def", "", Paths.get(generationPath.toString()).resolve("RailML_animation.def"));
				currentProject.addMachine(animationDefinitions);

				simB = Paths.get(getClass().getResource("railml_simb.json").toURI());
				File currentSimB = new File(generationPath.resolve("railml_simb.json").toString());
				if (!currentSimB.exists() || (currentSimB.exists() && !Arrays.equals(Files.readAllBytes(currentSimB.toPath()), Files.readAllBytes(simB)))) {
					Files.copy(simB, currentSimB.toPath());
				}
				Path resolvedPath = currentProject.getLocation().relativize(currentSimB.toPath());
				currentProject.getCurrentMachine().simulationsProperty().add(new SimulationModel(resolvedPath, Collections.emptyList()));

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

		/*
		final Path tempDotFile;
		try {
			tempDotFile = Files.createTempFile("railml-", ".dot");
		} catch (IOException e) {
			throw new ProBError("Failed to create temporary dot file", e);
		}

		DotVisualizationCommand customGraph = getByName("custom_graph", currentState);
		byte[] dotInput = customGraph.visualizeAsDotToBytes(Collections.emptyList());
		Files.write(tempDotFile, dotInput);
		tempDotFile.toFile().deleteOnExit();

		final Path tempSvgFileDot, tempSvgFileVisB;
		try {
			tempSvgFileDot = Files.createTempFile("railml-", ".svg");
			tempSvgFileVisB = Files.createTempFile("railml-", ".svg");
		} catch (IOException e) {
			throw new ProBError("Failed to create temporary svg files", e);
		}

		customGraph.visualizeAsSvgToFile(tempSvgFileDot, Collections.emptyList());
		RailMLSvgConverter.convertSvgForVisB(tempSvgFileDot.toString(), "VIS");

		//Path svgPath = Paths.get(generationPath.toString()).resolve(svgFileName.getValue());
		//setVisBPath();
		/*try {

			Files.write(new DotCall("custom_graph")
				.layoutEngine(stateSpace.getCurrentPreference("DOT_ENGINE"))
				.outputFormat(DotOutputFormat.SVG)
				.input(dotInput)
				.call(), tempDotFile.toFile());
		} catch (IOException | InterruptedException e) {

			//LOGGER.error("Failed to save file converted from dot", e);
		}*/
	}

	private void writeAnimationMachine() throws IOException {

		File animationMachine = new File(Paths.get(generationPath.toString()).resolve(animationFileName.getValue()).toString());
		animationMachine.createNewFile();
		OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(animationMachine.toPath()), StandardCharsets.UTF_8);
		Map<IEvalElement, AbstractEvalResult> variableConstants = currentState.getVariableValues(FormulaExpand.EXPAND);
		writer.write("/*@generated */\nMACHINE " + MoreFiles.getNameWithoutExtension(animationMachine.toPath()) + "\n");
		writer.write("CONSTANTS\n");
		for (IEvalElement e : variableConstants.keySet()) {
			if (e.getPrettyPrint().startsWith("RailML"))
				writer.write("\t" + e.getPrettyPrint() + ",\n");
		}
		writer.write("PROPERTIES\n");
		for (IEvalElement e : variableConstants.keySet()) {
			if (e.getPrettyPrint().startsWith("RailML"))
				writer.write("\t& " + e.getPrettyPrint() + " = " + variableConstants.get(e).toString() + "\n");
		}
	}

	@FXML
	public void cancel() {
		updater.shutdownNow();
		this.close();
	}
}

/*private void performComputation(String computation) {
		if (importSuccess) {
			currentState = currentState.perform(computation);
			updateProgress(computation);
		}
	}

	private void performErrorRule(String errorRule) {
		if (importSuccess) {
			currentState = currentState.perform(errorRule);
			updateProgress(errorRule);
			System.out.println(currentState.getVariableValues(FormulaExpand.EXPAND).toString());
			AbstractEvalResult ruleVar = currentState.eval(errorRule);
			if (ruleVar.toString().equals("\"FAIL\"")) {
				importSuccess = false;
				System.out.println(currentState.eval(errorRule + "_Counterexamples").toString());
			}
		}
	}

	private void performWarningRule(String warningRule) {
		if (importSuccess) {
			currentState = currentState.perform(warningRule);
			updateProgress(warningRule);
			AbstractEvalResult ruleVar = currentState.eval(warningRule);
			if (ruleVar.toString().equals("\"FAIL\"")) {
				System.out.println(currentState.eval(warningRule + "_Counterexamples").toString());
			}
		}
	}

	private void updateProgress(final String op) {
		Platform.runLater(() -> {
			progress.setValue(progress.getValue() + 1.0);
			progressBar.setProgress(progress.getValue() / 3.0);
			currentOperation.setValue(op);
		});
	}

		// RailML_readFile
		performErrorRule("is_supported_railml");
		performErrorRule("unique_ids");
		performComputation("read_file");

		// BEGIN RailML3_CO
		performComputation("set_name");
		performErrorRule("is_valid_name");
		performComputation("set_CO_NAME");
		// END RailML3_CO

		// BEGIN RailML3_IS
		performComputation("set_infrastructure");
		performErrorRule("is_valid_infrastructure");

		performComputation("set_topology");
		performErrorRule("is_valid_topology");

		performComputation("set_netElements");
		performErrorRule("is_valid_netElements");
		performComputation("set_netElement");
		performErrorRule("is_valid_netElement");
		performComputation("set_NET_ELEMENT");
		performWarningRule("warnings_netElement");

		performComputation("set_netRelations");
		performErrorRule("is_valid_netRelations");
		performComputation("set_netRelation");
		performErrorRule("is_valid_netRelation");
		performComputation("set_NET_RELATION");
		performWarningRule("warnings_netRelation");

		performComputation("set_networks");
		performErrorRule("is_valid_networks");
		performComputation("set_network");
		performErrorRule("is_valid_network");
		performComputation("set_level");
		performErrorRule("is_valid_level");
		performComputation("set_NETWORK");
		performErrorRule("validate_level");
		performWarningRule("warnings_level");

		performComputation("set_functionalInfrastructure");
		performErrorRule("is_valid_functionalInfrastructure");

		performComputation("set_borders");
		performErrorRule("is_valid_borders");
		performComputation("set_border");
		performErrorRule("is_valid_border");
		performComputation("set_BORDER");

		performComputation("set_bufferStops");
		performErrorRule("is_valid_bufferStops");
		performComputation("set_bufferStop");
		performErrorRule("is_valid_bufferStop");

		performComputation("set_crossings");
		performErrorRule("is_valid_crossings");
		performComputation("set_crossing");
		performErrorRule("is_valid_crossing");
		performComputation("set_CROSSING");

		performComputation("set_derailersIS");
		performErrorRule("is_valid_derailersIS");
		performComputation("set_derailerIS");
		performErrorRule("is_valid_derailerIS");

		performComputation("set_operationalPoints");
		performErrorRule("is_valid_operationalPoints");
		performComputation("set_operationalPoint");
		performErrorRule("is_valid_operationalPoint");

		performComputation("set_signalsIS");
		performErrorRule("is_valid_signalsIS");
		performComputation("set_signalIS");
		performErrorRule("is_valid_signalIS");

		performComputation("set_switchesIS");
		performErrorRule("is_valid_switchesIS");
		performComputation("set_switchIS");
		performErrorRule("is_valid_switchIS");
		performComputation("set_SWITCH");
		performErrorRule("validate_switchIS");

		performComputation("set_tracks");
		performErrorRule("is_valid_tracks");
		performComputation("set_track");
		performErrorRule("is_valid_track");

		performComputation("set_trainDetectionElements");
		performErrorRule("is_valid_trainDetectionElements");
		performComputation("set_trainDetectionElement");
		performErrorRule("is_valid_trainDetectionElement");

		performComputation("set_linearLocation");
		performErrorRule("is_valid_linearLocation");
		performComputation("set_LINEAR_LOCATION");
		performWarningRule("warnings_linearLocation");

		performComputation("set_spotLocation");
		performErrorRule("is_valid_spotLocation");
		performComputation("set_SPOT_LOCATION");
		performErrorRule("validate_spotLocation");
		performWarningRule("warnings_spotLocation");

		performComputation("set_linearCoordinate");
		performErrorRule("is_valid_linearCoordinate");
		//runComputation(set_LINEAR_COORDINATE);

		performComputation("set_SIGNAL");
		performComputation("set_NET_RELATION_SUBSEQUENT_BLOCKS");
		performErrorRule("validate_linearLocation");
		performComputation("set_TRACK");

		performErrorRule("validate_border");
		performErrorRule("validate_bufferStop");
		// END RailML3_IS

		// BEGIN RailML3_VIS
		performComputation("set_visualizations");
		performErrorRule("is_valid_visualizations");

		performComputation("set_infrastructureVisualizations");
		performErrorRule("is_valid_infrastructureVisualizations");
		performComputation("set_infrastructureVisualization");
		performErrorRule("is_valid_infrastructureVisualization");

		performComputation("set_spotElementProjection");
		performErrorRule("is_valid_spotElementProjection");

		performComputation("set_linearElementProjection");
		performErrorRule("is_valid_linearElementProjection");

		performComputation("set_NET_ELEMENT_COORDINATES");
		// END RailML3_VIS

		// BEGIN RailML3_IL
		performComputation("set_interlocking");
		performErrorRule("is_valid_interlocking");

		performComputation("set_assetsForInterlockings");
		performErrorRule("is_valid_assetsForInterlockings");
		performComputation("set_assetsForInterlocking");
		performErrorRule("is_valid_assetsForInterlocking");

		performComputation("set_tvdSections");
		performErrorRule("is_valid_tvdSections");
		performComputation("set_tvdSection");
		performErrorRule("is_valid_tvdSection");
		performComputation("set_TVD_SECTION_IDS");

		performComputation("set_derailersIL");
		performErrorRule("is_valid_derailersIL");
		performComputation("set_derailerIL");
		performErrorRule("is_valid_derailerIL");
		performComputation("set_IL_DERAILER_IDS");

		performComputation("set_movableCrossings");
		performErrorRule("is_valid_movableCrossings");
		performComputation("set_movableCrossing");
		performErrorRule("is_valid_movableCrossing");
		performComputation("set_IL_MOVABLE_CROSSING_IDS");

		performComputation("set_signalsIL");
		performErrorRule("is_valid_signalsIL");
		performComputation("set_signalIL");
		performErrorRule("is_valid_signalIL");
		performComputation("set_IL_SIGNAL_IDS");

		performComputation("set_switchesIL");
		performErrorRule("is_valid_switchesIL");
		performComputation("set_switchIL");
		performErrorRule("is_valid_switchIL");
		performComputation("set_IL_SWITCH_IDS");
		//runErrorRule(validate_switchIL);

		performComputation("set_IL_MOVABLE_CROSSING");
		performComputation("set_IL_DERAILER");
		performComputation("set_IL_SWITCH");

		performComputation("set_routeReleaseGroupsAhead");
		performErrorRule("is_valid_routeReleaseGroupsAhead");
		performComputation("set_routeReleaseGroupAhead");
		performErrorRule("is_valid_routeReleaseGroupAhead");
		performComputation("set_IL_ROUTE_RELEASE_GROUP_AHEAD");

		performComputation("set_routeReleaseGroupsRear");
		performErrorRule("is_valid_routeReleaseGroupsRear");
		performComputation("set_routeReleaseGroupRear");
		performErrorRule("is_valid_routeReleaseGroupRear");
		performComputation("set_IL_ROUTE_RELEASE_GROUP_REAR");

		performComputation("set_routes");
		performErrorRule("is_valid_routes");
		performComputation("set_route");
		performErrorRule("is_valid_route");
		performComputation("set_ROUTE_IDS");

		performComputation("set_overlaps");
		performErrorRule("is_valid_overlaps");
		performComputation("set_overlap");
		performErrorRule("is_valid_overlap");
		performWarningRule("warnings_overlap");
		performComputation("set_IL_OVERLAP");

		performComputation("set_routeRelations");
		performErrorRule("is_valid_routeRelations");
		performComputation("set_routeRelation");
		performErrorRule("is_valid_routeRelation");
		performComputation("set_IL_ROUTE_RELATION");

		performComputation("set_ROUTE");
		performErrorRule("validate_route_errors");
		performWarningRule("warnings_route");

		performComputation("set_conflictingRoutes");
		performErrorRule("is_valid_conflictingRoutes");
		performComputation("set_conflictingRoute");
		performErrorRule("is_valid_conflictingRoute");
		performComputation("set_CONFLICTING_ROUTE");

		performComputation("set_TVD_SECTIONS");
		performComputation("set_ROUTES_TVD_SECTIONS");
		performErrorRule("validate_tvdSection_errors");

		performComputation("set_specificInfrastructureManagers");
		performErrorRule("is_valid_specificInfrastructureManagers");
		performComputation("set_specificInfrastructureManager");
		performErrorRule("is_valid_specificInfrastructureManager");
		performComputation("set_IL_SPECIFIC_INFRASTRUCTURE_MANAGER_IDS");
		performComputation("set_IL_SPECIFIC_INFRASTRUCTURE_MANAGER");

		performComputation("set_signalBoxes");
		performErrorRule("is_valid_signalBoxes");
		performComputation("set_signalBox");
		performErrorRule("is_valid_signalBox");
		performComputation("set_IL_SIGNAL_BOX_IDS");
		performComputation("set_IL_SIGNAL_BOX");
		// END RailML3_IL
 */
