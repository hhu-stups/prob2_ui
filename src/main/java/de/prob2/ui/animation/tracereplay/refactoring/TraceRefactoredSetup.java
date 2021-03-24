package de.prob2.ui.animation.tracereplay.refactoring;

import com.google.common.io.Files;
import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.check.TraceChecker;
import de.prob.check.tracereplay.check.TraceCheckerUtils;
import de.prob.check.tracereplay.check.TraceModifier;
import de.prob.check.tracereplay.check.exploration.ReplayOptions;
import de.prob.check.tracereplay.check.renamig.DeltaCalculationException;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.StateSpace;
import de.prob2.ui.animation.tracereplay.MappingFactory;
import de.prob2.ui.animation.tracereplay.ProgressMemory;
import de.prob2.ui.animation.tracereplay.ReplayOptionsOverview;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class TraceRefactoredSetup {
	TraceChecker traceChecker;
	private final TraceJsonFile traceJsonFile;
	private final Path machineA;
	private final Path machineB;
	private final Path traceJsonFilePath;
	private final PersistentTrace persistentTrace;
	private final Injector injector;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle resourceBundle;
	private final StateSpace stateSpace;
	private final Map<String, OperationInfo> currentMachineOperations;
	private Stage progressStage;
	private Thread traceCheckerProcess;


	public TraceRefactoredSetup(TraceJsonFile traceJsonFile, Path machineA, Path machineB, Path tracePath, StateSpace stateSpace,
									Injector injector, CurrentProject currentProject, StageManager stageManager)  {
		this.traceJsonFile = traceJsonFile;
		this.machineA = machineA;
		this.machineB = machineB;
		this.traceJsonFilePath = tracePath;
		this.persistentTrace = new PersistentTrace(traceJsonFile.getDescription(), traceJsonFile.getTransitionList());
		this.injector = injector;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.resourceBundle = injector.getInstance(ResourceBundle.class);
		this.currentMachineOperations = stateSpace.getLoadedMachine().getOperations();
		this.stateSpace = stateSpace;
	}

	public TraceRefactoredSetup(TraceJsonFile traceJsonFile, Path machineA, Path machineB, Path tracePath,
								Injector injector, CurrentProject currentProject, StageManager stageManager) throws IOException, ModelTranslationError {
		this.traceJsonFile = traceJsonFile;
		this.machineA = machineA;
		this.machineB = machineB;
		this.traceJsonFilePath = tracePath;
		this.persistentTrace = new PersistentTrace(traceJsonFile.getDescription(), traceJsonFile.getTransitionList());
		this.injector = injector;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.resourceBundle = injector.getInstance(ResourceBundle.class);
		this.currentMachineOperations = TraceCheckerUtils.createStateSpace(machineA.toString(), injector).getLoadedMachine().getOperations();
		this.stateSpace = null;
	}


	private TraceChecker createChecker(ReplayOptions replayOptions, ProgressMemory progressMemory) throws IOException, ModelTranslationError, DeltaCalculationException {
		StateSpace localStateSpace = TraceCheckerUtils.createStateSpace(machineA.toString(), injector);


		return new TraceChecker(persistentTrace.getTransitionList(),
				new HashMap<>(traceJsonFile.getMachineOperationInfos()),
				new HashMap<>(localStateSpace.getLoadedMachine().getOperations()),
				new HashSet<>(traceJsonFile.getVariableNames()),
				new HashSet<>(localStateSpace.getLoadedMachine().getVariableNames()),
				machineA.toString(),
				machineB.toString(),
				injector,
				new MappingFactory(injector, stageManager),
				replayOptions,
				progressMemory);
	}

	private TraceChecker createCheckerForLocalMachine(StateSpace stateSpace, ReplayOptions replayOptions, ProgressMemory progressMemory) throws IOException, ModelTranslationError, DeltaCalculationException, DeltaCalculationException {
		return new TraceChecker(persistentTrace.getTransitionList(),
				new HashMap<>(traceJsonFile.getMachineOperationInfos()),
				new HashMap<>(stateSpace.getLoadedMachine().getOperations()),
				new HashSet<>(stateSpace.getLoadedMachine().getVariableNames()),
				new HashSet<>(traceJsonFile.getVariableNames()),
				machineA.toString(),
				injector,
				new MappingFactory(injector, stageManager),
				replayOptions,
				progressMemory);
	}


	private TraceChecker createComplexCheckerForLocalMachine(StateSpace stateSpace, ReplayOptions replayOptions, ProgressMemory progressMemory) throws IOException, ModelTranslationError, DeltaCalculationException {
		return new TraceChecker(persistentTrace.getTransitionList(),
				new HashMap<>(traceJsonFile.getMachineOperationInfos()),
				new HashMap<>(stateSpace.getLoadedMachine().getOperations()),
				new HashSet<>(traceJsonFile.getVariableNames()),
				new HashSet<>(stateSpace.getLoadedMachine().getVariableNames()),
				machineB.toString(),
				machineA.toString(),
				injector,
				new MappingFactory(injector, stageManager),
				replayOptions,
				progressMemory);
	}




	public void executeCheck(boolean withLocalStateSpace){

		ReplayOptionsOverview traceOptionChoice = new ReplayOptionsOverview(traceJsonFile.getVariableNames(), traceJsonFile.getMachineOperationInfos(), stageManager);
		Optional<ReplayOptions> optionResult = traceOptionChoice.showAndWait();
		ReplayOptions replayOptions = optionResult.get();

		progressStage = new Stage();
		ProgressMemory progressMemory = ProgressMemory.setupForTraceChecker(resourceBundle, stageManager, progressStage);
		stageManager.register(progressStage, null);
		progressStage.initOwner(stageManager.getMainStage().getOwner());
		progressStage.initModality(Modality.WINDOW_MODAL);
		progressStage.setOnCloseRequest(event -> {
			traceCheckerProcess.interrupt();
		});


		Runnable traceCheckerRunnable = () -> {

			try {
				if(withLocalStateSpace){
					if(machineB != null){
						traceChecker = createComplexCheckerForLocalMachine(stateSpace, replayOptions, progressMemory);
					}else{
						traceChecker = createCheckerForLocalMachine(stateSpace, replayOptions, progressMemory);
					}
				}else{
					traceChecker = createChecker(replayOptions, progressMemory);
				}

				Platform.runLater(progressStage::close);
			} catch (Exception e) {
				throw new TraceModificationError(e);
			}
		};

		traceCheckerProcess = new Thread(traceCheckerRunnable);
		traceCheckerProcess.start();
		progressStage.showAndWait();
	}


	/**
	 * Presents the user with the evaluation results and demands some input of him
	 * @return the selected traces to be stored
	 */
	public List<Path> evaluateResults(){

		try {
			traceCheckerProcess.join();
			progressStage.close();

			if(traceChecker == null){
				throw new NullPointerException();
			}

			final List<Path> result = new ArrayList<>();

			TraceModifier traceModifier = traceChecker.getTraceModifier();
			if(traceModifier.tracingFoundResult() && traceModifier.isDirty()){
				TraceRefactorResults dialog = new TraceRefactorResults(injector, stageManager, this, persistentTrace);
				stageManager.register(dialog);
				List<PersistentTrace> dialogResult = new ArrayList<>(dialog.showAndWait().get());//The dialog can only return 0,1 or 2 results
				if(dialogResult.remove(persistentTrace)){
					result.add(traceJsonFilePath);
				}
				if(!dialogResult.isEmpty()){
					result.add(saveNewTrace(dialogResult.get(0)));
				}
			}else if(traceModifier.tracingFoundResult() && !traceModifier.isDirty()){
				result.add(traceJsonFilePath);
			}
			else if(!traceModifier.tracingFoundResult() && traceModifier.thereAreIncompleteTraces() && traceModifier.ungracefulTraceWithMinLength(3).isEmpty()) {
				result.addAll(traceReplayedUngracefully(traceModifier.ungracefulTraceWithMinLength(3)));
			} else{
				result.addAll(traceNotReplayable());
			}

			return result;

		} catch (Exception e) {
			return traceNotReplayable();
		}
	}


	private List<Path> saveTraces(List<PersistentTrace> persistentTraces) throws IOException {

		List<Path> results = new ArrayList<>();

		for(PersistentTrace persistentTrace : persistentTraces) {
			String newMachineName = currentProject.getCurrentMachine().getName();
			results.add(save("_not_fully_replayable_for_", persistentTrace));

		}
		return results;
	}

	private Path saveNewTrace(PersistentTrace persistentTrace) throws IOException {
		return save( "_edited_for_", persistentTrace);
	}

	private Path save(String filenameModification, PersistentTrace persistentTrace) throws IOException {
		String newMachineName = currentProject.getCurrentMachine().getName();

		String filename = Files.getNameWithoutExtension(traceJsonFilePath.toString());

		String modified = filename +  filenameModification + newMachineName+"." + Files.getFileExtension(traceJsonFilePath.toString());

		Path saveAt = currentProject.getLocation().resolve(Paths.get(modified));

		TraceFileHandler traceManager = injector.getInstance(TraceFileHandler.class);
		//Todo, what about Vars, Const, Sets?
		traceManager.save(traceJsonFile.changeTrace(persistentTrace).changeModelName(newMachineName).changeMachineInfos(currentMachineOperations), saveAt);

		return saveAt;
	}



	private List<Path> traceReplayedUngracefully(List<List<PersistentTransition>> persistentTraces) throws IOException {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION, resourceBundle.getString("traceModification.alert.traceReplayedUngracefully") , ButtonType.YES, ButtonType.NO);
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.setTitle("Trace refactoring could not complete");

		Optional<ButtonType> dialogResult = alert.showAndWait();
		if (dialogResult.get() == ButtonType.YES) {
			List<PersistentTrace> toSave = persistentTraces.stream().map(persistentTrace::setTrace).collect(Collectors.toList());
			return saveTraces(toSave);
		}else{
			return emptyList();
		}
	}

	private List<Path> traceNotReplayable(){
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION, resourceBundle.getString("traceModification.alert.traceNotReplayable") , ButtonType.YES, ButtonType.NO);
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.setTitle("No suitable configuration found.");

		Optional<ButtonType> dialogResult = alert.showAndWait();
		if (dialogResult.get() == ButtonType.YES) {
			return singletonList(traceJsonFilePath);
		}else{
			return emptyList();
		}
	}

	public static void traceNotReplayableConfirmation(ResourceBundle bundle){
		Alert alert = new Alert(Alert.AlertType.INFORMATION, bundle.getString("traceModification.alert.traceNotReplayable"));
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.setTitle("No suitable configuration found.");
		alert.showAndWait();
	}


	class TraceModificationError extends RuntimeException{

		/**
		 * Constructs an instance of this class.
		 *
		 * @param message the detail message, can be null
		 * @param cause   the {@code IOException}
		 * @throws NullPointerException if the cause is {@code null}
		 */
		public TraceModificationError(String message, Exception cause) {
			super(message, cause);
		}

		/**
		 * Constructs an instance of this class.
		 *
		 * @param cause the {@code IOException}
		 * @throws NullPointerException if the cause is {@code null}
		 */
		public TraceModificationError(Exception cause) {
			super(cause);
		}
	}
}