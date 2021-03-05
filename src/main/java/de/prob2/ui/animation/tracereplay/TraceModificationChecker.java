package de.prob2.ui.animation.tracereplay;

import com.google.common.io.Files;
import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.TraceLoaderSaver;
import de.prob.check.tracereplay.check.ReplayOptions;
import de.prob.check.tracereplay.check.TraceChecker;
import de.prob.check.tracereplay.check.TraceModifier;
import de.prob.check.tracereplay.check.exceptions.DeltaCalculationException;
import de.prob.check.tracereplay.check.exceptions.PrologTermNotDefinedException;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.FactoryProvider;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;


/**
 * Use this class ONCE per checking process. Reusing can cause incredible pain leading to the dark side of the force.
 */
public class TraceModificationChecker {

	TraceChecker traceChecker;
	private final TraceJsonFile traceJsonFile;
	private final PersistentTrace persistentTrace;
	private final Injector injector;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final Path traceJsonFilePath;
	private final ResourceBundle resourceBundle;
	private final StateSpace stateSpace;
	private final Map<String, OperationInfo> currentMachine;
	private Stage progressStage;
	private Thread traceCheckerProcess;

	public TraceModificationChecker(ReplayTrace replayTrace, StateSpace stateSpace,
									Injector injector, CurrentProject currentProject, StageManager stageManager)  {
		this(replayTrace.getTraceJsonFile(), replayTrace.getLocation(), stateSpace, injector, currentProject, stageManager);
	}

	public TraceModificationChecker(TraceJsonFile traceJsonFile, Path traceJsonFilePath, StateSpace stateSpace,
									Injector injector, CurrentProject currentProject, StageManager stageManager)  {
		this.traceJsonFilePath = traceJsonFilePath;
		this.traceJsonFile = traceJsonFile;
		this.persistentTrace = new PersistentTrace(traceJsonFile.getDescription(), traceJsonFile.getTransitionList());
		this.injector = injector;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.resourceBundle = injector.getInstance(ResourceBundle.class);
		this.currentMachine  = stateSpace.getLoadedMachine().getOperations();
		this.stateSpace = stateSpace;
	}

	private TraceChecker createSimplerChecker(StateSpace stateSpace, Path newPath, ReplayOptions replayOptions, ProgressMemory progressMemory) throws IOException, ModelTranslationError, DeltaCalculationException {
		return new TraceChecker(persistentTrace.getTransitionList(),
				new HashMap<>(traceJsonFile.getMachineOperationInfos()),
				new HashMap<>(stateSpace.getLoadedMachine().getOperations()),
				new HashSet<>(stateSpace.getLoadedMachine().getVariableNames()),
				new HashSet<>(traceJsonFile.getVariableNames()),
				newPath.toString(),
				injector,
				new MappingFactory(injector, stageManager),
				replayOptions,
				progressMemory);
	}

	private TraceChecker createComplexChecker(StateSpace stateSpace, Path newPath, Path oldPath, ReplayOptions replayOptions, ProgressMemory progressMemory) throws IOException, ModelTranslationError, PrologTermNotDefinedException, DeltaCalculationException {
		return new TraceChecker(persistentTrace.getTransitionList(),
				new HashMap<>(traceJsonFile.getMachineOperationInfos()),
				new HashMap<>(stateSpace.getLoadedMachine().getOperations()),
				new HashSet<>(traceJsonFile.getVariableNames()),
				new HashSet<>(stateSpace.getLoadedMachine().getVariableNames()),
				oldPath.toString(),
				newPath.toString(),
				injector,
				new MappingFactory(injector, stageManager),
				replayOptions,
				progressMemory);
	}

	/**
	 * Runs a analysis of the given data, don't run twice
	 */
	public void check(){
		executeCheck(false);
	}


	/**
	 * Variation of check. This method only will recheck. Split is necessary to avoid loading the same machine twice and comparing,
	 * would result in no changes discovered while trace file would indicate different.
	 */
	public void recheck(){
		executeCheck(true);
	}

	/**
	 * Does two things:
	 * 1) Prepares a progress bar to show the progress while running the trace modification
	 * 2) Starts the trace modification check in a parallel thread. The progress bar gets started in the UI. The
	 * calculation thread will update the progress bar. When the progress bar gets closed the second thread needs to be interrupted
	 * @param recheck indicates wherever an recheck is requested. A recheck means that the old machine is definitely not available
	 */
	private void executeCheck(boolean recheck){
		Path newPath = currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation());

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

			Path oldPath = null;
			try{
				oldPath = getFile(traceJsonFilePath, traceJsonFile.getMetadata().getModelName());
			}catch (FileNotFoundException ignored){}

			try {
				if(oldPath != null && oldPath.toFile().isFile() && !recheck){
					traceChecker = createComplexChecker(stateSpace, newPath, oldPath, replayOptions, progressMemory);
				}else{
					traceChecker = createSimplerChecker(stateSpace, newPath, replayOptions, progressMemory);
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
			if(traceModifier.succesfullTracing() && !traceModifier.originalMatchesProduced()){
				TraceModificationAlert dialog = new TraceModificationAlert(injector, stageManager, this, persistentTrace);
				stageManager.register(dialog);
				List<PersistentTrace> dialogResult = dialog.showAndWait().get();//The dialog can only return 0,1 or 2 results
				if(dialogResult.remove(persistentTrace)){
					result.add(traceJsonFilePath);
				}
				if(!dialogResult.isEmpty()){
					result.add(saveNewTrace(dialogResult.get(0)));
				}
			}else if(traceModifier.succesfullTracing() && traceModifier.originalMatchesProduced()){
				result.add(traceJsonFilePath);
			}
			else if(!traceModifier.succesfullTracing()) {
				result.addAll(traceNotReplayable());
			}

			return result;

		} catch (Exception e) {
			return traceNotReplayable();
		}
	}


	private Path saveNewTrace(PersistentTrace persistentTrace) throws IOException {

		String filename = Files.getNameWithoutExtension(traceJsonFilePath.toString());
		String newMachineName = currentProject.getCurrentMachine().getName();
		String modified = filename + "_edited_for_" + newMachineName+"." + Files.getFileExtension(traceJsonFilePath.toString());

		Path saveAt = currentProject.getLocation().resolve(Paths.get(modified));

		TraceFileHandler traceManager = injector.getInstance(TraceFileHandler.class);
		traceManager.save(traceJsonFile.changeTrace(persistentTrace).changeModelName(newMachineName).changeMachineInfos(currentMachine), saveAt);

		return saveAt;
	}


	public static Path getFile(Path path, String name) throws FileNotFoundException {

		try {
			String[] entries = path.getParent().toFile().list();

			if (entries == null) {
				throw new FileNotFoundException();
			}
			List<String> endings = Arrays.asList(".mch", ".ref", ".imp");
			List<String> candidates = Arrays.stream(entries)
					.filter(entry -> entry.contains("."))
					.filter(entry -> endings.contains(entry.substring(entry.lastIndexOf("."))))
					.filter(entry -> entry.substring(0, entry.lastIndexOf(".")).equals(name)).collect(Collectors.toList());
			if (candidates.size() == 0) {
				throw new FileNotFoundException();
			}
			return path.getParent().resolve(candidates.get(0));
		}catch (NullPointerException  e){
			throw new FileNotFoundException();
		}

	}

	private List<Path> traceNotReplayable(){
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION, resourceBundle.getString("traceModification.alert.traceNotReplayable") , ButtonType.YES, ButtonType.NO);
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
