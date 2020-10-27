package de.prob2.ui.visb;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.Injector;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.command.ExecuteOperationException;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.MustacheTemplateManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.exceptions.VisBException;
import de.prob2.ui.visb.exceptions.VisBNestedException;
import de.prob2.ui.visb.exceptions.VisBParseException;
import de.prob2.ui.visb.visbobjects.VisBEvent;
import de.prob2.ui.visb.visbobjects.VisBHover;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Alert;
import netscape.javascript.JSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static de.prob2.ui.visb.VisBMustacheTemplateHandler.getModelNotInitialisedString;

/**
 * The VisBController controls the {@link VisBStage}, as well as using the {@link VisBFileHandler} and {@link VisBParser}.
 * Everything that can be done in Java only and uses interaction with ProB2-UI should be in here, not in the other classes.
 */
@Singleton
public class VisBController {
	private static final Logger LOGGER = LoggerFactory.getLogger(VisBController.class);

	private final CurrentTrace currentTrace;
	private final ChangeListener<Trace> currentTraceChangeListener;
	private final Injector injector;
	private final StageManager stageManager;
	private CurrentProject currentProject;
	private ResourceBundle bundle;

	private VisBVisualisation visBVisualisation;

	/**
	 * The VisBController constructor gets injected with ProB2-UI injector. In this method the final currentTraceListener is initialised as well. There is no further initialisation needed for this class.
	 * @param injector used for interaction with ProB2-UI
	 * @param stageManager currently not used
	 * @param currentProject used to add {@link ChangeListener} for changing machine
	 * @param currentTrace used to add {@link ChangeListener} for interacting with trace
	 */
	@Inject
	public VisBController(final Injector injector, final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final ResourceBundle bundle) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		this.visBVisualisation = new VisBVisualisation();

		LOGGER.debug("Initialise TraceChangeListener");
		currentTraceChangeListener = ((observable, oldTrace, newTrace) -> {
			if(newTrace != null){
				if(newTrace.getCurrentState() != null && newTrace.getCurrentState().isInitialised()){
					updateVisualisation();
				} else {
					showUpdateVisualisationNotPossible();
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
				//injector.getInstance(VisBStage.class).clear();
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
		//SVG Javascript && ValueTranslator (if necessary)
		if(this.visBVisualisation.isReady()) {
			String svgChanges;
			try{
				svgChanges = injector.getInstance(VisBParser.class).evaluateFormulas(this.visBVisualisation.getVisBItems());
				// TO DO: parse formula once when loading the file to check for syntax errors
			} catch(VisBParseException | VisBNestedException | IllegalArgumentException | BCompoundException | ProBError e){
				alert(e, "visb.controller.alert.eval.formulas.header", "visb.exception.visb.file.error.header");
				this.clearListeners();
				return;
			}
			if(svgChanges == null || svgChanges.isEmpty()){
				updateInfo("visb.infobox.no.change");
			} else {
				try {
					injector.getInstance(VisBStage.class).runScript(
					   "$(\"#visb_debug_messages\").text(\"\");\n" + // reset VisB debug text (if it exists)
					   "$(\"#visb_error_messages ul\").empty();\n"  // reset VisB error list
						+ svgChanges);
				} catch (JSException e){
					alert(e, "visb.exception.header","visb.controller.alert.visualisation.file");
					updateInfo("visb.infobox.visualisation.error");
					return;
				}
				//LOGGER.debug("Running script: "+svgChanges);
				updateInfo("visb.infobox.visualisation.updated.nr",countLines(svgChanges));
			}
		} else{
			if(!visBVisualisation.isReady()) {
				updateInfo("visb.infobox.visualisation.none.loaded");
			} else if(visBVisualisation.getVisBEvents() == null){
				updateInfo("visb.infobox.visualisation.items.alert");
			} else if(visBVisualisation.getSvgPath() == null){
				updateInfo("visb.infobox.visualisation.svg.alert");
			} else{
				updateInfo("visb.infobox.visualisation.error");
			}
		}
	}

	/**
	 * This method removes the ChangeListener on the Trace. It is used, when the VisB Window is closed.
	 */
	void clearListeners(){
		currentTrace.removeListener(currentTraceChangeListener);
		this.visBVisualisation = new VisBVisualisation();
	}

	/**
	 * This method throws an ProB2-UI ExceptionAlert
	 */
	private void alert(Throwable ex, String header, String message, Object... params){
		this.stageManager.makeExceptionAlert(ex, header, message, params).showAndWait();
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
	void executeEvent(String id, int pageX, int pageY, boolean shiftKey, boolean metaKey){
		LOGGER.debug("Finding event for id: "+id);
		if(!currentTrace.getCurrentState().isInitialised()){
			updateInfo("visb.infobox.events.not.initialise");
			return;
		}
		VisBEvent event = visBVisualisation.getEventForID(id);
		// TO DO: adapt predicates or add predicate for Click Coordinates
		if(event == null || event.getEvent().equals("")) {
			updateInfo("visb.infobox.no.events.for.id", id);
		} else {
			Trace trace = currentTrace.get();
			// if (trace.canExecuteEvent(event.getEvent(), event.getPredicates())) {
			try {
				// perform replacements to transmit event information:
				ArrayList<String> preds= new ArrayList<String>();
				preds.addAll(event.getPredicates());
				for(int j = 0; j < preds.size(); j++) {
					preds.set(j,preds.get(j).replace("%shiftKey", (shiftKey ? "TRUE" : "FALSE"))
											.replace("%metaKey",  (metaKey  ? "TRUE" : "FALSE"))
											.replace("%pageX", Integer.toString(pageX))
											.replace("%pageY", Integer.toString(pageY))
								 );
				}
				LOGGER.debug("Executing event for id: "+id + " and preds = " + preds);
				trace = trace.execute(event.getEvent(), preds);
				LOGGER.debug("Finished executed event for id: "+id + " and preds = " + preds);
				currentTrace.set(trace);
				updateInfo("visb.infobox.execute.event", event.getEvent(), id);
			} catch (ExecuteOperationException e) {
				if(e.getErrors().stream().anyMatch(err -> err.getType() == GetOperationByPredicateCommand.GetOperationErrorType.PARSE_ERROR)) {
					Alert alert = this.stageManager.makeExceptionAlert(e, "visb.exception.header", "visb.exception.parse", String.join("\n", e.getErrorMessages()));
					alert.initOwner(this.injector.getInstance(VisBStage.class));
					alert.show();
				}
				LOGGER.debug("Cannot execute event for id: "+e);
				updateInfo("visb.infobox.cannot.execute.event", event.getEvent(), id);
			} catch (EvaluationException e) {
				Alert alert = this.stageManager.makeExceptionAlert(e, "visb.exception.header", "visb.exception.parse", e.getLocalizedMessage());
				alert.initOwner(this.injector.getInstance(VisBStage.class));
				alert.show();
				LOGGER.debug("Cannot execute event for id: "+e);
				updateInfo("visb.infobox.cannot.execute.event", event.getEvent(), id);
			}
		}
	}
	

	/**
	 * Setting up the svg file for internal usage via {@link VisBFileHandler}.
	 * @param file svg file to be used
	 */
	private void setupSVGFile(File file) throws VisBException, IOException{
		if(file == null || !file.exists()){
			throw new VisBException(bundle.getString("visb.exception.svg.empty"));
		}
		String svgFile;
		svgFile = this.injector.getInstance(VisBFileHandler.class).fileToString(file);
		if(svgFile != null && !svgFile.isEmpty()) {
			this.injector.getInstance(VisBStage.class).initialiseWebView(file, svgFile);
			updateInfo("visb.infobox.visualisation.svg.loaded");
		} else{
			throw new VisBException(bundle.getString("visb.exception.svg.empty"));
		}
	}

	void reloadVisualisation(){
		if(!visBVisualisation.isReady()){
			updateInfo("visb.infoboy.empty.reload");
			return;
		}
		VisBVisualisation currentVisualisation = this.visBVisualisation;
		closeCurrentVisualisation();
		this.visBVisualisation = currentVisualisation;
		setupVisualisation();
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
	void setupVisBFile(File visFile){
		if(visFile == null){
			return;
		}
		this.visBVisualisation = null;
		clearListeners();
		try {
			this.visBVisualisation = VisBFileHandler.constructVisualisationFromJSON(visFile);
		} catch (IOException e) {
			alert(e,"visb.exception.io", "visb.infobox.visualisation.error");
			updateInfo("visb.infobox.visualisation.error");
			return;
		} catch(Exception e){
			alert(e, "visb.exception.header", "visb.exception.visb.file.error");
			updateInfo("visb.infobox.visualisation.error");
			return;
		}
	}

	public void setupVisualisation(){
		if(visBVisualisation == null){
			updateInfo("visb.infobox.visualisation.error");
			alert(new VisBException(), "visb.exception.visb.file.error.header", "visb.exception.visb.file.error");
			return;
		} else if (visBVisualisation.getVisBItems().isEmpty()){
			updateInfo("visb.infobox.visualisation.items.alert");
		} else if (visBVisualisation.getVisBEvents().isEmpty()){
			//There is no need to load on click functions, if no events are available.
			updateInfo("visb.infobox.visualisation.events.alert");
		} else {
			updateInfo("visb.infobox.visualisation.initialise");
		}
		try {
			setupSVGFile(new File(this.visBVisualisation.getSvgPath().toUri()));
			setupVisBFile(this.visBVisualisation.getJsonFile());
		} catch(VisBException e){
			alert(e, "visb.exception.header", "visb.exception.visb.file.error.header");
			return;
		} catch(IOException e){
			alert(e, "visb.exception.header", "visb.exception.io");
			return;
		}
		if(this.visBVisualisation.isReady()) {
			this.injector.getInstance(VisBStage.class).initialiseListViews(visBVisualisation);
			loadOnClickFunctions();
			startVisualisation();
			updateVisualisationIfPossible();
			//LOGGER.debug("LOADED:\n"+this.visBVisualisation.toString());
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
			updateVisualisation();
		} else {
			showUpdateVisualisationNotPossible();
		}
	}
	private void showUpdateVisualisationNotPossible(){
		updateInfo("visb.infobox.visualisation.updated.nr",0);
		injector.getInstance(VisBStage.class).runScript(getModelNotInitialisedString(visBVisualisation.getJsonFile()));
	}

	/**
	 * Checks if on click functionality for the svg items can be added, yet. If not, nothing happens. If it can be added, the JQuery that is needed is build and executed via {@link VisBStage}.
	 */
	private void loadOnClickFunctions(){
		if(visBVisualisation.isReady()) {
			StringBuilder onClickEventQuery = new StringBuilder();
			for (VisBEvent visBEvent : this.visBVisualisation.getVisBEvents()) {
				String event = visBEvent.getEvent();
				if (!(event.equals("") || isValidTopLevelEvent(event))) { // for "" we allow to provide just hover
					if (visBEvent.eventIsOptional()) {
					    System.out.println("Ignoring event " + event);
					} else {
						Alert alert = this.stageManager.makeExceptionAlert(new VisBException(), "visb.exception.header", "visb.infobox.invalid.event", event);
						alert.initOwner(this.injector.getInstance(VisBStage.class));
						alert.show();
					}
				} else {
					StringBuilder EnterAction = new StringBuilder();
					StringBuilder LeaveAction = new StringBuilder();
					ArrayList<VisBHover> hvs = visBEvent.getHovers();
					for(int j = 0; j < hvs.size(); j++) {
						EnterAction.append("    changeAttribute(\"#" + hvs.get(j).getHoverId() + "\",\""
								  + hvs.get(j).getHoverAttr() + "\", \""+ hvs.get(j).getHoverEnterVal() + "\");\n");
						LeaveAction.append("    changeAttribute(\"#" + hvs.get(j).getHoverId() + "\",\""
								  + hvs.get(j).getHoverAttr() + "\", \""+ hvs.get(j).getHoverLeaveVal() + "\");\n");
					}
					String queryPart = VisBMustacheTemplateHandler.getQueryString(visBEvent, EnterAction, LeaveAction);
					System.out.println(queryPart);
					onClickEventQuery.append(queryPart);
				}
			}
			try {
				this.injector.getInstance(VisBStage.class).loadOnClickFunctions(onClickEventQuery.toString());
			} catch (JSException e) {
				alert(new VisBException(), "visb.exception.header", "visb.exception.no.items.and.events");
				updateInfo(bundle.getString("visb.infobox.visualisation.events.alert"));
			}
		}
	}
	
	private boolean isValidTopLevelEvent(String event) {
			System.out.println("Check if event toplevel " + event);
			OperationInfo operationInfo = 
			        currentTrace.getStateSpace().getLoadedMachine().getMachineOperationInfo(event);
			return operationInfo != null && operationInfo.isTopLevel();
	}
}
