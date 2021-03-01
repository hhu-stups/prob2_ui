package de.prob2.ui.animation.tracereplay;

import com.google.common.io.Files;
import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.check.ReplayOptions;
import de.prob.check.tracereplay.check.TraceChecker;
import de.prob.check.tracereplay.check.exceptions.DeltaCalculationException;
import de.prob.check.tracereplay.check.exceptions.PrologTermNotDefinedException;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.scripting.ModelTranslationError;
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TraceModificationChecker {

	TraceChecker traceChecker;
	private final TraceJsonFile traceJsonFile;
	private final PersistentTrace persistentTrace;
	private final Injector injector;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final Path path;
	private final ResourceBundle resourceBundle;
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceModificationChecker.class);
	private final CompletableFuture<TraceChecker> futureTraceChecker = new CompletableFuture<>();
	private final Stage progressStage;



	public TraceModificationChecker(TraceJsonFile traceJsonFile, Path traceJsonFilePath, StateSpace stateSpace,
									Injector injector, CurrentProject currentProject, StageManager stageManager) throws IOException, ModelTranslationError, PrologTermNotDefinedException {
		this.path = traceJsonFilePath;
	
		this.traceJsonFile = traceJsonFile;
		this.persistentTrace = new PersistentTrace(traceJsonFile.getDescription(), traceJsonFile.getTransitionList());
		this.injector = injector;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.resourceBundle = injector.getInstance(ResourceBundle.class);


		Path oldPath = getFile(traceJsonFilePath, traceJsonFile.getMetadata().getModelName());
		System.out.println("path: " + oldPath);
		LOGGER.debug(oldPath.toString());
		Path newPath = currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation());


		ReplayOptionsOverview traceOptionChoice = new ReplayOptionsOverview(traceJsonFile.getVariableNames(), traceJsonFile.getMachineOperationInfos(), stageManager);
		Optional<ReplayOptions> optionResult = traceOptionChoice.showAndWait();
		ReplayOptions replayOptions = optionResult.get();

		progressStage = new Stage();

		ProgressMemory progressMemory = ProgressMemory.setupForTraceChecker(resourceBundle, stageManager, progressStage);

		stageManager.register(progressStage, "progressStage");

		progressStage.initOwner(stageManager.getMainStage().getOwner());
		progressStage.initModality(Modality.WINDOW_MODAL);

		ExecutorService executorService = Executors.newSingleThreadExecutor();

		Runnable traceCheckerProcess = () -> {

			try {
				if(java.nio.file.Files.exists(oldPath)) {
					futureTraceChecker.complete(createComplexChecker(stateSpace, newPath, oldPath, replayOptions, progressMemory));
				}
				else {
					futureTraceChecker.complete(createSimplerChecker(stateSpace, newPath, replayOptions, progressMemory));
				}
				Platform.runLater(progressStage::close);
			} catch (IOException | ModelTranslationError | PrologTermNotDefinedException | DeltaCalculationException e) {
				//LOGGER.;
				e.printStackTrace();
			}    };

		executorService.submit(traceCheckerProcess);

		progressStage.showAndWait();



	}

	private TraceChecker createSimplerChecker(StateSpace stateSpace, Path newPath, ReplayOptions replayOptions, ProgressMemory progressMemory) throws IOException, ModelTranslationError, DeltaCalculationException {
		return new TraceChecker(persistentTrace.getTransitionList(),
				new HashMap<>(stateSpace.getLoadedMachine().getOperations()),
				new HashMap<>(traceJsonFile.getMachineOperationInfos()),
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



	public List<Path> checkTrace(){

		traceChecker = futureTraceChecker.join();


		if(traceChecker == null){
			throw new NullPointerException();
		}

		final List<Path> result = new ArrayList<>();

		if(traceChecker.getTraceModifier().isDirty()) {

			final List<PersistentTrace> persistentTraces = new ArrayList<>();


			if(traceChecker.getTraceModifier().getChangelogPhase3II().isEmpty()) {
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION, resourceBundle.getString("traceModification.alert.traceNotReplayable") , ButtonType.YES, ButtonType.NO);
				alert.setTitle("No suitable configuration found.");
				Optional<ButtonType> dialogResult = alert.showAndWait();
				if(dialogResult.get()==ButtonType.YES){
					result.add(path);
				}
			}else {
				TraceModificationAlert dialog = new TraceModificationAlert(injector, stageManager, this, persistentTrace);
				stageManager.register(dialog);
				Optional<List<PersistentTrace>> dialogResult = dialog.showAndWait();
				persistentTraces.addAll(dialogResult.get());
			}


			if (persistentTraces.remove(persistentTrace)) {
				result.add(path);
			}

			persistentTraces.forEach(element -> {

				String filename = Files.getNameWithoutExtension(path.toString());
				String modified = filename + "_edited_for_" + currentProject.getCurrentMachine().getName() +"." + Files.getFileExtension(path.toString());

				Path saveAt = currentProject.getLocation().resolve(Paths.get(modified));

				result.add(saveAt);
				try {
					TraceManager traceManager = injector.getInstance(TraceManager.class);
					traceManager.save(saveAt, traceJsonFile.changeTrace(element));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}else{
			result.add(path);
		}

		return result;

	}

	private static Path getFile(Path path, String name){
		String[] entries = path.toFile().list();
		if(entries==null){
			return Paths.get("");
		}
		List<String> candidates = Arrays.stream(entries).filter(entry -> entry.startsWith(name)).collect(Collectors.toList());
		if(candidates.size()<1){
			return Paths.get("");
		}
		return path.resolve(candidates.get(0));
	}

}
