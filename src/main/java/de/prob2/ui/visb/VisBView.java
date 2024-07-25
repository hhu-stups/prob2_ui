package de.prob2.ui.visb;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.ExportVisBForHistoryCommand;
import de.prob.animator.command.ExportVisBHtmlForStates;
import de.prob.animator.command.ReadVisBPathFromDefinitionsCommand;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob.animator.domainobjects.VisBSVGObject;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpSystem;
import de.prob2.ui.helpsystem.HelpSystemStage;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.visb.help.UserManualStage;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds the main user interface and interacts with the {@link VisBController} class.
 */
@Singleton
@FXMLInjected
public final class VisBView extends BorderPane {
	/**
	 * Contains Java methods that are meant to be called from the VisB JavaScript code running inside the {@link #webView}.
	 * This class should only be used inside {@link VisBView}, but needs to be {@code public} so that it can be called from JavaScript.
	 */
	public final class VisBConnector {
		/**
		 * Whenever a svg item, that has an event in the JSON / VisB file is clicked, this method redirects the click towards the {@link VisBController}
		 * @param id of the svg item, that is clicked
		 */
		public void click(String id, int pageX, int pageY, boolean shiftKey, boolean metaKey) {
			// probably pageX,pageY is the one to use as they do not change when scrolling and are relative to the SVG
			LOGGER.debug("SVG Element with ID {} was clicked at page position {},{} with shift {} cmd/meta {}", id, pageX, pageY, shiftKey, metaKey); // 1=left, 2=middle, 3=right
			try {
				visBController.executeEvent(id, pageX, pageY, shiftKey, metaKey);
			} catch (Throwable t) {
				// It seems that Java exceptions are completely ignored if they are thrown back to JavaScript,
				// so log them manually here.
				LOGGER.error("Uncaught exception in VisBConnector.click called by JavaScript", t);
				Platform.runLater(() -> {
					Alert alert = stageManager.makeExceptionAlert(t, "visb.exception.header", "visb.exception.clickEvent");
					alert.initOwner(getScene().getWindow());
					alert.showAndWait();
				});
			}
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(VisBView.class);
	private final Injector injector;
	private final I18n i18n;
	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final CliTaskExecutor cliExecutor;
	private final FileChooserManager fileChooserManager;
	private final VisBController visBController;

	private final VisBConnector visBConnector;

	@FXML
	private MenuBar visbMenuBar;
	@FXML
	private Button loadVisualisationButton;
	@FXML
	private Button reloadVisualisationButton;
	@FXML
	private MenuItem loadDefaultItem;
	@FXML
	private MenuItem loadFromDefinitionsItem;
	@FXML
	private MenuItem setCurrentAsDefaultItem;
	@FXML
	private MenuItem unsetDefaultItem;
	@FXML
	private Button openSimulationButton;
	@FXML
	private StackPane zoomingPane;

	private WebView webView;

	@FXML
	private MenuButton saveTraceButton;
	@FXML
	private MenuItem saveTraceAndAddTestsItem;
	@FXML
	private MenuItem saveTraceAndRecordTestsItem;
	@FXML
	private MenuItem exportHistoryItem;
	@FXML
	private MenuItem exportCurrentStateItem;
	@FXML
	private HBox exportInProgress;
	@FXML
	private VBox placeholder;
	@FXML
	private Label placeholderLabel;

	/**
	 * The public constructor of this class is injected with the ProB2-UI injector.
	 * @param injector ProB2-UI injector
	 * @param stageManager ProB2-UI stageManager
	 * @param currentProject ProB2-UI currentProject
	 */
	@Inject
	public VisBView(
		Injector injector,
		I18n i18n,
		StageManager stageManager,
		CurrentProject currentProject,
		CurrentTrace currentTrace,
		CliTaskExecutor cliExecutor,
		FileChooserManager fileChooserManager,
		VisBController visBController
	) {
		super();
		this.injector = injector;
		this.i18n = i18n;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.cliExecutor = cliExecutor;
		this.fileChooserManager = fileChooserManager;
		this.visBController = visBController;

		this.visBConnector = new VisBConnector();

		this.stageManager.loadFXML(this, "visb_view.fxml");
	}

	/**
	 * With this method a visible stage with an empty WebView and an empty ListView is initialised.
	 */
	@FXML
	public void initialize(){
		this.loadVisualisationButton.disableProperty().bind(currentProject.currentMachineProperty().isNull());
		this.saveTraceButton.disableProperty().bind(visBController.absoluteVisBPathProperty().isNull());

		ChangeListener<? super Machine> machineListener = (observable, from, to) -> {
			loadDefaultItem.disableProperty().unbind();
			loadFromDefinitionsItem.disableProperty().unbind();
			setCurrentAsDefaultItem.disableProperty().unbind();
			unsetDefaultItem.disableProperty().unbind();
			if (to == null) {
				placeholderLabel.setText(i18n.translate("common.noModelLoaded"));
				loadDefaultItem.setDisable(true);
				loadFromDefinitionsItem.setDisable(true);
				setCurrentAsDefaultItem.setDisable(true);
				unsetDefaultItem.setDisable(true);
			} else {
				placeholderLabel.setText(i18n.translate("visb.placeholder.text"));
				loadDefaultItem.disableProperty().bind(to.visBVisualizationProperty().isNull()
					.or(visBController.relativeVisBPathProperty().isEqualTo(to.visBVisualizationProperty())));
				loadFromDefinitionsItem.disableProperty().bind(visBController.relativeVisBPathProperty().isEqualTo(VisBController.NO_PATH));
				setCurrentAsDefaultItem.disableProperty().bind(visBController.relativeVisBPathProperty().isNull()
					.or(visBController.relativeVisBPathProperty().isEqualTo(to.visBVisualizationProperty())));
				unsetDefaultItem.disableProperty().bind(to.visBVisualizationProperty().isNull());
			}
		};

		ChangeListener<? super VisBVisualisation> visBListener = (o, from, to) -> {
			if (to == null) {
				this.clear();
			} else {
				this.loadSvgFile(to);
				this.runWhenLoaded(() -> {
					final JSObject window = this.getJSWindow();

					// WebView doesn't have a proper API for detecting e. g. JavaScript syntax errors,
					// so as a workaround our JavaScript code sets a marker variable that we can check here.
					// If the marker variable doesn't have the expected value,
					// assume that the JavaScript code didn't load properly,
					// and disable VisB to avoid exceptions from code that tries to call the (nonexistant) JavaScript functions.
					Object loadedMarker = window.getMember("visBJavaScriptLoaded");
					if (!"VisB JavaScript loaded".equals(loadedMarker)) {
						LOGGER.error("VisB JavaScript failed to load (marker variable has incorrect value '{}')", loadedMarker);
						this.clear();
						stageManager.makeAlert(Alert.AlertType.ERROR, "", "visb.exception.javaScriptFailedToLoad").show();
						return;
					}

					updateDynamicSVGObjects(to);
					for (final VisBEvent event : to.getEvents()) {
						window.call("addClickEvent", visBConnector, event.getId(), event.getEvent(), event.getHovers().toArray(new VisBHover[0]));
					}
				});
			}
		};

		ChangeListener<? super StateSpace> stateSpaceListener = (o, from, to) -> loadVisBFileFromMachine(currentProject.getCurrentMachine(), to);

		// Load VisB file from machine, when window is opened and set listener on the current machine
		this.currentProject.currentMachineProperty().addListener(machineListener);
		this.visBController.visBVisualisationProperty().addListener(visBListener);
		this.currentTrace.stateSpaceProperty().addListener(stateSpaceListener);

		machineListener.changed(null, null, currentProject.getCurrentMachine());

		stateSpaceListener.changed(null, null, currentTrace.getStateSpace());

		this.reloadVisualisationButton.disableProperty().bind(visBController.absoluteVisBPathProperty().isNull());

		exportHistoryItem.setOnAction(e -> performHtmlExport(false));
		exportCurrentStateItem.setOnAction(e -> performHtmlExport(true));

		Platform.runLater(() -> {
			// WebView can only be constructed on the JavaFX application thread,
			// but VisBView.initialize generally runs on a background thread during UI startup,
			// so this part needs to be explicitly moved to the JavaFX application thread.
			this.webView = new WebView();
			this.zoomingPane.getChildren().add(webView);
			LOGGER.debug("JavaFX WebView user agent: {}", this.webView.getEngine().getUserAgent());
			this.webView.getEngine().setOnAlert(event -> showJavascriptAlert(event.getData()));
			this.webView.getEngine().setOnError(this::treatJavascriptError);
			visBListener.changed(null, null, visBController.getVisBVisualisation());
			loadVisBFileFromMachine(currentProject.getCurrentMachine(), currentTrace.getStateSpace());

			// Uncomment to make WebView console errors, warnings, etc. visible in the log.
			// This uses a private undocumented API and requires adding an export for the package javafx.web/com.sun.javafx.webkit
			// (see the corresponding commented out line in build.gradle).
			// com.sun.javafx.webkit.WebConsoleListener.setDefaultListener((wv, message, lineNumber, sourceId) -> LOGGER.info("WebView console: {}:{}: {}", sourceId, lineNumber, message));
		});

		this.visBController.getAttributeValues().addListener((MapChangeListener<VisBItem.VisBItemKey, String>)change -> {
			if (change.wasAdded()) {
				try {
					this.changeAttribute(change.getKey().getId(), change.getKey().getAttribute(), change.getValueAdded());
				} catch (final JSException e) {
					LOGGER.error("JavaScript error while updating VisB attributes", e);
					alert(e, "visb.exception.header","visb.controller.alert.visualisation.file");
				}
			}
		});
	}

	@FXML
	private void openHelpPage() {
		final HelpSystemStage helpSystemStage = injector.getInstance(HelpSystemStage.class);
		final HelpSystem helpSystem = injector.getInstance(HelpSystem.class);
		helpSystem.openHelpForKeyAndAnchor("mainView.visB", null);
		helpSystemStage.show();
		helpSystemStage.toFront();
	}

	@FXML
	private void openUserManual() {
		injector.getInstance(UserManualStage.class).show();
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
		visBController.unload();
		if(machine != null && stateSpace != null) {
			final Path visBVisualisation = machine.getVisBVisualisation();
			if (visBVisualisation != null) {
				try {
					visBController.loadFromRelativePath(visBVisualisation);
				} catch (IOException | RuntimeException exc) {
					this.showVisualisationLoadError(exc);
				}
			} else {
				cliExecutor.execute(() -> {
					Path visBPath = getPathFromDefinitions(stateSpace);
					if (visBPath != null) {
						Platform.runLater(() -> {
							try {
								visBController.loadFromAbsolutePath(visBPath);
							} catch (IOException | RuntimeException exc) {
								this.showVisualisationLoadError(exc);
							}
						});
					}
				});
			}
		}
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

	/**
	 * This creates additional SVG objects as specified in the VisB JSON file (under svg_objects)
	 * or by VISB_SVG_OBJECTS DEFINITIONS in a B machine
	 */
	private void updateDynamicSVGObjects(VisBVisualisation visBVisualisation) {
		for (VisBSVGObject svgObject : visBVisualisation.getSVGObjects()) {
			Map<String, String> attributes = svgObject.getAttributes();
			JSObject object = (JSObject)this.getJSWindow().call("getOrCreateSvgElement", svgObject.getId(), svgObject.getObject());
			for (Map.Entry<String, String> entry : attributes.entrySet()) {
				this.getJSWindow().call("changeCreatedElementAttribute", object, entry.getKey(), entry.getValue());
				// TODO: provide preference to specify which value has precedence: existing one in SVG or this one
				// calling changeElementAttribute always overrides any existing attribute
				// calling changeCreatedElementAttribute only sets attributes for objects created by VisB
			}
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
		LOGGER.debug("JavaScript ERROR: {}", event.getMessage());
		alert(event.getException(), "visb.exception.header", "visb.stage.alert.webview.jsalert", event.getMessage());
	}

	private void showJavascriptAlert(String message) {
		LOGGER.debug("JavaScript ALERT: {}", message);
		final Alert alert = this.stageManager.makeAlert(Alert.AlertType.ERROR, "visb.exception.header", "visb.stage.alert.webview.jsalert", message);
		alert.initOwner(this.getScene().getWindow());
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
		if (webView.getEngine().getLoadWorker().getState().equals(Worker.State.RUNNING)) {
			// execute code once page fully loaded
			// https://stackoverflow.com/questions/12540044/execute-a-task-after-the-webview-is-fully-loaded
			// Use new constructor instead of lambda expression to access change listener with keyword this
			webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<>() {
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
			});
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

	private void showExportInProgress(boolean visible) {
		exportInProgress.setManaged(visible);
		exportInProgress.setVisible(visible);
	}

	/**
	 * On click function for the button and file menu item
	 */
	@FXML
	private void askLoadVisBFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("visb.stage.filechooser.title"));
		fileChooser.getExtensionFilters().addAll(
			fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.visBVisualisation", "json")
		);
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		if (path != null) {
			try {
				visBController.loadFromAbsolutePath(path);
			} catch (IOException | RuntimeException exc) {
				this.showVisualisationLoadError(exc);
			}
		}
	}

	/**
	 * This method throws an ProB2-UI ExceptionAlert
	 */
	private void alert(Throwable ex, String header, String body, Object... params){
		final Alert alert = this.stageManager.makeExceptionAlert(ex, header, body, params);
		alert.initOwner(this.getScene().getWindow());
		alert.showAndWait();
	}

	private void showVisualisationLoadError(Throwable exc) {
		LOGGER.error("Error while (re)loading VisB file", exc);
		alert(exc, "visb.exception.visb.file.error.header", "visb.exception.visb.file.error");
	}

	@FXML
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
		try {
			visBController.reloadVisualisation();
		} catch (IOException | RuntimeException exc) {
			this.showVisualisationLoadError(exc);
		}
	}

	@FXML
	public void closeVisualisation() {
		visBController.unload();
	}

	@FXML
	public void zoomIn() {
		webView.setZoom(webView.getZoom()*1.2);
	}

	@FXML
	public void zoomOut() {
		webView.setZoom(webView.getZoom()/1.2);
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
	private void loadDefault() {
		this.loadVisBFileFromMachine(currentProject.getCurrentMachine(), currentTrace.getStateSpace());
	}

	@FXML
	private void loadFromDefinitions() {
		Path path = getPathFromDefinitions(currentTrace.getStateSpace());
		if (path != null) {
			try {
				visBController.loadFromAbsolutePath(path);
			} catch (IOException | RuntimeException exc) {
				this.showVisualisationLoadError(exc);
			}
		}
	}

	@FXML
	private void setCurrentAsDefault() {
		currentProject.getCurrentMachine().setVisBVisualisation(visBController.getRelativeVisBPath());
	}

	@FXML
	private void unsetDefault() {
		currentProject.getCurrentMachine().setVisBVisualisation(null);
	}

	private void performHtmlExport(final boolean onlyCurrentState) {
		Trace trace = currentTrace.get();
		if (trace == null) {
			return;
		}
		Path path = showHtmlExportFileChooser();
		if (path == null) {
			return;
		}

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				trace.getStateSpace().execute(onlyCurrentState ?
					new ExportVisBHtmlForStates(trace.getCurrentState(), path) : new ExportVisBForHistoryCommand(trace, path));
				return null;
			}
		};
		task.setOnRunning(r -> showExportInProgress(true));
		task.setOnSucceeded(s -> showExportInProgress(false));
		task.setOnFailed(f -> showExportInProgress(false));
		// makes UI responsive, but we can't do anything with the model during export anyway...
		cliExecutor.execute(task);
	}

	private Path showHtmlExportFileChooser() {
		final FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter htmlFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.html", "html");
		fileChooser.getExtensionFilters().setAll(htmlFilter);
		fileChooser.setTitle(i18n.translate("common.fileChooser.save.title"));

		return fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, this.getScene().getWindow());
	}
}


