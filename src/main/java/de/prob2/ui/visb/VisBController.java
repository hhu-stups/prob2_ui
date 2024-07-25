package de.prob2.ui.visb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetVisBAttributeValuesCommand;
import de.prob.animator.command.GetVisBDefaultSVGCommand;
import de.prob.animator.command.GetVisBSVGObjectsCommand;
import de.prob.animator.command.LoadVisBCommand;
import de.prob.animator.command.ReadVisBEventsHoversCommand;
import de.prob.animator.command.ReadVisBItemsCommand;
import de.prob.animator.command.ReadVisBSvgPathCommand;
import de.prob.animator.command.VisBPerformClickCommand;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBItem;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.Alert;

import netscape.javascript.JSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The VisBController controls the {@link VisBView}.
 * Everything that can be done in Java only and uses interaction with ProB2-UI should be in here, not in the other classes.
 */
@Singleton
public final class VisBController {
	private static final Logger LOGGER = LoggerFactory.getLogger(VisBController.class);

	public static final Path NO_PATH = Paths.get("");

	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;

	private final ObjectProperty<Path> absoluteVisBPath;
	private final ObjectProperty<Path> relativeVisBPath;
	private final ObjectProperty<VisBVisualisation> visBVisualisation;
	private final ObservableMap<VisBItem.VisBItemKey, String> attributeValues;

	@Inject
	public VisBController(Injector injector, StageManager stageManager, CurrentProject currentProject, CurrentTrace currentTrace) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;

		this.absoluteVisBPath = new SimpleObjectProperty<>(this, "absoluteVisBPath", null);
		this.relativeVisBPath = new SimpleObjectProperty<>(this, "relativeVisBPath", null);
		this.visBVisualisation = new SimpleObjectProperty<>(this, "visBVisualisation", null);
		this.attributeValues = FXCollections.observableHashMap();
		initialize();
	}

	private void initialize() {
		this.visBVisualisation.addListener((o, from, to) -> {
			if (to == null) {
				this.attributeValues.clear();
			}
		});

		currentTrace.addListener((o, from, to) -> {
			if (this.getVisBVisualisation() != null) {
				if (from != null && (to == null || !from.getStateSpace().equals(to.getStateSpace()))) {
					this.hideVisualisation();
				}
				this.updateVisualisationIfPossible();
			}
		});
	}

	public static Path resolveVisBPath(Path projectLocation, Path relativeVisBPath) {
		if (NO_PATH.equals(relativeVisBPath)) {
			return VisBController.NO_PATH;
		} else if (relativeVisBPath.isAbsolute()) {
			throw new IllegalArgumentException("Tried to resolve an already absolute VisB path: " + relativeVisBPath);
		} else {
			return projectLocation.resolve(relativeVisBPath);
		}
	}

	public static Path relativizeVisBPath(Path projectLocation, Path absoluteVisBPath) {
		if (NO_PATH.equals(absoluteVisBPath)) {
			return VisBController.NO_PATH;
		} else if (!absoluteVisBPath.isAbsolute()) {
			throw new IllegalArgumentException("Tried to relativize an already relative VisB path: " + absoluteVisBPath);
		} else {
			return projectLocation.relativize(absoluteVisBPath);
		}
	}

	public ReadOnlyObjectProperty<Path> absoluteVisBPathProperty() {
		return this.absoluteVisBPath;
	}

	public Path getAbsoluteVisBPath() {
		return this.absoluteVisBPathProperty().get();
	}

	public ReadOnlyObjectProperty<Path> relativeVisBPathProperty() {
		return this.relativeVisBPath;
	}

	public Path getRelativeVisBPath() {
		return this.relativeVisBPathProperty().get();
	}

	/**
	 * Hide the currently loaded visualisation, but remember its path (if any) so that the user can reload it.
	 * This should be called after visualisation errors that can possibly be fixed by a reload.
	 */
	public void hideVisualisation() {
		this.visBVisualisation.set(null);
	}

	/**
	 * Close any currently loaded visualisation.
	 * Unlike {@link #hideVisualisation()}, this also unsets the visualisation file path,
	 * so the user cannot reload the visualisation unless they explicitly re-select it.
	 */
	public void closeVisualisation() {
		this.hideVisualisation();
		this.absoluteVisBPath.set(null);
		this.relativeVisBPath.set(null);
	}

	public void loadFromAbsolutePath(Path path) throws IOException {
		Objects.requireNonNull(path, "path");
		Path relativePath = relativizeVisBPath(currentProject.getLocation(), path);
		this.absoluteVisBPath.set(path);
		this.relativeVisBPath.set(relativePath);
		this.reloadVisualisation();
	}

	public void loadFromRelativePath(Path path) throws IOException {
		Objects.requireNonNull(path, "path");
		Path absolutePath = resolveVisBPath(currentProject.getLocation(), path);
		this.absoluteVisBPath.set(absolutePath);
		this.relativeVisBPath.set(path);
		this.reloadVisualisation();
	}

	public ReadOnlyObjectProperty<VisBVisualisation> visBVisualisationProperty() {
		return this.visBVisualisation;
	}

	public VisBVisualisation getVisBVisualisation() {
		return this.visBVisualisationProperty().get();
	}

	public ObservableMap<VisBItem.VisBItemKey, String> getAttributeValues() {
		return this.attributeValues;
	}

	private void applySVGChanges(State state) {
		VisBView visBView = injector.getInstance(VisBView.class);

		try {
			final GetVisBAttributeValuesCommand getAttributesCmd = new GetVisBAttributeValuesCommand(state);
			state.getStateSpace().execute(getAttributesCmd);
			this.attributeValues.putAll(getAttributesCmd.getValues());
		} catch (ProBError e){
			alert(e, "visb.controller.alert.eval.formulas.header", "visb.exception.visb.file.error.header");
			visBView.clear();
			return;
		}

		try {
			visBView.resetMessages();
		} catch (JSException e){
			alert(e, "visb.exception.header","visb.controller.alert.visualisation.file");
		}
	}

	/**
	 * This method throws an ProB2-UI ExceptionAlert
	 */
	private void alert(Throwable ex, String header, String message, Object... params){
		Alert exceptionAlert = this.stageManager.makeExceptionAlert(ex, header, message, params);
		exceptionAlert.initOwner(injector.getInstance(VisBView.class).getScene().getWindow());
		exceptionAlert.showAndWait();
	}

	/**
	 * This method is used by the {@link VisBView} to execute an event, whenever an svg item was clicked. Only one event per svg item is allowed.
	 * @param id of the svg item that was clicked
	 */
	public void executeEvent(String id, int pageX, int pageY, boolean shiftKey, boolean metaKey) {
		Trace trace = currentTrace.get();
		if (!trace.getCurrentState().isInitialised()) {
			executeBeforeInitialisation();
			return;
		}
		LOGGER.debug("Finding event for id: {}", id);
		VisBEvent event = this.getVisBVisualisation().getEventsById().get(id);

		try {
			VisBPerformClickCommand performClickCommand = new VisBPerformClickCommand(trace.getStateSpace(), id, Collections.emptyList(), trace.getCurrentState().getId());
			trace.getStateSpace().execute(performClickCommand);
			List<Transition> transitions = performClickCommand.getTransitions();

			if (transitions.isEmpty()) {
				LOGGER.debug("No events found for id: {}", id);
			} else {
				LOGGER.debug("Executing event for id: {} and preds = {}", id, event.getPredicates());
				Trace newTrace = trace.addTransitions(transitions);
				LOGGER.debug("Finished executed event for id: {} and preds = {}", id, event.getPredicates());
				currentTrace.set(newTrace);
				RealTimeSimulator realTimeSimulator = injector.getInstance(RealTimeSimulator.class);
				for(Transition transition : transitions) {
					UIInteractionHandler uiInteraction = injector.getInstance(UIInteractionHandler.class);
					uiInteraction.addUserInteraction(realTimeSimulator, transition);
				}
			}
		} catch (ProBError e) {
			LOGGER.debug("Cannot execute event for id: {}", id, e);
			Alert alert = this.stageManager.makeExceptionAlert(e, "visb.exception.header", "visb.exception.parse", e.getLocalizedMessage());
			alert.initOwner(this.injector.getInstance(VisBView.class).getScene().getWindow());
			alert.show();
		}
	}

	private void executeBeforeInitialisation() {
		Trace trace = currentTrace.get();
		Set<Transition> nextTransitions = trace.getNextTransitions();
		if (nextTransitions.size() == 1) {
			Transition transition = nextTransitions.iterator().next();
			currentTrace.set(trace.add(transition));
			RealTimeSimulator realTimeSimulator = injector.getInstance(RealTimeSimulator.class);
			UIInteractionHandler uiInteraction = injector.getInstance(UIInteractionHandler.class);
			uiInteraction.addUserInteraction(realTimeSimulator, transition);
		} else {
			LOGGER.debug("Cannot perform non-deterministic initialization from VisB");
		}
	}

	/**
	 * This method takes a JSON / VisB file as input and returns a {@link VisBVisualisation} object.
	 * @param stateSpace the ProB animator instance using which to load the VisB file
	 * @param jsonPath path to the VisB JSON file
	 * @return VisBVisualisation object
	 */
	private static VisBVisualisation constructVisualisationFromJSON(StateSpace stateSpace, Path jsonPath) throws IOException {
		if (!jsonPath.equals(NO_PATH)) {
			jsonPath = jsonPath.toRealPath();
			if (!Files.isRegularFile(jsonPath)) {
				throw new IOException("Given json path is not a regular file: " + jsonPath);
			}
		}

		String jsonPathString = jsonPath.equals(NO_PATH) ? "" : jsonPath.toString();

		var loadCmd = new LoadVisBCommand(jsonPathString);
		var svgCmd = new ReadVisBSvgPathCommand(jsonPathString);
		var itemsCmd = new ReadVisBItemsCommand();
		var eventsCmd = new ReadVisBEventsHoversCommand();
		var svgObjectsCmd = new GetVisBSVGObjectsCommand();
		var defaultSVGCmd = new GetVisBDefaultSVGCommand();
		stateSpace.execute(loadCmd, svgCmd, itemsCmd, eventsCmd, svgObjectsCmd, defaultSVGCmd);

		String svgPathString = svgCmd.getSvgPath();
		final Path svgPath;
		final String svgContent;
		if (svgPathString.isEmpty()) {
			svgPath = NO_PATH;
			svgContent = defaultSVGCmd.getSVGFileContents();
		} else {
			if (jsonPath.equals(NO_PATH)) {
				svgPath = Paths.get(svgPathString).toRealPath();
			} else {
				svgPath = jsonPath.resolveSibling(svgPathString).toRealPath();
			}
			if (!Files.isRegularFile(svgPath) || Files.size(svgPath) <= 0) {
				throw new IOException("Given svg path is not a non-empty regular file: " + svgPath);
			}
			svgContent = Files.readString(svgPath);
		}
		
		return new VisBVisualisation(svgPath, svgContent, itemsCmd.getItems(), eventsCmd.getEvents(), svgObjectsCmd.getSvgObjects());
	}

	void reloadVisualisation() throws IOException {
		// Hide the previous visualisation before loading a new one.
		// This ensures that listeners on visBVisualisation are always called
		// and prevents an old visualisation remaining visible after an error.
		this.hideVisualisation();

		Path visBPath = this.getAbsoluteVisBPath();
		if (visBPath == null) {
			return;
		}

		this.visBVisualisation.set(constructVisualisationFromJSON(currentTrace.getStateSpace(), visBPath));

		updateVisualisationIfPossible();
	}

	/**
	 * As the name says, it updates the visualisation, if it is possible.
	 */
	private void updateVisualisationIfPossible(){
		LOGGER.debug("Trying to reload visualisation.");
		State currentState = this.currentTrace.getCurrentState();
		if (currentState != null && currentState.isInitialised()) {
			LOGGER.debug("Reloading visualisation...");
			//Updates visualisation, only if current state is initialised and visualisation items are not empty
			applySVGChanges(currentState);
		} else {
			injector.getInstance(VisBView.class).showModelNotInitialised();
		}
		LOGGER.debug("Visualisation has been reloaded.");
	}
}
