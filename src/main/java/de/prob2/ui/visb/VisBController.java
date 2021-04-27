package de.prob2.ui.visb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Injector;

import de.prob.animator.command.ExecuteOperationException;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.command.LoadVisBSetAttributesCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBItem;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.exceptions.VisBException;
import de.prob2.ui.visb.exceptions.VisBNestedException;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;

import javafx.beans.value.ChangeListener;
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
	private final ChangeListener<Trace> currentTraceChangeListener;
	private final Injector injector;
	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final ResourceBundle bundle;

	private VisBVisualisation visBVisualisation;

	/**
	 * The VisBController constructor gets injected with ProB2-UI injector. In this method the final currentTraceListener is initialised as well. There is no further initialisation needed for this class.
	 * @param injector used for interaction with ProB2-UI
	 * @param stageManager currently not used
	 * @param currentProject used to add {@link ChangeListener} for changing machine
	 * @param currentTrace used to add {@link ChangeListener} for interacting with trace
	 * @param bundle used to access string resources
	 */
	@Inject
	public VisBController(final VisBFileHandler visBFileHandler, final Injector injector, final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final ResourceBundle bundle) {
		this.visBFileHandler = visBFileHandler;
		this.injector = injector;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		this.visBVisualisation = new VisBVisualisation();

		LOGGER.debug("Initialise TraceChangeListener");
		this.currentTraceChangeListener = ((observable, oldTrace, newTrace) -> {
			if(newTrace != null){
				if(newTrace.getCurrentState() != null && newTrace.getCurrentState().isInitialised()){
					updateVisualisation();
				} else {
					showUpdateVisualisationNotPossible();
					reloadVisualisation();
				}
			}
		});
	}

	/**
	 * Whenever a new machine is loaded, this method cleans out the visualisation, meaning it resets all current changes.
	 */
	private void startVisualisation(){
		if(currentProject.getCurrentMachine() != null) {
			currentProject.currentMachineProperty().addListener((observable, from, to) -> {
				//This prepares VisB for the new Visualisation
				closeCurrentVisualisation();
				this.visBVisualisation = new VisBVisualisation();
			});
			currentTrace.addListener(currentTraceChangeListener);
			injector.getInstance(VisBStage.class).onCloseRequestProperty().setValue(t -> this.clearListeners());
		}
	}

	/**
	 * This method is used for updating the visualisation.
	 */
	private void updateVisualisation(){
		if(this.visBVisualisation.isReady()) {
			applySVGChanges();
		} else if(visBVisualisation.getVisBEvents() == null){
			updateInfo("visb.infobox.visualisation.items.alert");
		} else if(visBVisualisation.getSvgPath() == null){
			updateInfo("visb.infobox.visualisation.svg.alert");
		}
	}

	private void applySVGChanges() {
		String svgChanges;
		VisBStage visBStage = injector.getInstance(VisBStage.class);

		String stateID = currentTrace.getCurrentState().getId();
		LoadVisBSetAttributesCommand setAttributesCmd = new LoadVisBSetAttributesCommand(stateID, visBVisualisation.getVisBItemMap());
		currentTrace.getStateSpace().execute(setAttributesCmd);

		injector.getInstance(VisBDebugStage.class).updateItems(visBVisualisation.getVisBItems());

		try {
			svgChanges = buildJQueryForChanges(visBVisualisation.getVisBItems());
		} catch(VisBNestedException | IllegalArgumentException | ProBError e){
			alert(e, "visb.controller.alert.eval.formulas.header", "visb.exception.visb.file.error.header");
			updateInfo("visb.infobox.visualisation.error");
			visBStage.clear();
			return;
		}

		// TO DO: parse formula once when loading the file to check for syntax errors
		if(svgChanges.isEmpty()){
			updateInfo("visb.infobox.no.change");
		} else {
			try {
				visBStage.runScript("resetDebugMessages()");
				visBStage.runScript("resetErrorMessages()");
				visBStage.runScript(svgChanges);
			} catch (JSException e){
				alert(e, "visb.exception.header","visb.controller.alert.visualisation.file");
				updateInfo("visb.infobox.visualisation.error");
				return;
			}
			//LOGGER.debug("Running script: "+svgChanges);
			updateInfo("visb.infobox.visualisation.updated.nr",countLines(svgChanges));
		}
	}

	/**
	 * Uses evaluateFormula to evaluate the visualisation items.
	 * @param visItems items given by the {@link VisBController}
	 * @return all needed jQueries in one string
	 * @throws EvaluationException from evaluating formula on trace
	 */
	private String buildJQueryForChanges(List<VisBItem> visItems) throws EvaluationException, VisBNestedException {
		StringBuilder jQueryForChanges = new StringBuilder();
		try {
			for(VisBItem visItem : visItems){
				String jQueryTemp = buildInvocation("changeAttribute", wrapAsString(visItem.getId()), wrapAsString(visItem.getAttribute()), visItem.getValue());
				jQueryForChanges.append(jQueryTemp);
			}
		} catch (EvaluationException e){
			throw(new VisBNestedException("Exception evaluating B formulas",e));
		}
		return jQueryForChanges.toString();
	}

	/**
	 * This method removes the ChangeListener on the Trace. It is used, when the VisB Window is closed.
	 */
	private void clearListeners(){
		currentTrace.removeListener(currentTraceChangeListener);
		this.visBVisualisation = new VisBVisualisation();
	}

	/**
	 * This method throws an ProB2-UI ExceptionAlert
	 */
	private void alert(Throwable ex, String header, String message, Object... params){
		Alert exceptionAlert = this.stageManager.makeExceptionAlert(ex, header, message, params);
		exceptionAlert.initOwner(injector.getInstance(VisBStage.class));
		exceptionAlert.showAndWait();
	}

	private static int countLines(String str) {
	   String[] lines = str.split("\r\n|\r|\n");
	   return  lines.length;
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
		VisBEvent event = visBVisualisation.getEventForID(id);
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
	 * @param svgFile svg file to be used
	 * @param jsonFile json file to be used
	 */
	private void setupHTMLFile(File svgFile, File jsonFile) throws VisBException, IOException{
		if(svgFile == null || !svgFile.exists()){
			throw new VisBException(bundle.getString("visb.exception.svg.empty"));
		}
		String svgContent = this.injector.getInstance(VisBFileHandler.class).fileToString(svgFile);
		if(svgContent != null && !svgContent.isEmpty()) {
			List<VisBOnClickMustacheItem> clickEvents = generateOnClickItems();
			this.injector.getInstance(VisBStage.class).initialiseWebView(svgFile, clickEvents, jsonFile, svgContent);
			updateInfo("visb.infobox.visualisation.svg.loaded");
		} else{
			throw new VisBException(bundle.getString("visb.exception.svg.empty"));
		}
	}

	void reloadVisualisation(){
		VisBVisualisation currentVisualisation = this.visBVisualisation;
		closeCurrentVisualisation();
		this.visBVisualisation = currentVisualisation;
		setupVisualisation();
		if(!visBVisualisation.isReady()){
			if(visBVisualisation.getSvgPath() != null) {
				updateInfo("visb.infobox.visualisation.error");
			} else {
				updateInfo("visb.infoboy.empty.reload");
			}
			return;
		}
		LOGGER.debug("Visualisation has been reloaded.");
	}

	void closeCurrentVisualisation(){
		if(visBVisualisation == null) return;
		this.clearListeners();
		this.injector.getInstance(VisBStage.class).clear();
		LOGGER.debug("Current visualisation is cleared and closed.");
	}

	/**
	 * Setting up the JSON / VisB file for internal usage via {@link VisBFileHandler}.
	 * @param visFile JSON / VisB file to be used
	 */
	void setupVisBFile(File visFile) {
		if(visFile == null){
			return;
		}
		currentTrace.removeListener(currentTraceChangeListener);
		try {
			this.visBVisualisation = visBFileHandler.constructVisualisationFromJSON(visFile);
			this.injector.getInstance(VisBDebugStage.class).initialiseListViews(visBVisualisation);
		} catch (ProBError e) {
			throw e;
		} catch (IOException e) {
			alert(e,"visb.exception.io", "visb.infobox.visualisation.error");
			updateInfo("visb.infobox.visualisation.error");
		} catch (RuntimeException e) {
			alert(e, "visb.exception.header", "visb.exception.visb.file.error");
			updateInfo("visb.infobox.visualisation.error");
		}
	}

	public void setupVisualisation(File file){
		try {
			setupVisBFile(file);
			setupHTMLFile(new File(this.visBVisualisation.getSvgPath().toUri()), file);
		} catch(VisBException e) {
			alert(e, "visb.exception.header", "visb.exception.visb.file.error.header");
			updateInfo("visb.infobox.visualisation.error");
			return;
		} catch (ProBError e) {
			// Set VisB Visualisation with VisB file only. This is then used for reload (after the JSON syntax errors are fixed)
			this.visBVisualisation = new VisBVisualisation(null, null, null, file);
			alert(e, "visb.exception.header", "visb.exception.visb.file.error");
			updateInfo("visb.infobox.visualisation.error");
			return;
		} catch(IOException e){
			alert(e, "visb.exception.header", "visb.exception.io");
			return;
		}
		showVisualisationAfterSetup();
	}

	private void showVisualisationAfterSetup() {
		if(this.visBVisualisation.isReady()) {
			startVisualisation();
			updateVisualisationIfPossible();
		}
		if(visBVisualisation.getVisBEvents() == null){
			updateInfo("visb.infobox.visualisation.error");
			alert(new VisBException(), "visb.exception.visb.file.error.header", "visb.exception.visb.file.error");
		} else if (visBVisualisation.getVisBEvents().isEmpty()){
			//There is no need to load on click functions, if no events are available.
			updateInfo("visb.infobox.visualisation.events.alert");
		} else {
			updateInfo("visb.infobox.visualisation.initialise");
		}
	}

	public void setupVisualisation(){
		setupVisualisation(this.visBVisualisation.getJsonFile());
	}

	/**
	 * As the name says, it updates the visualisation, if it is possible.
	 */
	private void updateVisualisationIfPossible(){
		LOGGER.debug("Trying to reload visualisation.");
		if(this.currentTrace.getCurrentState() != null && this.currentTrace.getCurrentState().isInitialised()){
			LOGGER.debug("Reloading visualisation...");
			//Updates visualisation, only if current state is initialised and visualisation items are not empty
			updateVisualisation();
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
		List<VisBOnClickMustacheItem> onClickItems = new ArrayList<>();
		for (VisBEvent visBEvent : this.visBVisualisation.getVisBEvents()) {
			addOnClickItem(visBEvent, onClickItems);
		}
		return onClickItems;
	}

	private void addOnClickItem(VisBEvent visBEvent, List<VisBOnClickMustacheItem> onClickItems) {
		String enterAction = visBEvent.getHovers().stream()
				.map(hover -> buildInvocation("changeAttribute", wrapAsString(hover.getHoverID()), wrapAsString(hover.getHoverAttr()), wrapAsString(hover.getHoverEnterVal())))
				.collect(Collectors.joining("\n"));
		String leaveAction = visBEvent.getHovers().stream()
				.map(hover -> buildInvocation("changeAttribute", wrapAsString(hover.getHoverID()), wrapAsString(hover.getHoverAttr()), wrapAsString(hover.getHoverLeaveVal())))
				.collect(Collectors.joining("\n"));
		String eventID = visBEvent.getId();
		String eventName = visBEvent.getEvent();
		onClickItems.add(new VisBOnClickMustacheItem(enterAction, leaveAction, eventID, eventName));

	}
}
