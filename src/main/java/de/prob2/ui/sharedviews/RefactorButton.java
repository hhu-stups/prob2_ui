package de.prob2.ui.sharedviews;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.check.TraceCheckerUtils;
import de.prob.check.tracereplay.check.exploration.ReplayOptions;
import de.prob.check.tracereplay.check.refinement.VerticalTraceRefiner;
import de.prob.check.tracereplay.check.traceConstruction.AdvancedTraceConstructor;
import de.prob.check.tracereplay.check.traceConstruction.TraceConstructionError;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.*;
import de.prob2.ui.animation.tracereplay.refactoring.RefactorSetup;
import de.prob2.ui.animation.tracereplay.refactoring.RefactorSetupView;
import de.prob2.ui.animation.tracereplay.refactoring.ReplayOptionsOverview;
import de.prob2.ui.animation.tracereplay.refactoring.TraceRefactoredSetup;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


@FXMLInjected
public class RefactorButton extends Button {

	private final CurrentProject currentProject;
	private final MachineLoader machineLoader;
	private final RealTimeSimulator realTimeSimulator;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final FileChooserManager fileChooserManager;
	private final Injector injector;
	private final TraceFileHandler traceFileHandler;

	@Inject
	private RefactorButton(final StageManager stageManager, final CurrentProject currentProject, final MachineLoader machineLoader, final RealTimeSimulator realTimeSimulator, FileChooserManager fileChooserManager, ResourceBundle resourceBundle, TraceFileHandler traceFileHandler, Injector injector) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;
		this.realTimeSimulator = realTimeSimulator;
		this.bundle = resourceBundle;
		this.fileChooserManager = fileChooserManager;
		this.injector = injector;
		this.traceFileHandler = traceFileHandler;

		stageManager.loadFXML(this, "refactor_button.fxml");
	}

	@FXML
	private void initialize() {
		this.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(machineLoader.loadingProperty()).or(realTimeSimulator.runningProperty()));

		this.setOnAction(event -> {
			RefactorSetup result = new RefactorSetupView(stageManager, currentProject, bundle, fileChooserManager).showAndWait().get();

			switch (result.whatToDo){
				case REFACTOR_TRACE:
					TraceJsonFile traceJsonFile = traceFileHandler.loadFile(result.getTraceFile());
					TraceRefactoredSetup traceRefactoredSetup = null;
					try {
						traceRefactoredSetup = new TraceRefactoredSetup(traceJsonFile,  result.fileAlpha, result.fileBeta, result.getTraceFile(), injector, currentProject, stageManager);
					} catch (IOException e) {
						e.printStackTrace();
					}
					traceRefactoredSetup.executeCheck(currentProject.getCurrentMachine().getLocation()==result.getFileAlpha());
					List<Path> traceFiles = traceRefactoredSetup.evaluateResults();

					if(result.setResult){
						traceFiles.forEach(entry -> currentProject.getCurrentMachine().addTraceFile(entry));
					}
					break;
				case REFINEMENT_REPLAY:
					TraceJsonFile json= traceFileHandler.loadFile(result.getTraceFile());
					VerticalTraceRefiner refinementChecker = new VerticalTraceRefiner(injector, json.getTransitionList(), result.getFileAlpha(), result.fileBeta);
					try {
						List<PersistentTransition> resultingTrace = refinementChecker.refineTrace();
						boolean wasSuccessful = resultingTrace.size() == json.getTransitionList().size();
						String messageText;
						if(wasSuccessful){
							messageText = "Trace could be fully replayed while enforcing all predicates";
						}else{
							messageText = "Only " + resultingTrace.size() + "/" + json.getTransitionList().size() +" could be replayed while enforcing all predicates";
						}

						Alert alert = new Alert(Alert.AlertType.CONFIRMATION, messageText, ButtonType.OK);
						alert.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> ((Label)node).setMinHeight(Region.USE_PREF_SIZE));

						alert.showAndWait();
					} catch (IOException | TraceConstructionError | BCompoundException e) {
						e.printStackTrace();
					}
					break;
				case OPTION_REPLAY:
					TraceJsonFile json2= traceFileHandler.loadFile(result.getTraceFile());
					ReplayOptionsOverview traceOptionChoice = new ReplayOptionsOverview(json2.getVariableNames(), json2.getMachineOperationInfos(), stageManager);
					Optional<ReplayOptions> optionResult = traceOptionChoice.showAndWait();
					ReplayOptions replayOptions = optionResult.get();
					try {
						List<Transition> resultTrace = AdvancedTraceConstructor.constructTraceWithOptions(json2.getTransitionList(), TraceCheckerUtils.createStateSpace(result.fileAlpha.toString(), injector), replayOptions);
						boolean wasSuccessful = resultTrace.size() == json2.getTransitionList().size();
						String messageText;
						if(wasSuccessful){
							messageText = "Trace could be fully replayed while enforcing all predicates";
						}else{
							messageText = "Only " + resultTrace.size() + "/" + json2.getTransitionList().size() +" could be replayed while enforcing all predicates";
						}
						Alert alert = new Alert(Alert.AlertType.CONFIRMATION, messageText, ButtonType.OK);
						alert.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> ((Label)node).setMinHeight(Region.USE_PREF_SIZE));

						alert.showAndWait();
						TraceJsonFile json3 = json2.changeTransitionList(PersistentTransition.createFromList(resultTrace));
						Path path = result.getTraceFile().getParent().resolve(result.traceFile.getFileName() + "_adapted_with_options.prob2trace" );
						traceFileHandler.save(json3, path);

						if(result.setResult){
							currentProject.getCurrentMachine().addTraceFile(path);
						}

					} catch (TraceConstructionError | IOException traceConstructionError) {
						traceConstructionError.printStackTrace();
					}


					break;
			}

		});
	}

}
