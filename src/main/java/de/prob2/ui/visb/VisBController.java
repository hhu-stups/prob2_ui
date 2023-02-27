package de.prob2.ui.visb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Injector;

import de.prob.animator.command.ExecuteOperationException;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.command.GetVisBAttributeValuesCommand;
import de.prob.animator.command.GetVisBSVGObjectsCommand;
import de.prob.animator.command.LoadVisBCommand;
import de.prob.animator.command.ReadVisBEventsHoversCommand;
import de.prob.animator.command.ReadVisBItemsCommand;
import de.prob.animator.command.ReadVisBSvgPathCommand;
import de.prob.animator.command.VisBPerformClickCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBItem;
import de.prob.animator.domainobjects.VisBSVGObject;
import de.prob.exception.ProBError;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.Alert;

import netscape.javascript.JSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The VisBController controls the {@link VisBStage}.
 * Everything that can be done in Java only and uses interaction with ProB2-UI should be in here, not in the other classes.
 */
@Singleton
public class VisBController {
	private static final Logger LOGGER = LoggerFactory.getLogger(VisBController.class);

	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;
	private final I18n i18n;

	private final ObjectProperty<Path> visBPath;
	private final ObjectProperty<VisBVisualisation> visBVisualisation;
	private final ObservableMap<VisBItem.VisBItemKey, String> attributeValues;

	/**
	 * The VisBController constructor gets injected with ProB2-UI injector. In this method the final currentTraceListener is initialised as well. There is no further initialisation needed for this class.
	 * @param injector used for interaction with ProB2-UI
	 * @param stageManager currently not used
	 * @param currentTrace used to add {@link ChangeListener} for interacting with trace
	 * @param i18n used to access string resources
	 */
	@Inject
	public VisBController(final Injector injector, final StageManager stageManager, final CurrentTrace currentTrace, final I18n i18n) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.i18n = i18n;
		this.visBPath = new SimpleObjectProperty<>(this, "visBPath", null);
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

		this.visBPath.addListener((o, from, to) -> {
			if (to == null) {
				this.visBVisualisation.set(null);
			} else {
				this.setupVisualisation(to);
			}
		});
		currentTrace.addListener((o, from, to) -> {
			if (this.getVisBVisualisation() != null) {
				if (from != null && (to == null || !from.getStateSpace().equals(to.getStateSpace()))) {
					this.visBVisualisation.set(null);
				}
				this.updateVisualisationIfPossible();
			}
		});
	}

	public ObjectProperty<Path> visBPathProperty() {
		return this.visBPath;
	}

	public Path getVisBPath() {
		return this.visBPathProperty().get();
	}

	public void setVisBPath(final Path visBPath) {
		// Remark: The VisB path is reset to null, so that the listener for visBPath is triggered when the old visBPath is equal to the new one
		// Otherwise the listener is not triggered and thus the new visualization is not updated.
		// We had a similar issue on a ListProperty listener long time ago.
		// This was caused by JavaFX not triggering the listener when the old object is equal to the new one
		this.visBPathProperty().set(null);
		this.visBPathProperty().set(visBPath);
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

	private void applySVGChanges() {
		VisBStage visBStage = injector.getInstance(VisBStage.class);

		try {
			final GetVisBAttributeValuesCommand getAttributesCmd = new GetVisBAttributeValuesCommand(currentTrace.getCurrentState());
			currentTrace.getStateSpace().execute(getAttributesCmd);
			this.attributeValues.putAll(getAttributesCmd.getValues());
		} catch (ProBError e){
			alert(e, "visb.controller.alert.eval.formulas.header", "visb.exception.visb.file.error.header");
			updateInfo("visb.infobox.visualisation.error");
			visBStage.clear();
			return;
		}

		try {
			visBStage.resetMessages();
		} catch (JSException e){
			alert(e, "visb.exception.header","visb.controller.alert.visualisation.file");
			updateInfo("visb.infobox.visualisation.error");
		}
	}

	/**
	 * This method throws an ProB2-UI ExceptionAlert
	 */
	private void alert(Throwable ex, String header, String message, Object... params){
		Alert exceptionAlert = this.stageManager.makeExceptionAlert(ex, header, message, params);
		exceptionAlert.initOwner(injector.getInstance(VisBStage.class));
		exceptionAlert.showAndWait();
	}

	/**
	 * This redirects the information to the {@link VisBStage}.
	 * @param key bundlekey for string
	 */
	private void updateInfo(String key){
		injector.getInstance(VisBStage.class).updateInfo(i18n.translate(key));
	}

	private void updateInfo(String key, Object... params){
		injector.getInstance(VisBStage.class).updateInfo(i18n.translate(key, params));
	}

	/**
	 * This method is used by the {@link VisBConnector} to execute an event, whenever an svg item was clicked. Only one event per svg item is allowed.
	 * @param id of the svg item that was clicked
	 */
	public void executeEvent(String id, int pageX, int pageY, boolean shiftKey, boolean metaKey) {
		if(!currentTrace.getCurrentState().isInitialised()){
			executeBeforeInitialisation();
			return;
		}
		LOGGER.debug("Finding event for id: " + id);
		VisBEvent event = this.getVisBVisualisation().getEventsById().get(id);

		try {
			StateSpace stateSpace = currentTrace.getStateSpace();
			VisBPerformClickCommand performClickCommand = new VisBPerformClickCommand(stateSpace, id, Collections.emptyList(), currentTrace.getCurrentState().getId());
			stateSpace.execute(performClickCommand);
			List<Transition> transitions = performClickCommand.getTransitions();

			if (transitions.isEmpty()) {
				updateInfo("visb.infobox.no.events.for.id", id);
			} else {
				LOGGER.debug("Executing event for id: "+id + " and preds = " + event.getPredicates());
				Trace trace = currentTrace.get().addTransitions(transitions);
				LOGGER.debug("Finished executed event for id: "+id + " and preds = " + event.getPredicates());
				currentTrace.set(trace);
				RealTimeSimulator realTimeSimulator = injector.getInstance(RealTimeSimulator.class);
				for(Transition transition : transitions) {
					UIInteractionHandler uiInteraction = injector.getInstance(UIInteractionHandler.class);
					uiInteraction.addUserInteraction(realTimeSimulator, transition);
				}
				updateInfo("visb.infobox.execute.event", event.getEvent(), id);
			}
		} catch (ExecuteOperationException e) {
			LOGGER.debug("Cannot execute event for id: {}", id, e);
			updateInfo("visb.infobox.cannot.execute.event", event.getEvent(), id);
			if (e.getErrors().stream().anyMatch(err -> err.getType() == GetOperationByPredicateCommand.GetOperationErrorType.PARSE_ERROR)) {
				Alert alert = this.stageManager.makeExceptionAlert(e, "visb.exception.header", "visb.exception.parse", String.join("\n", e.getErrorMessages()));
				alert.initOwner(this.injector.getInstance(VisBStage.class));
				alert.show();
			}
		} catch (EvaluationException e) {
			LOGGER.debug("Cannot execute event for id: {}", id, e);
			updateInfo("visb.infobox.cannot.execute.event", event.getEvent(), id);
			Alert alert = this.stageManager.makeExceptionAlert(e, "visb.exception.header", "visb.exception.parse", e.getLocalizedMessage());
			alert.initOwner(this.injector.getInstance(VisBStage.class));
			alert.show();
		}
	}

	private void executeBeforeInitialisation() {
		Set<Transition> nextTransitions = currentTrace.get().getNextTransitions();
		if(currentTrace.get().getNextTransitions().size() == 1) {
			String transitionName = nextTransitions.stream().map(Transition::getName).collect(Collectors.toList()).get(0);
			Trace trace = currentTrace.get().execute(transitionName, new ArrayList<>());
			currentTrace.set(trace);
			RealTimeSimulator realTimeSimulator = injector.getInstance(RealTimeSimulator.class);
			for(Transition transition : nextTransitions) {
				UIInteractionHandler uiInteraction = injector.getInstance(UIInteractionHandler.class);
				uiInteraction.addUserInteraction(realTimeSimulator, transition);
			}
		} else {
			updateInfo("visb.infobox.events.not.initialise");
		}
	}

	void reloadVisualisation(){
		if (this.getVisBPath() == null) {
			return;
		}
		this.visBVisualisation.set(null);
		setupVisualisation(this.getVisBPath());
		if (this.getVisBVisualisation() == null) {
			updateInfo("visb.infobox.visualisation.error");
			return;
		}
		LOGGER.debug("Visualisation has been reloaded.");
	}

	/**
	 * This method takes a JSON / VisB file as input and returns a {@link VisBVisualisation} object.
	 * @param jsonPath path to the VisB JSON file
	 * @return VisBVisualisation object
	 */
	private VisBVisualisation constructVisualisationFromJSON(Path jsonPath) throws IOException {
		jsonPath = jsonPath.toRealPath();
		if (!Files.isRegularFile(jsonPath) && !jsonPath.toFile().isDirectory()) {
			throw new IOException("Given json path is not a regular file: " + jsonPath);
		}
		
		LoadVisBCommand loadCmd = new LoadVisBCommand(jsonPath.toFile().isDirectory() ? "" : jsonPath.toString());
		
		currentTrace.getStateSpace().execute(loadCmd);
		ReadVisBSvgPathCommand svgCmd = new ReadVisBSvgPathCommand(jsonPath.toFile().isDirectory() ? "" : jsonPath.toString());
		
		currentTrace.getStateSpace().execute(svgCmd);
		String svgPathString = svgCmd.getSvgPath();
		
		Path svgPath = jsonPath.resolveSibling(svgPathString).toRealPath();
		if (!svgPathString.isEmpty() && (!Files.isRegularFile(svgPath) || Files.size(svgPath) <= 0)) {
			throw new IOException("Given svg path is not a non-empty regular file: " + svgPath);
		}
		
		ReadVisBItemsCommand readVisBItemsCommand = new ReadVisBItemsCommand();
		currentTrace.getStateSpace().execute(readVisBItemsCommand);
		List<VisBItem> items = readVisBItemsCommand.getItems();
		
		ReadVisBEventsHoversCommand readEventsCmd = new ReadVisBEventsHoversCommand();
		currentTrace.getStateSpace().execute(readEventsCmd);
		List<VisBEvent> visBEvents = readEventsCmd.getEvents();
		
		GetVisBSVGObjectsCommand command = new GetVisBSVGObjectsCommand();
		currentTrace.getStateSpace().execute(command);
		List<VisBSVGObject> visBSVGObjects = command.getSvgObjects();
		
		return new VisBVisualisation(svgPath, items, visBEvents, visBSVGObjects);
	}

	private void setupVisualisation(final Path visBPath){
		try {
			this.visBVisualisation.set(constructVisualisationFromJSON(visBPath));
		} catch (Exception e) {
			this.visBVisualisation.set(null);
			LOGGER.warn("error while loading visb file", e);
			alert(e, "visb.exception.visb.file.error.header", "visb.exception.visb.file.error");
			updateInfo("visb.infobox.visualisation.error");
			return;
		}

		updateInfo("visb.infobox.visualisation.initialise");
		updateVisualisationIfPossible();
	}

	/**
	 * As the name says, it updates the visualisation, if it is possible.
	 */
	private void updateVisualisationIfPossible(){
		LOGGER.debug("Trying to reload visualisation.");
		if(this.currentTrace.getCurrentState() != null && this.currentTrace.getCurrentState().isInitialised()){
			LOGGER.debug("Reloading visualisation...");
			//Updates visualisation, only if current state is initialised and visualisation items are not empty
			applySVGChanges();
		} else {
			showUpdateVisualisationNotPossible();
		}
	}

	private void showUpdateVisualisationNotPossible(){
		updateInfo("visb.infobox.visualisation.updated");
		injector.getInstance(VisBStage.class).showModelNotInitialised();
	}
}
