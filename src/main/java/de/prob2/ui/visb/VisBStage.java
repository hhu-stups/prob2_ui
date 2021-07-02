package de.prob2.ui.visb;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Injector;

import de.prob.animator.command.ExportVisBForCurrentStateCommand;
import de.prob.animator.command.ExportVisBForHistoryCommand;
import de.prob.animator.command.ReadVisBPathFromDefinitionsCommand;
import de.prob.statespace.Transition;
import de.prob2.ui.Main;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.animation.tracereplay.TraceSaver;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.MustacheTemplateManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.DefaultPathHandler;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.TraceSelectionView;
import de.prob2.ui.simulation.SimulatorStage;
import de.prob2.ui.visb.help.UserManualStage;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
	private final ResourceBundle bundle;
	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final DefaultPathHandler defaultPathHandler;
	private final VisBController visBController;
	private boolean connectorSet = false;
	private final FileChooserManager fileChooserManager;

	@FXML
	private MenuBar visbMenuBar;
	@FXML
	private Button loadVisualisationButton;
	@FXML
	private Button reloadVisualisationButton;
	@FXML
	private Button manageDefaultVisualisationButton;
	@FXML
	private Button openTraceSelectionButton;
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
	private MenuItem saveTraceItem;
	@FXML
	private MenuItem exportHistoryItem;
	@FXML
	private MenuItem exportCurrentStateItem;
	@FXML
	private Label information;
	@FXML
	private VBox placeholder;

	/**
	 * The public constructor of this class is injected with the ProB2-UI injector.
	 * @param injector ProB2-UI injector
	 * @param stageManager ProB2-UI stageManager
	 * @param currentProject ProB2-UI currentProject
	 */
	@Inject
	public VisBStage(final Injector injector, final StageManager stageManager, final CurrentProject currentProject,
					 final CurrentTrace currentTrace, final ResourceBundle bundle, final FileChooserManager fileChooserManager,
					 final DefaultPathHandler defaultPathHandler, final VisBController visBController) {
		super();
		this.injector = injector;
		this.bundle = bundle;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.fileChooserManager = fileChooserManager;
		this.defaultPathHandler = defaultPathHandler;
		this.visBController = visBController;
		defaultPathHandler.setVisBStage(this);
		this.stageManager.loadFXML(this, "visb_plugin_stage.fxml");
	}

	/**
	 * With this method a visible stage with an empty WebView and an empty ListView is initialised.
	 */
	@FXML
	public void initialize(){
		this.stageManager.setMacMenuBar(this, visbMenuBar);
		this.helpMenu_userManual.setOnAction(e -> injector.getInstance(UserManualStage.class).show());
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
		this.titleProperty().bind(Bindings.createStringBinding(() -> visBController.getVisBPath() == null ? bundle.getString("visb.title") : String.format(bundle.getString("visb.currentVisualisation"), currentProject.getLocation().relativize(visBController.getVisBPath()).toString()), visBController.visBPathProperty()));

		this.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
			visBController.setVisBPath(null);
			visBController.closeCurrentVisualisation();
		});
		//Load VisB file from machine, when window is opened and set listener on the current machine
		this.addEventFilter(WindowEvent.WINDOW_SHOWING, event -> {
			updateUIOnMachine(currentProject.getCurrentMachine());
			loadVisBFileFromMachine(currentProject.getCurrentMachine());
		});

		this.reloadVisualisationButton.disableProperty().bind(visBController.visBPathProperty().isNull());

		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			openTraceSelectionButton.disableProperty().unbind();
			manageDefaultVisualisationButton.disableProperty().unbind();
			updateUIOnMachine(to);
			loadVisBFileFromMachine(to);
		});

		this.currentTrace.stateSpaceProperty().addListener((observable, from, to) -> {
			if (to != null && (from == null || !from.getLoadedMachine().equals(to.getLoadedMachine())) && visBController.getVisBPath() != null) {
				this.setupMachineVisBFile();
			}
		});

		saveTraceItem.setOnAction(e -> injector.getInstance(TraceSaver.class).saveTrace(this.getScene().getWindow(), TraceReplayErrorAlert.Trigger.TRIGGER_VISB));
		exportHistoryItem.setOnAction(e -> saveHTMLExport(VisBExportKind.CURRENT_TRACE));
		exportCurrentStateItem.setOnAction(e -> saveHTMLExport(VisBExportKind.CURRENT_STATE));

		injector.getInstance(VisBDebugStage.class).initOwner(this);
	}

	private void updateUIOnMachine(Machine machine) {
		final BooleanBinding openTraceDefaultDisableProperty = currentProject.currentMachineProperty().isNull();
		manageDefaultVisualisationButton.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(visBController.visBPathProperty().isNull()));
		if(machine != null) {
			openTraceSelectionButton.disableProperty().bind(machine.tracesProperty().emptyProperty());
		} else {
			openTraceSelectionButton.disableProperty().bind(openTraceDefaultDisableProperty);
		}
	}

	public void loadVisBFileFromMachine(Machine machine) {
		clear();
		visBController.setVisBPath(null);
		if(machine != null) {

			Path pathFromDefinitions = null;

			if(currentTrace.getStateSpace() != null) {
				ReadVisBPathFromDefinitionsCommand cmd = new ReadVisBPathFromDefinitionsCommand();
				currentTrace.getStateSpace().execute(cmd);
				pathFromDefinitions = cmd.getPath() == null ? null : currentProject.getLocation().resolve(cmd.getPath());
			}

			Path visBVisualisation = machine.getVisBVisualisation();
			visBController.setVisBPath(visBVisualisation == null ? pathFromDefinitions : currentProject.getLocation().resolve(visBVisualisation));
			if(currentTrace.getStateSpace() != null) {
				Platform.runLater(this::setupMachineVisBFile);
			}
		}
	}

	private void setupMachineVisBFile() {
		final Path path = visBController.getVisBPath();
		if (path != null) {
			visBController.setupVisualisation(path);
		}
	}

	private void sendCloseRequest(){
		this.fireEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST));
	}

	/**
	 * After loading the svgFile and preparing it in the {@link VisBController} the WebView is initialised.
	 * @param svgContent the image/ svg, that should to be loaded into the context of the WebView
	 */
	void initialiseWebView(List<VisBOnClickMustacheItem> clickEvents, String svgContent) {
		if (svgContent != null) {
			this.placeholder.setVisible(false);
			this.webView.setVisible(true);
			String jqueryLink = Main.class.getResource("jquery.js").toExternalForm();
			String htmlFile = generateHTMLFileWithSVG(jqueryLink, clickEvents, svgContent);
			this.webView.getEngine().loadContent(htmlFile);
			addVisBConnector();
		}

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

	private void addVisBConnector() {
		if(connectorSet){
			LOGGER.debug("Connector is already set, no action needed.");
		} else {
			LOGGER.debug("VisBConnector is set into globals.");
			connectorSet = true;
			this.webView.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
				if (newState == Worker.State.SUCCEEDED) {
					JSObject jsobj = (JSObject) webView.getEngine().executeScript("window");
					jsobj.setMember("visBConnector", injector.getInstance(VisBConnector.class));
					webView.getEngine().executeScript("activateClickEvents();");
				}
			});
			this.webView.getEngine().setOnAlert(event -> showJavascriptAlert(event.getData()));
			this.webView.getEngine().setOnError(this::treatJavascriptError); // check if we get errors
			// Note: only called while loading page: https://stackoverflow.com/questions/31391736/for-javafxs-webengine-how-do-i-get-notified-of-javascript-errors
			// engine.setConfirmHandler(message -> showConfirm(message));
			LOGGER.debug("VisBConnector was set into globals.");
		}
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
	 * This method runs the jQuery script in the WebView.
	 * @param jQuery script to be run
	 */
	public void runScript(String jQuery) {
		if(webView.getEngine().getLoadWorker().getState().equals(Worker.State.RUNNING)){
			// execute JQuery script once page fully loaded
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
						LOGGER.debug("runScript: "+jQuery+"\n-----");
						webView.getEngine().executeScript(jQuery);
					}
				}
			);
			LOGGER.debug("registered runScript as Listener");
		} else {
			LOGGER.debug("runScript directly: "+jQuery+"\n-----");
			this.webView.getEngine().executeScript(jQuery);
		}
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
		fileChooser.setTitle(bundle.getString("visb.stage.filechooser.title"));
		fileChooser.getExtensionFilters().addAll(
				fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.visBVisualisation", "json")
		);
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		if(path != null) {
			clear();
			visBController.setVisBPath(path);
			visBController.setupVisualisation(path);
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
		fileChooser.setTitle(bundle.getString("visb.stage.filechooser.export.title"));
		fileChooser.getExtensionFilters().addAll(
				fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.png", "png")
		);
		Path path = fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		if(path != null) {
			File file = path.toFile();
			WritableImage snapshot = webView.snapshot(new SnapshotParameters(), null);
			RenderedImage renderedImage = SwingFXUtils.fromFXImage(snapshot, null);
			try {
				ImageIO.write(renderedImage, "png", file);
			} catch (IOException e){
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
		visBController.closeCurrentVisualisation();
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
	private void openTraceSelection() {
		TraceSelectionView traceSelectionView = injector.getInstance(TraceSelectionView.class);
		traceSelectionView.show();
		traceSelectionView.toFront();
	}

	@FXML
	private void openSimulation() {
		SimulatorStage simulatorStage = injector.getInstance(SimulatorStage.class);
		simulatorStage.show();
		simulatorStage.toFront();
	}

	private String generateHTMLFileWithSVG(String jqueryLink, List<VisBOnClickMustacheItem> clickEvents, String svgContent) {
		InputStream inputStream = this.getClass().getResourceAsStream("visb_html_view.mustache");
		MustacheTemplateManager templateManager = new MustacheTemplateManager(inputStream, "visb_html_view");
		templateManager.put("jqueryLink", jqueryLink);
		templateManager.put("clickEvents", clickEvents);
		templateManager.put("svgContent", svgContent);
		return templateManager.apply();
	}

	@FXML
	public void showVisBItemsAndEvents() {
		injector.getInstance(VisBDebugStage.class).show();
	}

	@FXML
	public void manageDefaultVisualisation() {
		defaultPathHandler.manageDefault(DefaultPathHandler.DefaultKind.VISB);
	}

	public ObjectProperty<Path> getVisBPath() {
		return visBController.visBPathProperty();
	}

	public void saveHTMLExport(VisBExportKind kind) {
		if (currentTrace.get() != null) {
			final FileChooser fileChooser = new FileChooser();
			FileChooser.ExtensionFilter htmlFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.html", "html");
			fileChooser.getExtensionFilters().setAll(htmlFilter);
			fileChooser.setTitle(bundle.getString("common.fileChooser.save.title"));

			Path path = fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, this);
			if(path != null) {
				if(kind == VisBExportKind.CURRENT_STATE) {
					ExportVisBForCurrentStateCommand cmd = new ExportVisBForCurrentStateCommand(path.toAbsolutePath().toString());
					currentTrace.getStateSpace().execute(cmd);
				} else if(kind == VisBExportKind.CURRENT_TRACE) {
					List<String> transIDs = currentTrace.get().getTransitionList().stream()
							.map(Transition::getId)
							.collect(Collectors.toList());
					ExportVisBForHistoryCommand cmd = new ExportVisBForHistoryCommand(transIDs, path.toAbsolutePath().toString());
					currentTrace.getStateSpace().execute(cmd);
				}
			}
		}
	}

}


