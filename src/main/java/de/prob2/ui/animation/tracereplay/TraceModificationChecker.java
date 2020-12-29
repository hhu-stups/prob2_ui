package de.prob2.ui.animation.tracereplay;

import com.google.common.io.Files;
import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.check.TraceChecker;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.check.tracereplay.json.storage.TraceMetaData;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TraceModificationChecker {

	final TraceChecker traceChecker;
	private final TraceJsonFile traceJsonFile;
	private final PersistentTrace persistentTrace;
	private final Injector injector;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final TraceManager traceManager;
	private final Path path;

	public TraceModificationChecker(TraceManager traceManager, Path traceJsonFilePath, StateSpace stateSpace,
									Injector injector, CurrentProject currentProject, StageManager stageManager) throws IOException, ModelTranslationError {
		this.path = traceJsonFilePath;
		this.traceManager = traceManager;
		traceJsonFile = traceManager.load(traceJsonFilePath);
		this.persistentTrace = traceJsonFile.getTrace();
		this.injector = injector;
		this.currentProject = currentProject;
		this.stageManager = stageManager;


		Path oldPath = currentProject.getLocation().resolve(Paths.get( ((TraceMetaData) traceJsonFile.getMetaData()).getPath()));
		Path newPath = currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation());

		if(java.nio.file.Files.exists(path) && newPath!=oldPath) {
			traceChecker = new TraceChecker(persistentTrace.getTransitionList(),
					new HashMap<>(traceJsonFile.getMachineOperationInfos()),
					new HashMap<>(stateSpace.getLoadedMachine().getOperations()),
					new HashSet<>(traceJsonFile.getVariableNames()),
					new HashSet<>(stateSpace.getLoadedMachine().getVariableNames()),
					new HashSet<>(stateSpace.getLoadedMachine().getSetNames()),
					new HashSet<>(stateSpace.getLoadedMachine().getConstantNames()),
					oldPath.toString(),
					newPath.toString(),
					injector,
					new MappingFactory(injector, stageManager));
		}else {

			traceChecker = new TraceChecker(persistentTrace.getTransitionList(),
					new HashMap<>(stateSpace.getLoadedMachine().getOperations()),
					new HashMap<>(traceJsonFile.getMachineOperationInfos()),
					new HashSet<>(stateSpace.getLoadedMachine().getVariableNames()),
					new HashSet<>(traceJsonFile.getVariableNames()),
					new HashSet<>(stateSpace.getLoadedMachine().getSetNames()),
					new HashSet<>(stateSpace.getLoadedMachine().getConstantNames()),
					newPath.toString(),
					injector,
					new MappingFactory(injector, stageManager));


		}

	}



	public List<Path> checkTrace(){

		final List<Path> result = new ArrayList<>();

		if(traceChecker.getTraceModifier().isDirty()) {

			final List<PersistentTrace> persistentTraces = new ArrayList<>();


			if(traceChecker.getTraceModifier().getChangelogPhase3II().isEmpty()) {
				ResourceBundle resourceBundle = injector.getInstance(ResourceBundle.class);
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


}
