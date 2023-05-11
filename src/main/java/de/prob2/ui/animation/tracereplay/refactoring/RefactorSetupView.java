package de.prob2.ui.animation.tracereplay.refactoring;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.check.exploration.ReplayOptions;
import de.prob.check.tracereplay.check.refinement.AbstractTraceRefinement;
import de.prob.check.tracereplay.check.refinement.TraceConnector;
import de.prob.check.tracereplay.check.refinement.TraceRefinerEventB;
import de.prob.check.tracereplay.check.refinement.VerticalTraceRefiner;
import de.prob.check.tracereplay.check.traceConstruction.AdvancedTraceConstructor;
import de.prob.check.tracereplay.check.traceConstruction.TraceConstructionError;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.model.eventb.EventBModel;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.EventBFactory;
import de.prob.scripting.EventBPackageFactory;
import de.prob.scripting.FactoryProvider;
import de.prob.scripting.ModelFactory;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.visualisation.traceDifference.TracePlotter;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class RefactorSetupView extends Dialog<RefactorSetup> {

	private static final List<String> CLASSICAL_EXTENSIONS = Collections.unmodifiableList(Arrays.asList(EventBFactory.RODIN_MACHINE_EXTENSION, EventBPackageFactory.EXTENSION, ClassicalBFactory.CLASSICAL_B_MACHINE_EXTENSION, ClassicalBFactory.CLASSICAL_B_REFINEMENT_EXTENSION));

	@FXML
	ComboBox<RefactorSetup.WhatToDo> options;

	@FXML
	Button firstMachine;

	@FXML
	Button secondMachine;

	@FXML
	Button traceFile;

	@FXML
	CheckBox checkBox;

	@FXML
	Label label;

	@FXML
	Spinner<Integer> maxDepth;

	@FXML
	Spinner<Integer> maxBreadth;



	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final TraceManager traceManager;
	private final TraceFileHandler traceFileHandler;
	private final Injector injector;

	private final SimpleObjectProperty<Path> alpha = new SimpleObjectProperty<>();
	private final SimpleObjectProperty<Path> beta = new SimpleObjectProperty<>();
	private final SimpleObjectProperty<Path> trace = new SimpleObjectProperty<>();
	private final SimpleObjectProperty<RefactorSetup.WhatToDo> whatToDo = new SimpleObjectProperty<>();

	@Inject
	RefactorSetupView(
		StageManager stageManager,
		CurrentProject currentProject,
		I18n i18n,
		FileChooserManager fileChooserManager,
		TraceManager traceManager,
		TraceFileHandler traceFileHandler,
		Injector injector
	) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.traceManager = traceManager;
		this.traceFileHandler = traceFileHandler;
		this.injector = injector;
		stageManager.loadFXML(this, "refactor_setup_view.fxml");
	}

	@FXML
	private void initialize() {
		ButtonType buttonTypeA = new ButtonType(i18n.translate("traceModification.traceRefactorSetup.button.abort"), ButtonBar.ButtonData.CANCEL_CLOSE);
		ButtonType buttonTypeS = new ButtonType(i18n.translate("traceModification.traceRefactorSetup.button.start"), ButtonBar.ButtonData.APPLY);
		this.getDialogPane().getButtonTypes().addAll(buttonTypeS, buttonTypeA);

		beta.set(currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation()));

		whatToDo.bind(options.getSelectionModel().selectedItemProperty());

		this.getDialogPane().lookupButton(buttonTypeS).disableProperty().setValue(true);

		firstMachine.disableProperty().bind(options.getSelectionModel().selectedItemProperty().isNull());
		secondMachine.disableProperty().bind(options.getSelectionModel().selectedItemProperty().isNotEqualTo(RefactorSetup.WhatToDo.REFINEMENT_REPLAY));
		traceFile.disableProperty().bind(options.getSelectionModel().selectedItemProperty().isNull());
		options.setItems(FXCollections.observableArrayList(RefactorSetup.WhatToDo.validValues()));
		options.setConverter(i18n.translateConverter());
		options.setPromptText(i18n.translate("traceModification.traceRefactorSetup.whatToDo.prompt"));
		options.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null) {
				whatToDo.set(RefactorSetup.WhatToDo.NOTHING);
			} else {
				switch (newValue) {
					case REFINEMENT_REPLAY:
						firstMachine.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.file1.refinement"));
						secondMachine.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.file2.refinement"));
						traceFile.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.trace.refinement"));
						break;
					case OPTION_REPLAY:
						firstMachine.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.file1.replay"));
						secondMachine.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.file2.replay"));
						traceFile.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.trace.replay"));
						break;
				}
			}
		});

		addPathSelectionAction(firstMachine, alpha, "common.fileChooser.fileTypes.classicalB", CLASSICAL_EXTENSIONS);
		addPathSelectionAction(secondMachine, beta, "common.fileChooser.fileTypes.classicalB", CLASSICAL_EXTENSIONS);
		addPathSelectionAction(traceFile, trace, "common.fileChooser.fileTypes.proB2Trace", Collections.singletonList(TraceFileHandler.TRACE_FILE_EXTENSION));

		this.setResultConverter(param -> {
					if (param.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
						return new RefactorSetup(RefactorSetup.WhatToDo.NOTHING, alpha.get(), beta.get(), trace.get(), checkBox.isSelected(), maxDepth.getValue(), maxBreadth.getValue());

					} else {
						return new RefactorSetup(whatToDo.get(), alpha.get(), beta.get(), trace.get(), checkBox.isSelected(), maxDepth.getValue(), maxBreadth.getValue());
					}
				}
		);

		ChangeListener<Path> reactionPath = (observable, oldValue, newValue) -> {
			if (allowOK()) {
				this.getDialogPane().lookupButton(buttonTypeS).disableProperty().setValue(false);
			}
		};

		alpha.addListener(reactionPath);
		beta.addListener(reactionPath);
		trace.addListener(reactionPath);
		whatToDo.addListener((observable, oldValue, newValue) -> {
			if (allowOK()) {
				this.getDialogPane().lookupButton(buttonTypeS).disableProperty().setValue(false);
			}
		});


		ChangeListener<Path> validTarget = (observable, oldValue, newValue) -> {
			if(whatToDo.get() == RefactorSetup.WhatToDo.OPTION_REPLAY){
				if(!currentProject.getMachines().stream().map(Machine::getName).collect(Collectors.toList()).contains(alpha.getName())){
					label.setText(i18n.translate("traceModification.traceRefactorSetup.checkBox.setResult.Warning"));
					//label.setText(i18n.translate("traceModification.traceRefactorSetup.checkBox.setResult"));
					label.setTextFill(Color.color(1, 0, 0));
				}else{
					label.setText(i18n.translate("traceModification.traceRefactorSetup.checkBox.setResult"));
					label.setTextFill(Color.color(0, 0, 0));
				}
			}else{
				if(whatToDo.get() == RefactorSetup.WhatToDo.REFINEMENT_REPLAY){
					if(!beta.getName().equals("") && !currentProject.getMachines().stream().map(Machine::getName).collect(Collectors.toList()).contains(beta.getName())){
						label.setText(i18n.translate("traceModification.traceRefactorSetup.checkBox.setResult.Warning"));
						label.setTextFill(Color.color(1, 0, 0));
					}
				}else{
					label.setText(i18n.translate("traceModification.traceRefactorSetup.checkBox.setResult"));
					label.setTextFill(Color.color(0, 0, 0));
				}
			}
		};

		alpha.addListener(validTarget);
		beta.addListener(validTarget);

		maxBreadth.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2,1000));
		maxBreadth.setEditable(true);
		maxDepth.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,1000));
		maxDepth.setEditable(true);

	}

	private void addPathSelectionAction(Button button, Property<Path> pathProperty, String extensionKey, List<String> extensions) {
		button.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(i18n.translate("traceModification.traceRefactorSetup.traceFileChooser.title", button.getText()));
			fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
			fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter(extensionKey, extensions));
			pathProperty.setValue(fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent()));
			if (pathProperty.getValue() != null) {
				button.textProperty().setValue(button.getText() + ": " + pathProperty.getValue().getFileName());
			}
		});
	}

	private boolean allowOK() {
		if (whatToDo.get() != null && whatToDo.get() != RefactorSetup.WhatToDo.NOTHING && trace.get() != null) {
			if (alpha.get() != null && beta.get() != null) {
				return true;
			}
			return beta.get() == null && whatToDo.get() == RefactorSetup.WhatToDo.OPTION_REPLAY; //Conditional replay can work with one trace

		}

		return false;
	}

	private List<String> skipTransitions(RefactorSetup result, AbstractTraceRefinement abstractTraceRefinement) throws IOException {
		if(abstractTraceRefinement instanceof TraceRefinerEventB){
			// TODO Use already loaded model
			EventBModel eventBModel = injector.getInstance(EventBFactory.class).extract(result.getFileBeta().toString()).getModel();
			return eventBModel.introducedBySkip();
		}else{
			return Collections.emptyList();
		}
	}

	private void createFeedNackMessage(String messageText){
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION, messageText, ButtonType.OK);
		alert.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> ((Label)node).setMinHeight(Region.USE_PREF_SIZE));
		alert.showAndWait();
	}


	private void saveAdaptedTrace(List<PersistentTransition> resultTrace, TraceJsonFile fileObject, RefactorSetup userInput) throws IOException {
		TraceJsonFile newFileObject = fileObject.changeTransitionList(resultTrace);
		Path path = userInput.getTraceFile().getParent().resolve(userInput.traceFile.getFileName().toString().replaceFirst("[.][^.]+$", "") + "_adapted_with_options.prob2trace" );
		traceManager.save(path, newFileObject);

		if(userInput.setResult){
			traceFileHandler.addTraceFile(currentProject.getCurrentMachine(), path);
		}
	}
	private void saveRefinedTrace(List<PersistentTransition> resultTrace, TraceJsonFile fileObject, RefactorSetup userInput, String adaptedFor) throws IOException {

		//TraceJsonFile newFileObject = new TraceJsonFile(resultTrace, , TraceJsonFile.metadataBuilder());
		TraceJsonFile newFileObject = fileObject.changeTransitionList(resultTrace).changeModelName(adaptedFor); //TODO this can cause error as all the meta data are not updated
		Path path = userInput.getTraceFile().getParent().resolve(userInput.traceFile.getFileName().toString().replaceFirst("[.][^.]+$", "")  + "___refined_from___"+adaptedFor+".prob2trace" );
		traceManager.save(path, newFileObject);

		if(userInput.setResult){
			traceFileHandler.addTraceFile(currentProject.getCurrentMachine(), path);
		}
	}

	/*
	 * TODO: Move this to ProB2 UI rendering
	 */
	private void caterForGraphic(TraceJsonFile fileObject, List<PersistentTransition> resultingTrace, List<String> skips){
		TracePlotter.ResizableCanvas canvas = new TracePlotter.ResizableCanvas();
		canvas.resize(500, 300);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		TraceConnector traceConnector = new TraceConnector(fileObject.getTransitionList(), resultingTrace, skips);
		TracePlotter tracePlotter = new TracePlotter();
		tracePlotter.drawTraces(traceConnector.connect(), gc);

		MenuBar menuBar = new MenuBar();
		Menu menu = new Menu("File");
		menuBar.getMenus().add(menu);
		MenuItem menuItem = new MenuItem("Save as");
		menu.getItems().add(menuItem);

		ScrollPane secondaryLayout = new ScrollPane(gc.getCanvas());
		VBox vBox = new VBox(menuBar, secondaryLayout);
		Scene secondScene = new Scene(vBox, 500, 300);

		Stage newWindow = new Stage();
		newWindow.setTitle("Result as Image");
		newWindow.setScene(secondScene);

		menuItem.setOnAction(e -> {
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG","*.png"));
			fc.setTitle("Save Map");
			File file = fc.showSaveDialog(newWindow);
			if(file != null) {
				WritableImage wi = new WritableImage((int) gc.getCanvas().getWidth(), (int) gc.getCanvas().getHeight());
				try {

					ImageIO.write(SwingFXUtils.fromFXImage(canvas.snapshot(null,wi),null),"png",file);
				} catch (IOException other) {
					other.printStackTrace();
				}
			}
		});
		newWindow.showAndWait();
	}

	private AbstractTraceRefinement checkCorrectFileType(TraceJsonFile fileObject, RefactorSetup refactorButton) {

		if (refactorButton.getFileAlpha().toString().endsWith(ClassicalBFactory.CLASSICAL_B_MACHINE_EXTENSION) && refactorButton.getFileBeta().toString().endsWith(ClassicalBFactory.CLASSICAL_B_MACHINE_EXTENSION)) {
			return new VerticalTraceRefiner(injector, fileObject.getTransitionList(), refactorButton.getFileAlpha(), refactorButton.getFileBeta());
		}

		if (refactorButton.getFileBeta().toString().endsWith(EventBFactory.RODIN_MACHINE_EXTENSION)) {
			try {
				return new TraceRefinerEventB(injector, fileObject.getTransitionList(), refactorButton.getFileBeta(), refactorButton.getMaxBreadth(), refactorButton.getMaxDepth());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public void showAndPerformAction() {
		RefactorSetup result = this.showAndWait().get();
		if (result.whatToDo == RefactorSetup.WhatToDo.NOTHING) {
			return;
		}

		TraceJsonFile fileObject;
		try {
			fileObject = traceManager.load(result.getTraceFile());
		} catch (IOException e) {
			traceFileHandler.showLoadError(result.getTraceFile(), e);
			return;
		}

		switch (result.whatToDo){
			case REFINEMENT_REPLAY:

				AbstractTraceRefinement abstractTraceRefinement = checkCorrectFileType(fileObject, result);

				if(abstractTraceRefinement != null){
					try {
						List<PersistentTransition> resultingTrace = abstractTraceRefinement.refineTraceExtendedFeedback().getResultTracePersistentTransition();
						Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Refinement Successful", ButtonType.OK, new ButtonType("Show"));
						Optional<ButtonType> buttonPressed = alert.showAndWait();
						if(buttonPressed.isPresent() && buttonPressed.get().getButtonData().equals(ButtonBar.ButtonData.OTHER)){
							caterForGraphic(fileObject, resultingTrace, skipTransitions(result, abstractTraceRefinement));
						}
						saveRefinedTrace(resultingTrace, fileObject, result, result.getFileBeta().getFileName().toString().replaceFirst("[.][^.]+$", ""));
					} catch (IOException | BCompoundException e) {
						e.printStackTrace();
					} catch (TraceConstructionError e) {
						createFeedNackMessage("Trace could be refined while enforcing all predicates");
					}
				}
				break;
			case OPTION_REPLAY:
				ReplayOptionsOverview traceOptionChoice = new ReplayOptionsOverview(fileObject.getVariableNames(), fileObject.getMachineOperationInfos(), stageManager);
				Optional<ReplayOptions> optionResult = traceOptionChoice.showAndWait();
				ReplayOptions replayOptions = optionResult.get();
				try {
					// TODO Use shared animator instead of starting a new one
					ModelFactory<?> factory = injector.getInstance(FactoryProvider.factoryClassFromExtension(MoreFiles.getFileExtension(result.fileAlpha)));
					List<Transition> resultTrace = AdvancedTraceConstructor.constructTraceWithOptions(fileObject.getTransitionList(), factory.extract(result.fileAlpha.toString()).load(), replayOptions);
					createFeedNackMessage("Trace could be fully replayed while enforcing all predicates");
					saveAdaptedTrace(PersistentTransition.createFromList(resultTrace), fileObject, result);
				} catch ( IOException e) {
					e.printStackTrace();
				} catch (TraceConstructionError e){
					createFeedNackMessage( "Trace could not fully replayed with all all predicates enforced");
				}
				break;
		}
	}
}
