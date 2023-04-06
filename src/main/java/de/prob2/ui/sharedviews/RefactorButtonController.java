package de.prob2.ui.sharedviews;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.ReusableAnimator;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.check.TraceCheckerUtils;
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
import de.prob.statespace.StateSpace;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.animation.tracereplay.refactoring.RefactorSetup;
import de.prob2.ui.animation.tracereplay.refactoring.RefactorSetupView;
import de.prob2.ui.animation.tracereplay.refactoring.ReplayOptionsOverview;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.visualisation.traceDifference.TracePlotter;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public final class RefactorButtonController {

	private final CurrentProject currentProject;
	private final MachineLoader machineLoader;
	private final RealTimeSimulator realTimeSimulator;
	private final StageManager stageManager;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final TraceManager traceManager;
	private final Injector injector;
	private final TraceFileHandler traceFileHandler;

	@FXML
	private Button button;

	@Inject
	private RefactorButtonController(final StageManager stageManager, final CurrentProject currentProject, final MachineLoader machineLoader, final RealTimeSimulator realTimeSimulator, FileChooserManager fileChooserManager, I18n i18n, TraceFileHandler traceFileHandler, TraceManager traceManager, Injector injector) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;
		this.realTimeSimulator = realTimeSimulator;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.traceManager = traceManager;
		this.injector = injector;
		this.traceFileHandler = traceFileHandler;
	}

	@FXML
	private void initialize() {
		button.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(machineLoader.loadingProperty()).or(realTimeSimulator.runningProperty()));

		button.setOnAction(event -> {
			RefactorSetup result = new RefactorSetupView(stageManager, currentProject, i18n, fileChooserManager).showAndWait().get();
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
						List<Transition> resultTrace = AdvancedTraceConstructor.constructTraceWithOptions(fileObject.getTransitionList(), TraceCheckerUtils.createStateSpace(result.fileAlpha.toString(), injector), replayOptions);
						createFeedNackMessage("Trace could be fully replayed while enforcing all predicates");
						saveAdaptedTrace(PersistentTransition.createFromList(resultTrace), fileObject, result);
					} catch ( IOException e) {
						e.printStackTrace();
					} catch (TraceConstructionError e){
						createFeedNackMessage( "Trace could not fully replayed with all all predicates enforced");
					}
					break;
			}

		});
	}

	private List<String> skipTransitions(RefactorSetup result, AbstractTraceRefinement abstractTraceRefinement) throws IOException {
		ReusableAnimator animator = injector.getInstance(ReusableAnimator.class);
		StateSpace stateSpace = animator.createStateSpace();
		if(abstractTraceRefinement instanceof TraceRefinerEventB){
			EventBFactory eventBFactory = injector.getInstance(EventBFactory.class);
			eventBFactory.extract(result.getFileBeta().toString()).loadIntoStateSpace(stateSpace);
			EventBModel eventBModel = (EventBModel) stateSpace.getModel();
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

}