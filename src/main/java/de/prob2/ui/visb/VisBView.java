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
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.ExportVisBForHistoryCommand;
import de.prob.animator.command.ExportVisBHtmlForStates;
import de.prob.animator.command.GetVisBAttributeValuesCommand;
import de.prob.animator.command.ReadVisBPathFromDefinitionsCommand;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBExportOptions;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob.animator.domainobjects.VisBSVGObject;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpSystem;
import de.prob2.ui.helpsystem.HelpSystemStage;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.internal.executor.FxThreadExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.visb.help.UserManualStage;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
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
				if (visBController.isExecutingEvent()) {
					LOGGER.debug("Ignoring click because another event is currently being executed");
					return;
				}

				visBController.executeEvent(id, pageX, pageY, shiftKey, metaKey).exceptionally(exc -> {
					stageManager.showUnhandledExceptionAlert(exc, getScene().getWindow());
					return null;
				});
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
	
	private enum LoadingStatus {
		NONE_LOADED,
		LOADING,
		LOADED,
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(VisBView.class);
	private final Injector injector;
	private final I18n i18n;
	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final CliTaskExecutor cliExecutor;
	private final FxThreadExecutor fxExecutor;
	private final DisablePropertyController disablePropertyController;
	private final FileChooserManager fileChooserManager;
	private final VisBController visBController;

	private final VisBConnector visBConnector;

	private final ObjectProperty<VisBView.LoadingStatus> loadingStatus;
	private final BooleanProperty updatingVisualisation;

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
	private HBox inProgressBox;
	@FXML
	private Label inProgressLabel;
	@FXML
	private VBox placeholder;
	@FXML
	private ProgressIndicator loadingProgress;
	@FXML
	private Label placeholderLabel;
	@FXML
	private Button initButton;
	@FXML
	private StackPane mainPane;

	private WebView webView;

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
		FxThreadExecutor fxExecutor,
		DisablePropertyController disablePropertyController,
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
		this.fxExecutor = fxExecutor;
		this.disablePropertyController = disablePropertyController;
		this.fileChooserManager = fileChooserManager;
		this.visBController = visBController;

		this.visBConnector = new VisBConnector();

		this.loadingStatus = new SimpleObjectProperty<>(this, "loadingStatus", VisBView.LoadingStatus.NONE_LOADED);
		this.updatingVisualisation = new SimpleBooleanProperty(this, "updatingVisualisation", false);

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
				loadDefaultItem.setDisable(true);
				loadFromDefinitionsItem.setDisable(true);
				setCurrentAsDefaultItem.setDisable(true);
				unsetDefaultItem.setDisable(true);
			} else {
				loadDefaultItem.disableProperty().bind(to.visBVisualizationProperty().isNull()
					.or(visBController.relativeVisBPathProperty().isEqualTo(to.visBVisualizationProperty())));
				loadFromDefinitionsItem.disableProperty().bind(visBController.relativeVisBPathProperty().isEqualTo(VisBController.NO_PATH));
				setCurrentAsDefaultItem.disableProperty().bind(visBController.relativeVisBPathProperty().isNull()
					.or(visBController.relativeVisBPathProperty().isEqualTo(to.visBVisualizationProperty())));
				unsetDefaultItem.disableProperty().bind(to.visBVisualizationProperty().isNull());
			}
		};

		ChangeListener<? super VisBVisualisation> visBListener = (o, from, to) -> {
			loadingStatus.set(to == null ? VisBView.LoadingStatus.NONE_LOADED : VisBView.LoadingStatus.LOADING);
			visBController.getAttributeValues().clear();
			this.updateView(loadingStatus.get(), currentTrace.get());

			if (to != null) {
				this.loadVisualisationIntoWebView(to);
			}
		};

		ChangeListener<Trace> traceListener = (o, from, to) -> {
			if (from == null || to == null || !from.getStateSpace().equals(to.getStateSpace())) {
				visBController.closeVisualisation();
				if (to != null) {
					loadVisBFileFromMachine(currentProject.getCurrentMachine(), to.getStateSpace());
				}
			}

			this.updateView(loadingStatus.get(), to);
		};

		// Load VisB file from machine, when window is opened and set listener on the current machine
		this.currentProject.currentMachineProperty().addListener(machineListener);
		this.visBController.visBVisualisationProperty().addListener(visBListener);
		this.currentTrace.addListener(traceListener);
		this.loadingStatus.addListener((o, from, to) -> this.updateView(to, currentTrace.get()));

		machineListener.changed(null, null, currentProject.getCurrentMachine());
		traceListener.changed(null, null, currentTrace.get());

		visBController.executingEventProperty().addListener(o -> this.updateInProgress());
		updatingVisualisation.addListener(o -> this.updateInProgress());

		initButton.disableProperty().bind(disablePropertyController.disableProperty());

		this.reloadVisualisationButton.disableProperty().bind(visBController.absoluteVisBPathProperty().isNull());

		exportHistoryItem.setOnAction(e -> performHtmlExport(false));
		exportCurrentStateItem.setOnAction(e -> performHtmlExport(true));

		Platform.runLater(() -> {
			// WebView can only be constructed on the JavaFX application thread,
			// but VisBView.initialize generally runs on a background thread during UI startup,
			// so this part needs to be explicitly moved to the JavaFX application thread.
			this.webView = new WebView();
			this.mainPane.getChildren().add(webView);
			LOGGER.debug("JavaFX WebView user agent: {}", this.webView.getEngine().getUserAgent());
			this.webView.getEngine().setOnAlert(event -> showJavascriptAlert(event.getData()));
			this.webView.getEngine().setOnError(this::treatJavascriptError);
			visBListener.changed(null, null, visBController.getVisBVisualisation());
			Trace trace = currentTrace.get();
			traceListener.changed(null, trace, trace);

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

	private void loadFromDefinitions(StateSpace stateSpace) {
		loadingStatus.set(VisBView.LoadingStatus.LOADING);
		cliExecutor.submit(() -> getPathFromDefinitions(stateSpace)).thenComposeAsync(path -> {
			if (path == null) {
				loadingStatus.set(VisBView.LoadingStatus.NONE_LOADED);
				return CompletableFuture.completedFuture(null);
			} else {
				return visBController.loadFromAbsolutePath(path);
			}
		}, fxExecutor).exceptionally(exc -> {
			Platform.runLater(() -> this.showVisualisationLoadError(exc));
			return null;
		});
	}

	public void loadFromAbsolutePath(Path path) {
		loadingStatus.set(VisBView.LoadingStatus.LOADING);
		try {
			visBController.loadFromAbsolutePath(path).exceptionally(exc -> {
				Platform.runLater(() -> this.showVisualisationLoadError(exc));
				return null;
			});
		} catch (RuntimeException exc) {
			this.showVisualisationLoadError(exc);
		}
	}

	public void loadFromRelativePath(Path path) {
		loadingStatus.set(VisBView.LoadingStatus.LOADING);
		try {
			visBController.loadFromRelativePath(path).exceptionally(exc -> {
				Platform.runLater(() -> this.showVisualisationLoadError(exc));
				return null;
			});
		} catch (RuntimeException exc) {
			this.showVisualisationLoadError(exc);
		}
	}

	public void loadVisBFileFromMachine(final Machine machine, final StateSpace stateSpace) {
		Path visBVisualisation = machine.getVisBVisualisation();
		if (visBVisualisation != null) {
			this.loadFromRelativePath(visBVisualisation);
		} else {
			this.loadFromDefinitions(stateSpace);
		}
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

	private void loadVisualisationIntoWebView(VisBVisualisation visBVisualisation) {
		final Path path = visBVisualisation.getSvgPath();
		final String baseUrl;
		if (path.equals(VisBController.NO_PATH)) {
			baseUrl = "";
		} else {
			baseUrl = path.getParent().toUri().toString();
		}
		LOGGER.trace("Generating VisB HTML code...");
		String htmlFile = generateHTMLFileWithSVG(visBVisualisation.getSvgContent(), baseUrl);
		LOGGER.debug("Loading generated VisB HTML code into WebView...");
		this.webView.getEngine().loadContent(htmlFile);

		this.runWhenHtmlLoaded(() -> {
			JSObject window = this.getJSWindow();

			// WebView doesn't have a proper API for detecting e. g. JavaScript syntax errors,
			// so as a workaround our JavaScript code sets a marker variable that we can check here.
			// If the marker variable doesn't have the expected value,
			// assume that the JavaScript code didn't load properly,
			// and disable VisB to avoid exceptions from code that tries to call the (nonexistant) JavaScript functions.
			Object loadedMarker = window.getMember("visBJavaScriptLoaded");
			if (!"VisB JavaScript loaded".equals(loadedMarker)) {
				LOGGER.error("VisB JavaScript failed to load (marker variable has incorrect value '{}')", loadedMarker);
				stageManager.makeAlert(Alert.AlertType.ERROR, "", "visb.exception.javaScriptFailedToLoad").show();
				visBController.hideVisualisation();
				return;
			}

			LOGGER.trace("Setting up VisB dynamic SVG objects...");
			updateDynamicSVGObjects(visBVisualisation);
			LOGGER.trace("Setting up VisB click events...");
			for (VisBEvent event : visBVisualisation.getEvents()) {
				window.call("addClickEvent", visBConnector, event.getId(), event.getEvent(), event.getHovers().toArray(new VisBHover[0]));
			}

			LOGGER.debug("VisB visualisation is fully loaded");
			loadingStatus.set(VisBView.LoadingStatus.LOADED);
		});
	}

	private void treatJavascriptError(WebErrorEvent event) {
		LOGGER.info("JavaScript error: {}", event.getMessage(), event.getException());
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

	private void showPlaceholder(String placeholderLabelText) {
		if (this.webView != null) {
			// The web view might not be loaded yet...
			// (see the Platform.runLater block in initialize)
			this.webView.setVisible(false);
		}

		this.placeholder.setVisible(true);
		this.placeholderLabel.setText(placeholderLabelText);
		this.loadingProgress.setVisible(false);
		this.initButton.setVisible(false);
	}

	private void updateView(VisBView.LoadingStatus status, Trace trace) {
		if (trace == null) {
			this.showPlaceholder(i18n.translate("common.noModelLoaded"));
		} else if (status == VisBView.LoadingStatus.LOADING) {
			this.showPlaceholder(i18n.translate("visb.placeholder.loadingVisualisation"));
			this.loadingProgress.setVisible(true);
		} else if (status == VisBView.LoadingStatus.NONE_LOADED) {
			this.showPlaceholder(i18n.translate("visb.placeholder.noVisualisation"));
		} else if (!trace.getCurrentState().isInitialised()) {
			this.initButton.setText(i18n.translate(trace.getCurrentState().isConstantsSetUp() ? "visb.placeholder.button.initialise" : "visb.placeholder.button.setupConstants"));
			
			if (trace.getCurrentState().getOutTransitions().size() == 1) {
				this.showPlaceholder(i18n.translate("visb.placeholder.notInitialised.deterministic"));
				this.initButton.setVisible(true);
			} else {
				this.showPlaceholder(i18n.translate("visb.placeholder.notInitialised.nonDeterministic"));
			}
		} else {
			assert status == VisBView.LoadingStatus.LOADED;
			this.updateVisualisation(trace.getCurrentState());
		}
	}

	private void updateVisualisation(State state) {
		LOGGER.debug("Reloading VisB visualisation...");

		updatingVisualisation.set(true);
		cliExecutor.submit(() -> {
			var getAttributesCmd = new GetVisBAttributeValuesCommand(state);
			state.getStateSpace().execute(getAttributesCmd);
			return getAttributesCmd.getValues();
		}).whenCompleteAsync((res, exc) -> {
			if (exc == null) {
				LOGGER.trace("Applying VisB attribute values...");
				visBController.getAttributeValues().putAll(res);
				LOGGER.trace("Done applying VisB attribute values");

				try {
					this.resetMessages();
				} catch (JSException e) {
					alert(e, "visb.exception.header", "visb.controller.alert.visualisation.file");
				}

				this.placeholder.setVisible(false);
				this.initButton.setVisible(false);
				this.webView.setVisible(true);
			} else {
				// TODO Perhaps the visualisation should only be hidden temporarily and shown again after the next state change?
				visBController.hideVisualisation();
				alert(exc, "visb.controller.alert.eval.formulas.header", "visb.exception.visb.file.error.header");
			}

			LOGGER.debug("VisB visualisation reloaded");
		}, fxExecutor).whenCompleteAsync((res, exc) -> updatingVisualisation.set(false), fxExecutor);
	}

	/**
	 * Run the given {@link Runnable} once the {@link WebView} has successfully finished loading.
	 * You should use {@link #runWhenVisualisationLoaded(Runnable)} instead in most cases,
	 * which also waits for other initialisation code to finish
	 * (e. g. creation of dynamic SVG objects).
	 *
	 * @param runnable the code to run once the {@link WebView} has finished loading
	 */
	private void runWhenHtmlLoaded(final Runnable runnable) {
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

	/**
	 * Run the given {@link Runnable} once the visualisation has been loaded successfully.
	 * If the visualisation is already fully loaded, the {@link Runnable} is executed immediately.
	 * If the visualisation fails to load, the {@link Runnable} is never executed.
	 *
	 * @param runnable the code to run once the visualisation has finished loading
	 */
	private void runWhenVisualisationLoaded(Runnable runnable) {
		if (loadingStatus.get() == VisBView.LoadingStatus.LOADED) {
			runnable.run();
		} else {
			loadingStatus.addListener(new ChangeListener<>() {
				@Override
				public void changed(ObservableValue<? extends VisBView.LoadingStatus> observable, VisBView.LoadingStatus from, VisBView.LoadingStatus to) {
					if (to == VisBView.LoadingStatus.LOADED) {
						observable.removeListener(this);
						runnable.run();
					}
				}
			});
		}
	}

	public void changeAttribute(final String id, final String attribute, final String value) {
		if (loadingStatus.get() != VisBView.LoadingStatus.LOADED) {
			throw new IllegalStateException("Tried to call changeAttribute before VisB visualisation has been fully loaded");
		}

		this.getJSWindow().call("changeAttribute", id, attribute, value);
	}

	public void changeAttributeIfLoaded(String id, String attribute, String value) {
		if (loadingStatus.get() == VisBView.LoadingStatus.LOADED) {
			this.changeAttribute(id, attribute, value);
		}
	}

	public void resetMessages() {
		if (loadingStatus.get() != VisBView.LoadingStatus.LOADED) {
			throw new IllegalStateException("Tried to call resetMessages before VisB visualisation has been fully loaded");
		}

		this.getJSWindow().call("resetMessages");
	}

	@FXML
	private void doInitialisation() {
		visBController.executeBeforeInitialisation().whenComplete((res, exc) -> {
			if (exc != null) {
				LOGGER.error("Exception while executing initialisation from VisB view", exc);
				stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
			}
		});
	}

	private void showInProgress(String text) {
		inProgressLabel.setText(text);
		inProgressBox.setManaged(true);
		inProgressBox.setVisible(true);
	}

	private void hideInProgress() {
		inProgressBox.setManaged(false);
		inProgressBox.setVisible(false);
	}

	private void updateInProgress() {
		if (visBController.isExecutingEvent()) {
			showInProgress(i18n.translate("visb.inProgress.executingEvent"));
		} else if (updatingVisualisation.get()) {
			showInProgress(i18n.translate("visb.inProgress.updatingVisualisation"));
		} else {
			hideInProgress();
		}
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
			this.loadFromAbsolutePath(path);
		}
	}

	/**
	 * This method throws an ProB2-UI ExceptionAlert
	 */
	private void alert(Throwable ex, String header, String body, Object... params){
		final Alert alert = this.stageManager.makeExceptionAlert(ex, header, body, params);
		alert.initOwner(this.getScene().getWindow());
		alert.show();
	}

	private void showVisualisationLoadError(Throwable exc) {
		LOGGER.error("Error while (re)loading VisB file", exc);
		loadingStatus.set(VisBView.LoadingStatus.NONE_LOADED);
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
			loadingStatus.set(VisBView.LoadingStatus.LOADING);
			visBController.reloadVisualisation().exceptionally(exc -> {
				Platform.runLater(() -> this.showVisualisationLoadError(exc));
				return null;
			});
		} catch (RuntimeException exc) {
			this.showVisualisationLoadError(exc);
		}
	}

	@FXML
	public void closeVisualisation() {
		visBController.closeVisualisation();
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
		this.loadFromDefinitions(currentTrace.getStateSpace());
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
					new ExportVisBHtmlForStates(trace.getCurrentState(), VisBExportOptions.DEFAULT.withShowVariables(true), path)
						: new ExportVisBForHistoryCommand(trace, path));
				return null;
			}
		};
		task.setOnRunning(r -> showInProgress(i18n.translate("visb.inProgress.htmlExport")));
		task.setOnSucceeded(s -> hideInProgress());
		task.setOnFailed(f -> hideInProgress());
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


