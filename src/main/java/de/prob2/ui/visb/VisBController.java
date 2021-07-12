package de.prob2.ui.visb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Injector;

import de.prob.animator.command.ExecuteOperationException;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.command.GetVisBAttributeValuesCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBItem;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
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

import static de.prob2.ui.internal.JavascriptFunctionInvoker.buildInvocation;
import static de.prob2.ui.internal.JavascriptFunctionInvoker.wrapAsString;

/**
 * The VisBController controls the {@link VisBStage}, as well as using the {@link VisBFileHandler}.
 * Everything that can be done in Java only and uses interaction with ProB2-UI should be in here, not in the other classes.
 */
@Singleton
public class VisBController {
	private static final Logger LOGGER = LoggerFactory.getLogger(VisBController.class);

	private final VisBFileHandler visBFileHandler;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	private final ObjectProperty<Path> visBPath;
	private final ObjectProperty<VisBVisualisation> visBVisualisation;
	private final ObservableMap<VisBItem.VisBItemKey, String> attributeValues;

	/**
	 * The VisBController constructor gets injected with ProB2-UI injector. In this method the final currentTraceListener is initialised as well. There is no further initialisation needed for this class.
	 * @param injector used for interaction with ProB2-UI
	 * @param stageManager currently not used
	 * @param currentTrace used to add {@link ChangeListener} for interacting with trace
	 * @param bundle used to access string resources
	 */
	@Inject
	public VisBController(final VisBFileHandler visBFileHandler, final Injector injector, final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle) {
		this.visBFileHandler = visBFileHandler;
		this.injector = injector;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		this.visBPath = new SimpleObjectProperty<>(this, "visBPath", null);
		this.visBVisualisation = new SimpleObjectProperty<>(this, "visBVisualisation", null);
		this.attributeValues = FXCollections.observableHashMap();
		this.visBVisualisation.addListener((o, from, to) -> {
			if (to == null) {
				this.attributeValues.clear();
				this.injector.getInstance(VisBStage.class).clear();
				LOGGER.debug("Current visualisation is cleared and closed.");
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

		final List<String> svgChanges = buildJQueryForChanges(this.attributeValues);

		// TO DO: parse formula once when loading the file to check for syntax errors
		if(svgChanges.isEmpty()){
			updateInfo("visb.infobox.no.change");
		} else {
			try {
				visBStage.runScript("resetDebugMessages()");
				visBStage.runScript("resetErrorMessages()");
				visBStage.runScript(String.join("", svgChanges));
			} catch (JSException e){
				alert(e, "visb.exception.header","visb.controller.alert.visualisation.file");
				updateInfo("visb.infobox.visualisation.error");
				return;
			}
			//LOGGER.debug("Running script: "+svgChanges);
			updateInfo("visb.infobox.visualisation.updated.nr", svgChanges.size());
		}
	}

	/**
	 * Uses evaluateFormula to evaluate the visualisation items.
	 * @param attributeValues new attribute values for all items
	 * @return all needed jQueries
	 */
	private static List<String> buildJQueryForChanges(final Map<VisBItem.VisBItemKey, String> attributeValues) {
		final List<String> calls = new ArrayList<>();
		attributeValues.forEach((k, v) -> calls.add(buildInvocation("changeAttribute", wrapAsString(k.getId()), wrapAsString(k.getAttribute()), v)));
		return calls;
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
		injector.getInstance(VisBStage.class).updateInfo(bundle.getString(key));
	}

	private void updateInfo(String key, Object... params){
		injector.getInstance(VisBStage.class).updateInfo(String.format(bundle.getString(key), params));
	}

	/**
	 * This method is used by the {@link VisBConnector} to execute an event, whenever an svg item was clicked. Only one event per svg item is allowed.
	 * @param id of the svg item that was clicked
	 */
	public void executeEvent(String id, int pageX, int pageY, boolean shiftKey, boolean metaKey){
		if(!currentTrace.getCurrentState().isInitialised()){
			executeBeforeInitialisation();
			return;
		}
		LOGGER.debug("Finding event for id: " + id);
		VisBEvent event = this.getVisBVisualisation().getEventForID(id);
		// TO DO: adapt predicates or add predicate for Click Coordinates
		if(event == null || event.getEvent().equals("")) {
			updateInfo("visb.infobox.no.events.for.id", id);
		} else {
			try {
				executeEvent(event, currentTrace.get(), id, pageX, pageY, shiftKey, metaKey);
			} catch (Exception e) {
				handleExecuteOperationError(e, event, id);
			}
		}
	}

	private void executeBeforeInitialisation() {
		Set<Transition> nextTransitions = currentTrace.get().getNextTransitions();
		if(currentTrace.get().getNextTransitions().size() == 1) {
			String transitionName = nextTransitions.stream().map(Transition::getName).collect(Collectors.toList()).get(0);
			Trace trace = currentTrace.get().execute(transitionName, new ArrayList<>());
			currentTrace.set(trace);
		} else {
			updateInfo("visb.infobox.events.not.initialise");
		}
	}

	private void executeEvent(VisBEvent event, Trace trace, String id, int pageX, int pageY, boolean shiftKey, boolean metaKey) {
		// perform replacements to transmit event information:
		List<String> preds = event.getPredicates();
		for(int j = 0; j < preds.size(); j++) {
			preds.set(j, preds.get(j).replace("%shiftKey", (shiftKey ? "TRUE" : "FALSE"))
					.replace("%metaKey",  (metaKey  ? "TRUE" : "FALSE"))
					.replace("%pageX", Integer.toString(pageX))
					.replace("%pageY", Integer.toString(pageY)));
		}
		LOGGER.debug("Executing event for id: "+id + " and preds = " + preds);
		trace = trace.execute(event.getEvent(), preds);
		LOGGER.debug("Finished executed event for id: "+id + " and preds = " + preds);
		currentTrace.set(trace);
		updateInfo("visb.infobox.execute.event", event.getEvent(), id);
	}

	private void handleExecuteOperationError(Exception e, VisBEvent event, String id) {
		if (e instanceof ExecuteOperationException) {
			if (((ExecuteOperationException) e).getErrors().stream().anyMatch(err -> err.getType() == GetOperationByPredicateCommand.GetOperationErrorType.PARSE_ERROR)) {
				Alert alert = this.stageManager.makeExceptionAlert(e, "visb.exception.header", "visb.exception.parse", String.join("\n", ((ExecuteOperationException) e).getErrorMessages()));
				alert.initOwner(this.injector.getInstance(VisBStage.class));
				alert.show();
			}
			LOGGER.debug("Cannot execute event for id: " + e);
			updateInfo("visb.infobox.cannot.execute.event", event.getEvent(), id);
		} else if(e instanceof EvaluationException) {
			Alert alert = this.stageManager.makeExceptionAlert(e, "visb.exception.header", "visb.exception.parse", e.getLocalizedMessage());
			alert.initOwner(this.injector.getInstance(VisBStage.class));
			alert.show();
			LOGGER.debug("Cannot execute event for id: "+e);
			updateInfo("visb.infobox.cannot.execute.event", event.getEvent(), id);
		}
	}
	

	/**
	 * Setting up the html file, it also sets the svg file for internal usage via {@link VisBFileHandler}.
	 * @param svgPath svg file to be used
	 */
	private void setupHTMLFile(final Path svgPath) throws IOException{
		String svgContent = new String(Files.readAllBytes(svgPath), StandardCharsets.UTF_8);
		if(!svgContent.isEmpty()) {
			List<VisBOnClickMustacheItem> clickEvents = generateOnClickItems();
			this.injector.getInstance(VisBStage.class).initialiseWebView(clickEvents, svgContent);
			updateInfo("visb.infobox.visualisation.svg.loaded");
		} else{
			throw new IOException(bundle.getString("visb.exception.svg.empty"));
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

	public void setupVisualisation(final Path visBPath){
		try {
			this.visBVisualisation.set(visBFileHandler.constructVisualisationFromJSON(visBPath));
			setupHTMLFile(this.getVisBVisualisation().getSvgPath());
		} catch (IOException | ProBError e) {
			this.visBVisualisation.set(null);
			alert(e, "visb.exception.header", "visb.exception.visb.file.error");
			updateInfo("visb.infobox.visualisation.error");
			return;
		}
		showVisualisationAfterSetup();
	}

	private void showVisualisationAfterSetup() {
		if (this.getVisBVisualisation() == null) {
			updateInfo("visb.infobox.visualisation.error");
			final Alert alert = this.stageManager.makeAlert(Alert.AlertType.ERROR, "visb.exception.visb.file.error.header", "visb.exception.visb.file.error");
			alert.initOwner(injector.getInstance(VisBStage.class));
			alert.showAndWait();
		} else {
			updateInfo("visb.infobox.visualisation.initialise");
			updateVisualisationIfPossible();
		}
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
		if("root".equals(this.currentTrace.get().getCurrent().toString())) {
			updateInfo("visb.infobox.visualisation.updated.nr", 0);
			injector.getInstance(VisBStage.class).runScript("showModelNotInitialised()");
		}
	}

	/**
	 * Checks if on click functionality for the svg items can be added, yet. If not, nothing happens. If it can be added, the JQuery that is needed is built and executed via {@link VisBStage}.
	 */
	private List<VisBOnClickMustacheItem> generateOnClickItems(){
		return this.getVisBVisualisation().getVisBEvents().stream()
			.map(VisBController::generateOnClickItem)
			.collect(Collectors.toList());
	}

	private static VisBOnClickMustacheItem generateOnClickItem(VisBEvent visBEvent) {
		String enterAction = visBEvent.getHovers().stream()
				.map(hover -> buildInvocation("changeAttribute", wrapAsString(hover.getHoverID()), wrapAsString(hover.getHoverAttr()), wrapAsString(hover.getHoverEnterVal())))
				.collect(Collectors.joining("\n"));
		String leaveAction = visBEvent.getHovers().stream()
				.map(hover -> buildInvocation("changeAttribute", wrapAsString(hover.getHoverID()), wrapAsString(hover.getHoverAttr()), wrapAsString(hover.getHoverLeaveVal())))
				.collect(Collectors.joining("\n"));
		String eventID = visBEvent.getId();
		String eventName = visBEvent.getEvent();
		return new VisBOnClickMustacheItem(enterAction, leaveAction, eventID, eventName);
	}
}
