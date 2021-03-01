package de.prob2.ui.animation.tracereplay;

import com.google.common.io.Files;
import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.check.TraceChecker;
import de.prob.check.tracereplay.check.exceptions.PrologTermNotDefinedException;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.check.tracereplay.json.storage.TraceMetaData;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TraceModificationChecker {

	final TraceChecker traceChecker;
	private final TraceJsonFile traceJsonFile;
	private final PersistentTrace persistentTrace;
	private final Injector injector;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final TraceManager traceManager;
	private final Path path;
	private final ResourceBundle resourceBundle;
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceModificationChecker.class);



	public TraceModificationChecker(TraceManager traceManager, Path traceJsonFilePath, StateSpace stateSpace,
									Injector injector, CurrentProject currentProject, StageManager stageManager) throws IOException, ModelTranslationError {
		TraceChecker traceChecker1;
		this.path = traceJsonFilePath;
		this.traceManager = traceManager;
		traceJsonFile = traceManager.load(traceJsonFilePath);
		this.persistentTrace = traceJsonFile.getTrace();
		this.injector = injector;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.resourceBundle = injector.getInstance(ResourceBundle.class);


		Path oldPath = currentProject.getLocation().resolve(Paths.get( ((TraceMetaData) traceJsonFile.getMetaData()).getPath()));
		Path newPath = currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation());

		boolean flag = false;

		if(java.nio.file.Files.exists(path) && newPath!=oldPath) {
			try {
				traceChecker1 = new TraceChecker(persistentTrace.getTransitionList(),
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

			} catch (PrologTermNotDefinedException e) {
				LOGGER.error("A problem with the prolog core appeared:", e);
				traceChecker1 = createSimplerChecker(stateSpace, path);
			}
		}
		else {
			traceChecker1 = createSimplerChecker(stateSpace, newPath);


		}

		traceChecker = traceChecker1;
	}

	private TraceChecker createSimplerChecker(StateSpace stateSpace, Path newPath) throws IOException, ModelTranslationError {
		return new TraceChecker(persistentTrace.getTransitionList(),
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



	public List<Path> checkTrace(){

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
