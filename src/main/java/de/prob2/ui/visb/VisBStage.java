package de.prob2.ui.visb;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.io.CharStreams;
import com.google.inject.Injector;
import com.google.inject.Provider;

import de.prob.animator.command.ExportVisBForCurrentStateCommand;
import de.prob.animator.command.ExportVisBForHistoryCommand;
import de.prob.animator.command.ReadVisBPathFromDefinitionsCommand;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob.animator.domainobjects.VisBSVGObject;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.helpsystem.HelpSystem;
import de.prob2.ui.helpsystem.HelpSystemStage;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.SafeBindings;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.DefaultPathDialog;
import de.prob2.ui.simulation.SimulatorStage;
import de.prob2.ui.visb.help.UserManualStage;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds the main user interface and interacts with the {@link VisBController} and {@link VisBConnector} classes.
 */
@Singleton
public class VisBStage extends Stage {

	public enum VisBExportKind {
		CURRENT_STATE, CURRENT_TRACE
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(VisBStage.class);
	private final Injector injector;
	private final I18n i18n;
	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final Provider<DefaultPathDialog> defaultPathDialogProvider;
	private final VisBController visBController;
	private final FileChooserManager fileChooserManager;
	private final TraceFileHandler traceFileHandler;

	@FXML
	private MenuBar visbMenuBar;
	@FXML
	private Button loadVisualisationButton;
	@FXML
	private Button reloadVisualisationButton;
	@FXML
	private Button manageDefaultVisualisationButton;
	@FXML
	private Button openSimulationButton;
	@FXML
	private StackPane zoomingPane;
	@FXML
	private WebView webView;
	@FXML
	private MenuItem editMenu_reload;
	@FXML
	private MenuItem editMenu_close;
	@FXML
	private MenuItem fileMenu_close;
	@FXML
	private MenuItem fileMenu_visB;
	@FXML
	private MenuItem fileMenu_export;
	@FXML
	private MenuItem viewMenu_zoomIn;
	@FXML
	private MenuItem viewMenu_zoomOut;
	@FXML
	private MenuItem viewMenu_zoomFontsIn;
	@FXML
	private MenuItem viewMenu_zoomFontsOut;
	@FXML
	private MenuItem helpMenu_userManual;
	@FXML
	private MenuItem helpMenu_helpPage;
	@FXML
	private MenuItem saveTraceItem;
	@FXML
	private MenuItem saveTraceAndAddTestsItem;
	@FXML
	private MenuItem saveTraceAndRecordTestsItem;
	@FXML
	private MenuItem exportHistoryItem;
	@FXML
	private MenuItem exportCurrentStateItem;
	@FXML
	private Label information;
	@FXML
	private VBox placeholder;
	@FXML
	private HelpButton helpButton;

	/**
	 * The public constructor of this class is injected with the ProB2-UI injector.
	 * @param injector ProB2-UI injector
	 * @param stageManager ProB2-UI stageManager
	 * @param currentProject ProB2-UI currentProject
	 */
	@Inject
	public VisBStage(final Injector injector, final StageManager stageManager, final CurrentProject currentProject,
					 final CurrentTrace currentTrace, final I18n i18n, final FileChooserManager fileChooserManager,
					 final Provider<DefaultPathDialog> defaultPathDialogProvider, final VisBController visBController, final TraceFileHandler traceFileHandler) {
		super();
		this.injector = injector;
		this.i18n = i18n;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.fileChooserManager = fileChooserManager;
		this.defaultPathDialogProvider = defaultPathDialogProvider;
		this.visBController = visBController;
		this.traceFileHandler = traceFileHandler;
		this.stageManager.loadFXML(this, "visb_plugin_stage.fxml");
	}

	/**
	 * With this method a visible stage with an empty WebView and an empty ListView is initialised.
	 */
	@FXML
	public void initialize(){
		this.stageManager.setMacMenuBar(this, visbMenuBar);
		this.helpMenu_userManual.setOnAction(e -> injector.getInstance(UserManualStage.class).show());
		this.helpMenu_helpPage.setOnAction(e -> openHelpPage());
		this.helpButton.setHelpContent("mainmenu.visualisations.visB", null);
		this.loadVisualisationButton.setOnAction(e -> loadVisBFile());
		this.fileMenu_visB.setOnAction(e -> loadVisBFile());
		this.fileMenu_close.setOnAction(e -> sendCloseRequest());
		this.fileMenu_export.setOnAction(e -> exportImage());
		this.editMenu_reload.setOnAction(e -> reloadVisualisation());
		this.editMenu_close.setOnAction(e -> closeVisualisation());
		this.viewMenu_zoomIn.setOnAction(e -> zoomIn());
		this.viewMenu_zoomOut.setOnAction(e -> zoomOut());
		// zoom fonts in/out (but only of those that are not given a fixed size):
		this.viewMenu_zoomFontsIn.setOnAction(e -> webView.setFontScale(webView.getFontScale()*1.25));
		this.viewMenu_zoomFontsOut.setOnAction(e -> webView.setFontScale(webView.getFontScale()/1.25));
		this.titleProperty().bind(
				Bindings.when(visBController.visBPathProperty().isNull())
						.then(i18n.translateBinding("visb.title"))
						.otherwise(i18n.translateBinding(
								"visb.currentVisualisation",
								SafeBindings.createSafeStringBinding(
										() -> currentProject.getLocation().relativize(visBController.getVisBPath()).toString(),
										currentProject, visBController.visBPathProperty()
								)
						))
		);

		ChangeListener<? super Machine> machineListener = (observable, from, to) -> {
			manageDefaultVisualisationButton.disableProperty().unbind();
			manageDefaultVisualisationButton.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(visBController.visBPathProperty().isNull()));
		};

		ChangeListener<? super VisBVisualisation> visBListener = (o, from, to) -> {
			if (to == null) {
				this.clear();
			} else {
				this.loadSvgFile(to);
				updateInfo(i18n.translate("visb.infobox.visualisation.svg.loaded"));
				this.runWhenLoaded(() -> {
					final JSObject window = this.getJSWindow();
					final VisBConnector visBConnector = injector.getInstance(VisBConnector.class);
					updateDynamicSVGObjects(to);
					for (final VisBEvent event : to.getEvents()) {
						window.call("addClickEvent", visBConnector, event.getId(), event.getEvent(), event.getHovers().toArray(new VisBHover[0]));
					}
				});
			}
		};

		ChangeListener<? super StateSpace> stateSpaceListener = (o, from, to) -> loadVisBFileFromMachine(currentProject.getCurrentMachine(), to);

		this.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
			this.currentProject.currentMachineProperty().removeListener(machineListener);
			this.visBController.visBVisualisationProperty().removeListener(visBListener);
			this.currentTrace.stateSpaceProperty().removeListener(stateSpaceListener);
			visBController.setVisBPath(null);
		});
		//Load VisB file from machine, when window is opened and set listener on the current machine
		this.addEventFilter(WindowEvent.WINDOW_SHOWING, event -> {
			this.currentProject.currentMachineProperty().addListener(machineListener);
			this.visBController.visBVisualisationProperty().addListener(visBListener);
			this.currentTrace.stateSpaceProperty().addListener(stateSpaceListener);
			loadVisBFileFromMachine(currentProject.getCurrentMachine(), currentTrace.getStateSpace());

			machineListener.changed(null, null, currentProject.getCurrentMachine());
			visBListener.changed(null, null, visBController.getVisBVisualisation());
			stateSpaceListener.changed(null, null, currentTrace.getStateSpace());
		});

		this.reloadVisualisationButton.disableProperty().bind(visBController.visBPathProperty().isNull());

		saveTraceItem.setOnAction(e -> {
			try {
				traceFileHandler.save(currentTrace.get(), currentProject.getCurrentMachine());
			} catch (IOException | RuntimeException exc) {
				traceFileHandler.showSaveError(exc);
			}
		});
		exportHistoryItem.setOnAction(e -> saveHTMLExport(VisBExportKind.CURRENT_TRACE));
		exportCurrentStateItem.setOnAction(e -> saveHTMLExport(VisBExportKind.CURRENT_STATE));

		LOGGER.debug("JavaFX WebView user agent: {}", this.webView.getEngine().getUserAgent());
		this.webView.getEngine().setOnAlert(event -> showJavascriptAlert(event.getData()));
		this.webView.getEngine().setOnError(this::treatJavascriptError);

		this.visBController.getAttributeValues().addListener((MapChangeListener<VisBItem.VisBItemKey, String>)change -> {
			if (change.wasAdded()) {
				try {
					this.changeAttribute(change.getKey().getId(), change.getKey().getAttribute(), change.getValueAdded());
					updateInfo(i18n.translate("visb.infobox.visualisation.updated"));
				} catch (final JSException e) {
					LOGGER.error("JavaScript error while updating VisB attributes", e);
					alert(e, "visb.exception.header","visb.controller.alert.visualisation.file");
					updateInfo(i18n.translate("visb.infobox.visualisation.error"));
				}
			}
		});

		injector.getInstance(VisBDebugStage.class).initOwner(this);
	}

	@FXML
	public void openHelpPage() {
		final HelpSystemStage helpSystemStage = injector.getInstance(HelpSystemStage.class);
		final HelpSystem helpSystem = injector.getInstance(HelpSystem.class);
		helpSystem.openHelpForKeyAndAnchor("mainmenu.visualisations.visB", null);
		helpSystemStage.show();
		helpSystemStage.toFront();
	}

	private static Path getPathFromDefinitions(final StateSpace stateSpace) {
		ReadVisBPathFromDefinitionsCommand cmd = new ReadVisBPathFromDefinitionsCommand();
		stateSpace.execute(cmd);
		if (cmd.getPath() == null) {
			// null means that there is no VISB_JSON_FILE in the model's DEFINITIONS.
			return null;
		} else if (cmd.getPath().isEmpty()) {
			// VISB_JSON_FILE == "" means that there is no separate JSON file
			// and the VisB items/events are written as DEFINITIONS.
			return VisBController.NO_PATH;
		} else {
			return stateSpace.getModel().getModelFile().toPath().resolveSibling(cmd.getPath());
		}
	}

	public void loadVisBFileFromMachine(final Machine machine, final StateSpace stateSpace) {
		visBController.setVisBPath(null);
		if(machine != null && stateSpace != null) {
			final Path visBVisualisation = machine.getVisBVisualisation();
			final Path visBPath;
			if (visBVisualisation != null) {
				visBPath = currentProject.getLocation().resolve(visBVisualisation);
			} else {
				visBPath = getPathFromDefinitions(stateSpace);
			}
			visBController.setVisBPath(visBPath);
		}
	}

	private void sendCloseRequest(){
		this.fireEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST));
	}

	/**
	 * After loading the svgFile and preparing it in the {@link VisBController} the WebView is initialised.
	 * @param svgContent the image/ svg, that should to be loaded into the context of the WebView
	 */
	private void initialiseWebView(String svgContent, String baseUrl) {
		this.placeholder.setVisible(false);
		this.webView.setVisible(true);
		String htmlFile = generateHTMLFileWithSVG(svgContent, baseUrl);
		this.webView.getEngine().loadContent(htmlFile);
	}

	private void updateDynamicSVGObjects(VisBVisualisation visBVisualisation) {
		List<VisBSVGObject> svgObjects = visBVisualisation.getSVGObjects();
		// TODO: Maybe use templates
		if(!svgObjects.isEmpty()) {
			StringBuilder scriptString = new StringBuilder();
			scriptString.append("if(document.querySelector(\"svg\") != null) {\n");
			for(VisBSVGObject svgObject : svgObjects) {
				String id = svgObject.getId();
				String object = svgObject.getObject();
				Map<String, String> attributes = svgObject.getAttributes();
				scriptString.append(String.format(Locale.ROOT, "var new__%s = document.createElementNS(\"http://www.w3.org/2000/svg\",\"%s\");\n", id, object));
				scriptString.append(String.format(Locale.ROOT, "new__%s.setAttribute(\"id\",\"%s\");\n", id, id));
				for(Map.Entry<String, String> entry : attributes.entrySet()) {
					if (entry.getKey().equals("text")) {
					   // text attribute needs to be set differently:
					   scriptString.append(String.format(Locale.ROOT, "new__%s.textContent = \"%s\";\n", id, entry.getValue()));
					} else {
					   scriptString.append(String.format(Locale.ROOT, "new__%s.setAttribute(\"%s\",\"%s\");\n", id, entry.getKey(), entry.getValue()));
					}
				}
				scriptString.append(String.format(Locale.ROOT, "document.querySelector(\"svg\").appendChild(new__%s);\n", id));
			}
			scriptString.append("}");
			//System.out.println("Script: "+ scriptString);
			webView.getEngine().executeScript(scriptString.toString());
		}
	}

	private void loadSvgFile(final VisBVisualisation visBVisualisation) {
		final Path path = visBVisualisation.getSvgPath();
		final String baseUrl;
		if (path.equals(VisBController.NO_PATH)) {
			baseUrl = "";
		} else {
			baseUrl = path.getParent().toUri().toString();
		}
		this.initialiseWebView(visBVisualisation.getSvgContent(), baseUrl);
	}

	private void treatJavascriptError(WebErrorEvent event) {
		LOGGER.debug("JavaScript ERROR: " + event.getMessage());
		alert(event.getException(), "visb.exception.header", "visb.stage.alert.webview.jsalert", event.getMessage());
	}

	private void showJavascriptAlert(String message) {
		LOGGER.debug("JavaScript ALERT: " + message);
		final Alert alert = this.stageManager.makeAlert(Alert.AlertType.ERROR, "visb.exception.header", "visb.stage.alert.webview.jsalert", message);
		alert.initOwner(this);
		alert.showAndWait();
	}

	private JSObject getJSWindow() {
		return (JSObject)this.webView.getEngine().executeScript("window");
	}

	/**
	 * This method clears our the WebView and the ListView and removes possible listeners, so that the VisBStage no longer interacts with anything.
	 */
	void clear(){
		LOGGER.debug("Clear the stage!");
		this.webView.setVisible(false);
		this.placeholder.setVisible(true);
		injector.getInstance(VisBDebugStage.class).clear();
	}

	/**
	 * Run the given {@link Runnable} once the {@link WebView} has finished loading.
	 * If the {@link WebView} is already fully loaded,
	 * the {@link Runnable} is executed immediately.
	 * If the {@link WebView} fails to load,
	 * the {@link Runnable} is never executed.
	 *
	 * @param runnable the code to run once the {@link WebView} has finished loading
	 */
	private void runWhenLoaded(final Runnable runnable) {
		if(webView.getEngine().getLoadWorker().getState().equals(Worker.State.RUNNING)){
			// execute code once page fully loaded
			// https://stackoverflow.com/questions/12540044/execute-a-task-after-the-webview-is-fully-loaded
			webView.getEngine().getLoadWorker().stateProperty().addListener(
				//Use new constructor instead of lambda expression to access change listener with keyword this
				new ChangeListener<Worker.State>() {
					@Override
					public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
						switch (newValue) {
							case SUCCEEDED:
							case FAILED:
							case CANCELLED:
								webView.getEngine().getLoadWorker().stateProperty().removeListener(this);
						}
						if (newValue != Worker.State.SUCCEEDED) {
							return;
						}
						runnable.run();
					}
				}
			);
		} else {
			runnable.run();
		}
	}

	public void changeAttribute(final String id, final String attribute, final String value) {
		this.runWhenLoaded(() -> this.getJSWindow().call("changeAttribute", id, attribute, value));
	}

	public void showModelNotInitialised() {
		this.runWhenLoaded(() -> this.getJSWindow().call("showModelNotInitialised"));
	}

	public void resetMessages() {
		this.runWhenLoaded(() -> this.getJSWindow().call("resetMessages"));
	}

	/**
	 * Setter for the info label.
	 * @param text to be set
	 */
	void updateInfo(String text){
		information.setText(text);
	}

	/**
	 * On click function for the button and file menu item
	 */
	@FXML
	public void loadVisBFile() {
		if(currentProject.getCurrentMachine() == null){
			LOGGER.debug("Tried to start visualisation when no machine was loaded.");
			final Alert alert = this.stageManager.makeAlert(Alert.AlertType.ERROR, "visb.stage.alert.load.machine.header", "visb.exception.no.machine");
			alert.initOwner(this);
			alert.showAndWait();
			return;
		}
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("visb.stage.filechooser.title"));
		fileChooser.getExtensionFilters().addAll(
				fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.visBVisualisation", "json")
		);
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		if(path != null) {
			clear();
			visBController.setVisBPath(path);
			for(VisBItem.VisBItemKey key : visBController.getAttributeValues().keySet()) {
				changeAttribute(key.getId(), key.getAttribute(), visBController.getAttributeValues().get(key));
			}
		}
	}

	/**
	 * This method throws an ProB2-UI ExceptionAlert
	 */
	private void alert(Throwable ex, String header, String body, Object... params){
		final Alert alert = this.stageManager.makeExceptionAlert(ex, header, body, params);
		alert.initOwner(this);
		alert.showAndWait();
	}

	private void exportImage() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("visb.stage.filechooser.export.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getPngFilter());
		Path path = fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		exportImageWithPath(path);
	}

	public void exportImageWithPath(Path path) {
		if(path != null) {
			File file = path.toFile();
			if(!this.isShowing()) {
				this.show();
			}
			WritableImage snapshot = webView.snapshot(new SnapshotParameters(), null);
			RenderedImage image = SwingFXUtils.fromFXImage(snapshot,null);
			try {
				ImageIO.write(image, "png", file);
			} catch (IOException e) {
				alert(e, "visb.stage.image.export.error.title","visb.stage.image.export.error");
			}

		}
	}

	@FXML
	public void reloadVisualisation() {
		visBController.reloadVisualisation();
	}

	@FXML
	public void closeVisualisation() {
		visBController.setVisBPath(null);
	}

	@FXML
	public void zoomIn() {
		webView.setZoom(webView.getZoom()*1.2);
	}

	@FXML
	public void zoomOut() {
		webView.setZoom(webView.getZoom()/1.2);
	}

	@FXML
	private void openSimulation() {
		SimulatorStage simulatorStage = injector.getInstance(SimulatorStage.class);
		simulatorStage.show();
		simulatorStage.toFront();
	}

	private String generateHTMLFileWithSVG(String svgContent, String baseUrl) {
		final InputStream inputStream = this.getClass().getResourceAsStream("visb_html_view.html");
		if (inputStream == null) {
			throw new AssertionError("VisB HTML template resource not found - this should never happen");
		}
		try (final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			return CharStreams.toString(reader)
				.replace("@BASE_URL@", baseUrl)
				.replace("@SVG_CONTENT@", svgContent);
		} catch (IOException e) {
			throw new UncheckedIOException("I/O exception while reading VisB HTML template resource - this should never happen", e);
		}
	}

	@FXML
	public void showVisBItemsAndEvents() {
		injector.getInstance(VisBDebugStage.class).show();
	}

	@FXML
	public void manageDefaultVisualisation() {
		final DefaultPathDialog defaultPathDialog = defaultPathDialogProvider.get();
		defaultPathDialog.initOwner(this);
		defaultPathDialog.initStrings(
			"visb.defaultVisualisation.header",
			"visb.defaultVisualisation.text",
			"visb.noDefaultVisualisation.text",
			"visb.defaultVisualisation.load",
			"visb.defaultVisualisation.set",
			"visb.defaultVisualisation.reset"
		);
		final Path loadedPathRelative = currentProject.getLocation().relativize(visBController.getVisBPath());
		final Machine currentMachine = currentProject.getCurrentMachine();
		defaultPathDialog.initPaths(loadedPathRelative, currentMachine.getVisBVisualisation());
		defaultPathDialog.showAndWait().ifPresent(action -> {
			switch (action) {
				case LOAD_DEFAULT:
					this.loadVisBFileFromMachine(currentMachine, currentTrace.getStateSpace());
					break;

				case SET_CURRENT_AS_DEFAULT:
					currentMachine.setVisBVisualisation(loadedPathRelative);
					break;

				case UNSET_DEFAULT:
					currentMachine.setVisBVisualisation(null);
					break;

				default:
					throw new AssertionError("Unhandled action: " + action);
			}
		});
	}

	public void saveHTMLExport(VisBExportKind kind) {
		if (currentTrace.get() != null) {
			final FileChooser fileChooser = new FileChooser();
			FileChooser.ExtensionFilter htmlFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.html", "html");
			fileChooser.getExtensionFilters().setAll(htmlFilter);
			fileChooser.setTitle(i18n.translate("common.fileChooser.save.title"));

			Path path = fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, this);
			saveHTMLExportWithPath(currentTrace.get(), kind, path);
		}
	}

	public void saveHTMLExportWithPath(Trace trace, VisBExportKind kind, Path path) {
		if(path != null) {
			if(kind == VisBExportKind.CURRENT_STATE) {
				ExportVisBForCurrentStateCommand cmd = new ExportVisBForCurrentStateCommand(path.toAbsolutePath().toString());
				trace.getStateSpace().execute(cmd);
			} else if(kind == VisBExportKind.CURRENT_TRACE) {
				List<String> transIDs = trace.getTransitionList().stream()
						.map(Transition::getId)
						.collect(Collectors.toList());
				ExportVisBForHistoryCommand cmd = new ExportVisBForHistoryCommand(transIDs, path.toAbsolutePath().toString());
				trace.getStateSpace().execute(cmd);
			}
		}
	}

}


