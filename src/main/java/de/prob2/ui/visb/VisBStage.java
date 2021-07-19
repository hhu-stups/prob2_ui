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
import java.util.ResourceBundle;
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
import de.prob.statespace.StateSpace;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.animation.tracereplay.TraceSaver;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.DefaultPathDialog;
import de.prob2.ui.sharedviews.TraceSelectionView;
import de.prob2.ui.simulation.SimulatorStage;
import de.prob2.ui.visb.help.UserManualStage;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
	private final ResourceBundle bundle;
	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final Provider<DefaultPathDialog> defaultPathDialogProvider;
	private final VisBController visBController;
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
					 final Provider<DefaultPathDialog> defaultPathDialogProvider, final VisBController visBController) {
		super();
		this.injector = injector;
		this.bundle = bundle;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.fileChooserManager = fileChooserManager;
		this.defaultPathDialogProvider = defaultPathDialogProvider;
		this.visBController = visBController;
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

		this.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> visBController.setVisBPath(null));
		//Load VisB file from machine, when window is opened and set listener on the current machine
		this.addEventFilter(WindowEvent.WINDOW_SHOWING, event -> {
			updateUIOnMachine(currentProject.getCurrentMachine());
			loadVisBFileFromMachine(currentProject.getCurrentMachine(), currentTrace.getStateSpace());
		});

		this.reloadVisualisationButton.disableProperty().bind(visBController.visBPathProperty().isNull());

		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			openTraceSelectionButton.disableProperty().unbind();
			manageDefaultVisualisationButton.disableProperty().unbind();
			updateUIOnMachine(to);
		});

		this.currentTrace.stateSpaceProperty().addListener((o, from, to) -> loadVisBFileFromMachine(currentProject.getCurrentMachine(), to));

		saveTraceItem.setOnAction(e -> injector.getInstance(TraceSaver.class).saveTrace(this.getScene().getWindow(), TraceReplayErrorAlert.Trigger.TRIGGER_VISB));
		exportHistoryItem.setOnAction(e -> saveHTMLExport(VisBExportKind.CURRENT_TRACE));
		exportCurrentStateItem.setOnAction(e -> saveHTMLExport(VisBExportKind.CURRENT_STATE));

		this.webView.getEngine().setOnAlert(event -> showJavascriptAlert(event.getData()));
		this.webView.getEngine().setOnError(this::treatJavascriptError);
		this.webView.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED) {
				final JSObject window = this.getJSWindow();
				final VisBConnector visBConnector = injector.getInstance(VisBConnector.class);
				for (final VisBEvent event : this.visBController.getVisBVisualisation().getVisBEvents()) {
					window.call("addClickEvent", visBConnector, event.getId(), event.getEvent(), event.getHovers().toArray(new VisBHover[0]));
				}
			}
		});

		this.visBController.visBVisualisationProperty().addListener((o, from, to) -> {
			if (to == null) {
				this.clear();
			}
		});
		this.visBController.getAttributeValues().addListener((MapChangeListener<VisBItem.VisBItemKey, String>)change -> {
			if (change.wasAdded()) {
				try {
					this.changeAttribute(change.getKey().getId(), change.getKey().getAttribute(), change.getValueAdded());
					updateInfo(bundle.getString("visb.infobox.visualisation.updated"));
				} catch (final JSException e) {
					LOGGER.error("JavaScript error while updating VisB attributes", e);
					alert(e, "visb.exception.header","visb.controller.alert.visualisation.file");
					updateInfo(bundle.getString("visb.infobox.visualisation.error"));
				}
			}
		});

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

	private static Path getPathFromDefinitions(final StateSpace stateSpace) {
		ReadVisBPathFromDefinitionsCommand cmd = new ReadVisBPathFromDefinitionsCommand();
		stateSpace.execute(cmd);
		return cmd.getPath() == null ? null : stateSpace.getModel().getModelFile().toPath().resolveSibling(cmd.getPath());
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
	void initialiseWebView(String svgContent) {
		if (svgContent != null) {
			this.placeholder.setVisible(false);
			this.webView.setVisible(true);
			String htmlFile = generateHTMLFileWithSVG(svgContent);
			this.webView.getEngine().loadContent(htmlFile);
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
		fileChooser.setTitle(bundle.getString("visb.stage.filechooser.title"));
		fileChooser.getExtensionFilters().addAll(
				fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.visBVisualisation", "json")
		);
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		if(path != null) {
			clear();
			visBController.setVisBPath(path);
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

	private String generateHTMLFileWithSVG(String svgContent) {
		final InputStream inputStream = this.getClass().getResourceAsStream("visb_html_view.html");
		if (inputStream == null) {
			throw new AssertionError("VisB HTML template resource not found - this should never happen");
		}
		try (final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			return CharStreams.toString(reader).replace("@SVG_CONTENT@", svgContent);
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


