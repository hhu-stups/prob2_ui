package de.prob2.ui.railml;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.hhu.stups.railml2b.RailML2B;
import de.hhu.stups.railml2b.internal.RailML2BFactory;
import de.hhu.stups.railml2b.load.ImportArguments;
import de.hhu.stups.railml2b.load.ImportArguments.ImportArgumentsBuilder;
import de.prob.exception.ProBError;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static de.hhu.stups.railml2b.output.MachinePrinter.*;

@FXMLInjected
@Singleton
public class RailMLStage extends Stage {

	@FXML
	private Button btStartImport;
	@FXML
	private TextField fileLocationField;
	@FXML
	private Tooltip fileLocationTooltip;
	@FXML
	private TextField locationField;
	@FXML
	private Tooltip locationTooltip;
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
	private ChoiceBox<ImportArguments.VisualisationStrategy> visualisationStrategyChoiceBox;
	private ImportArguments.ImportArgumentsBuilder importArguments = new ImportArguments.ImportArgumentsBuilder(null);
	private String modelName = null;

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
	                   final StopActions stopActions) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.updater = new BackgroundUpdater("railml2b");
		stopActions.add(this::cancel);
		stageManager.loadFXML(this, "railml_stage.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		btStartImport.disableProperty()
			.bind(fileLocationField.lengthProperty().lessThanOrEqualTo(0)
			.or(animationMachineCheckbox.selectedProperty().not()
				.and(validationMachineCheckbox.selectedProperty().not()
				.and(visualisationCheckbox.selectedProperty().not())))
			.or(visualisationCheckbox.selectedProperty()
				.and(visualisationStrategyChoiceBox.valueProperty().isNull()))
			.or(updater.runningProperty()));
		fileLocationField.setText("");
		fileLocationTooltip.textProperty().bind(fileLocationField.textProperty());
		locationField.setText("");
		locationTooltip.textProperty().bind(locationField.textProperty());
		visualisationStrategyField.visibleProperty()
			.bind(visualisationCheckbox.selectedProperty());
		visualisationStrategyChoiceBox.getItems().addAll(ImportArguments.VisualisationStrategy.values());
		visualisationStrategyChoiceBox.visibleProperty().bind(visualisationCheckbox.selectedProperty());

		generateFileListView.setItems(generateFileList);
		generatedFiles.visibleProperty().bind(Bindings.isEmpty(generateFileList).not());
		generatedFiles.managedProperty().bind(generatedFiles.visibleProperty());
		generateFileListView.setFixedCellSize(24);
		generateFileListView.prefHeightProperty().bind(Bindings.size(generateFileList).multiply(generateFileListView.getFixedCellSize()).add(2));
		animationMachineCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				generateFileList.addAll(modelName + ANIMATION_MCH, VISB_DEF, SIMB_JSON);
			} else {
				generateFileList.removeAll(modelName + ANIMATION_MCH, VISB_DEF, SIMB_JSON);
			}
		});
		validationMachineCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				generateFileList.add(modelName + VALIDATION_MCH);
			} else {
				generateFileList.remove(modelName + VALIDATION_MCH);
			}
		});
		animationMachineCheckbox.selectedProperty().or(validationMachineCheckbox.selectedProperty()).addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				generateFileList.add(modelName + DATA_MCH);
			} else {
				generateFileList.remove(modelName + DATA_MCH);
			}
		});
		visualisationStrategyChoiceBox.setValue(ImportArguments.VisualisationStrategy.DOT);
		visualisationCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				generateFileList.add(modelName + ".svg");
			} else {
				generateFileList.remove(modelName + ".svg");
			}
		});

		progressBox.visibleProperty().bind(updater.runningProperty());
		progressBox.managedProperty().bind(progressBox.visibleProperty());
		progressBar.visibleProperty().bind(updater.runningProperty());
		progressBar.managedProperty().bind(progressBar.visibleProperty());

		this.getScene().addPostLayoutPulseListener(this::sizeToScene);
		setOnCloseRequest(e -> this.cancel());
	}

	@FXML
	public void selectRailMLFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("railml.stage.filechooser.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getRailMLFilter());
		initializeForPath(fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.RAILML, stageManager.getCurrent()));
	}

	public void initializeForPath(Path path) {
		if (path != null) {
			generateFileList.clear();
			animationMachineCheckbox.setSelected(false);
			validationMachineCheckbox.setSelected(false);
			visualisationCheckbox.setSelected(false);
			Path outputPath = path.getParent().toAbsolutePath();
			modelName = MoreFiles.getNameWithoutExtension(path);
			importArguments = new ImportArgumentsBuilder(path).output(outputPath).modelName(modelName);
			fileLocationField.setText(path.toAbsolutePath().toString());
			locationField.setText(outputPath.toString());
		}
	}

	@FXML
	public void selectRailMLDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(i18n.translate("railml.stage.directorychooser.title"));
		Path path = fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.RAILML, stageManager.getCurrent());
		if(path != null) {
			importArguments.output(path.toAbsolutePath());
		}
	}

	@FXML
	public void startImport() {
		ImportArguments args = importArguments.generateAnimationMachine(animationMachineCheckbox.isSelected())
				.generateValidationMachine(validationMachineCheckbox.isSelected())
				.generateDataMachine(animationMachineCheckbox.isSelected() || validationMachineCheckbox.isSelected())
				.generateVisualisation(visualisationCheckbox.isSelected())
				.visualisationStrategy(visualisationCheckbox.isSelected() ? visualisationStrategyChoiceBox.getValue() : null)
				.build();
		clearProgressWithMessage("Initialize import");

		updater.execute(() -> {
			try {
				railML2B = injector.getInstance(RailML2BFactory.class).createWithArguments(args);
				listener = new UIProgressListener(progressBar, progressOperation, progressLabel, progressDescription,
					railML2B.getMachineLoader().getNumberOfOperations());
				railML2B.loadAndValidate(listener);

				if (!Thread.currentThread().isInterrupted()) {
					Platform.runLater(() -> {
						RailMLInspectDotStage railMLInspectDotStage = injector.getInstance(RailMLInspectDotStage.class);
						if (args.generateVisualisation()) {
							this.close();
							railMLInspectDotStage.initializeForArguments(args, railML2B.getMachineLoader().getCurrentTrace().getCurrentState());
							railMLInspectDotStage.show();
							railMLInspectDotStage.toFront();
							try {
								railMLInspectDotStage.visualizeCustomGraph();
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							if (args.generateAnimationMachine() || args.generateValidationMachine()) {
								railMLInspectDotStage.setOnHidden(event -> createMachinesAndProject());
							}
						} else {
							createMachinesAndProject();
							this.close();
						}
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

	private void createMachinesAndProject() {
		boolean replacingProject = currentProject.confirmReplacingProject();
		if (replacingProject) {
			try {
				railML2B.generateMachines(listener);

				ImportArguments args = importArguments.build();
				String modelName = args.modelName();
				Path outputPath = args.output();
				currentProject.switchTo(new Project(modelName, "", Collections.emptyList(), Collections.emptyList(),
					Collections.emptyList(), Project.metadataBuilder().build(), outputPath), true);

				Path fileName = args.file().getFileName();
				if (args.generateDataMachine()) {
					currentProject.addMachine(new Machine(modelName + DATA_MCH,
						"Data machine generated from " + fileName, outputPath.relativize(outputPath.resolve(modelName + DATA_MCH))));
				}
				if (args.generateValidationMachine()) {
					currentProject.addMachine(new Machine(modelName + VALIDATION_MCH,
						"Validation machine generated from " + args.file().getFileName(),
						args.output().relativize(args.output().resolve(modelName + VALIDATION_MCH))));
				}
				if (args.generateAnimationMachine()) {
					final Machine animationMachine = new Machine(modelName + ANIMATION_MCH,
						"Animation machine generated from " + fileName, outputPath.relativize(outputPath.resolve(modelName + ANIMATION_MCH)));
					animationMachine.getMachineProperties().simulationsProperty()
						.add(new SimulationModel(outputPath.relativize(outputPath.resolve("RailML3_SimB.json")), Collections.emptyList()));
					currentProject.addMachine(animationMachine);
				}

				List<Machine> createdMachines = currentProject.getMachines();
				if (!createdMachines.isEmpty()) {
					currentProject.startAnimation(createdMachines.getLast());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@FXML
	public void cancel() {
		updater.cancel(true);
		if (railML2B != null) railML2B.finish();
		this.close();
	}
}
