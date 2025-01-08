package de.prob2.ui.visb;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.ExportVisBForHistoryCommand;
import de.prob.animator.command.ExportVisBHtmlForStates;
import de.prob.animator.command.GetVisBAttributeValuesCommand;
import de.prob.animator.command.ReadVisBPathFromDefinitionsCommand;
import de.prob.animator.domainobjects.VisBClickMetaInfos;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBExportOptions;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob.animator.domainobjects.VisBSVGObject;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.dynamic.plantuml.PlantUmlLocator;
import de.prob2.ui.helpsystem.HelpSystem;
import de.prob2.ui.helpsystem.HelpSystemStage;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.internal.executor.FxThreadExecutor;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.help.UserManualStage;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

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
		public void click(String id, int pageX, int pageY, boolean altKey, boolean ctrlKey, boolean metaKey,
		                  boolean shiftKey, String jsVars) {
			// probably pageX,pageY is the one to use as they do not change when scrolling and are relative to the SVG
			JsonObject visbVarsJson = new Gson().fromJson(jsVars, JsonObject.class);
			Map<String,String> jsVarsMap = new HashMap<>();
			for (String key : visbVarsJson.keySet()) {
				try {
					jsVarsMap.put(key, visbVarsJson.get(key).getAsString());
				} catch (UnsupportedOperationException e) {
					jsVarsMap.put(key, visbVarsJson.get(key).toString());
				}
			}
			LOGGER.debug("SVG Element with ID {} was clicked at page position {},{} with alt {} ctrl {} cmd/meta {} shift {} and JS vars {}",
					id, pageX, pageY, altKey, ctrlKey, metaKey, shiftKey, jsVarsMap); // 1=left, 2=middle, 3=right
			try {
				if (visBController.isExecutingEvent()) {
					LOGGER.debug("Ignoring click because another event is currently being executed");
					return;
				}
				VisBClickMetaInfos metaInfos = new VisBClickMetaInfos(altKey,ctrlKey,metaKey,pageX,pageY,shiftKey,jsVarsMap);
				visBController.executeEvent(id, metaInfos).exceptionally(exc -> {
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
	private final ExternalEditor externalEditor;
	private final PlantUmlLocator plantUmlLocator;
	private final VisBController visBController;

	private final VisBConnector visBConnector;

	private final ObjectProperty<VisBView.LoadingStatus> loadingStatus;
	private final BooleanProperty updatingVisualisation;

	@FXML
	private Button loadVisualisationButton;
	@FXML
	private Button reloadVisualisationButton;
	@FXML
	private ComboBox<Path> cbVisualisations;
	@FXML
	private MenuItem loadFromDefinitionsItem;
	@FXML
	private MenuItem setSelectedAsDefaultItem;
	@FXML
	private MenuItem deleteSelectedItem;
	@FXML
	private MenuItem saveCurrentItem;
	@FXML
	private MenuItem editCurrentExternalItem;
	@FXML
	private MenuButton saveTraceButton;
	@FXML
	private MenuItem saveTraceAndAddTestsItem;
	@FXML
	private MenuItem saveTraceAndRecordTestsItem;
	@FXML
	private MenuItem exportHistoryItem;
	@FXML
	private MenuItem exportHistoryWithSourceItem;
	@FXML
	private MenuItem exportHistoryCustomItem;
	@FXML
	private MenuItem exportCurrentStateItem;
	@FXML
	private MenuItem exportCurrentStateWithSourceItem;
	@FXML
	private MenuItem exportCurrentStateCustomItem;
	@FXML
	private MenuItem exportImageItem;
	@FXML
	private MenuItem exportSvgItem;
	@FXML
	private HBox inProgressBox;
	@FXML
	private Label inProgressLabel;
	@FXML
	private Button zoomInButton;
	@FXML
	private Button zoomOutButton;
	@FXML
	private Label lblActualVisualisationName;
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
		ExternalEditor externalEditor,
		PlantUmlLocator plantUmlLocator,
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
		this.externalEditor = externalEditor;
		this.plantUmlLocator = plantUmlLocator;
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

		this.cbVisualisations.setConverter(new StringConverter<>() {
			@Override
			public String toString(Path path) {
				if (path == null) {
					return "";
				} else if (VisBController.NO_PATH.equals(path)) {
					return i18n.translate("visb.visualisationDropdown.fromDefinitions");
				} else {
					return path.toString();
				}
			}

			@Override
			public Path fromString(String string) {
				throw new AssertionError("Should never be called");
			}
		});
		this.cbVisualisations.getSelectionModel().selectedItemProperty().subscribe(to -> {
			this.lblActualVisualisationName.setText("");
			if (to == null) {
				this.loadingStatus.setValue(LoadingStatus.NONE_LOADED);
				this.visBController.closeVisualisation();
			} else if (VisBController.NO_PATH.equals(to)) {
				this.loadingStatus.set(VisBView.LoadingStatus.LOADING);
				try {
					cliExecutor.submit(() -> getPathFromDefinitions(this.currentTrace.getStateSpace())).thenComposeAsync(path -> {
						if (path == null) {
							loadingStatus.set(VisBView.LoadingStatus.NONE_LOADED);
							return CompletableFuture.completedFuture(null);
						} else {
							Path relative = VisBController.relativizeVisBPath(this.currentProject.getLocation(), path);
							this.lblActualVisualisationName.setText(relative.toString());
							return visBController.loadFromAbsolutePath(path);
						}
					}, fxExecutor).exceptionally(exc -> {
						Platform.runLater(() -> this.showVisualisationLoadError(exc));
						return null;
					});
				} catch (RuntimeException exc) {
					this.showVisualisationLoadError(exc);
				}
			} else {
				this.loadingStatus.set(VisBView.LoadingStatus.LOADING);
				try {
					this.visBController.loadFromRelativePath(to).exceptionally(exc -> {
						Platform.runLater(() -> this.showVisualisationLoadError(exc));
						return null;
					});
				} catch (RuntimeException exc) {
					this.showVisualisationLoadError(exc);
				}
			}
		});

		this.currentProject.currentMachineProperty().subscribe(to -> {
			loadFromDefinitionsItem.disableProperty().unbind();
			setSelectedAsDefaultItem.disableProperty().unbind();
			deleteSelectedItem.disableProperty().unbind();
			saveCurrentItem.disableProperty().unbind();
			editCurrentExternalItem.disableProperty().unbind();
			cbVisualisations.itemsProperty().unbind();
			cbVisualisations.getSelectionModel().clearSelection();
			if (to == null) {
				loadFromDefinitionsItem.setDisable(true);
				setSelectedAsDefaultItem.setDisable(true);
				deleteSelectedItem.setDisable(true);
				saveCurrentItem.setDisable(true);
				editCurrentExternalItem.setDisable(true);
				cbVisualisations.setItems(FXCollections.observableArrayList());
			} else {
				ObjectBinding<Path> defaultVis = Bindings.valueAt(to.getVisBVisualisations(), 0);
				ReadOnlyObjectProperty<Path> currentVis = visBController.relativeVisBPathProperty();
				ReadOnlyObjectProperty<Path> selectedVis = cbVisualisations.getSelectionModel().selectedItemProperty();
				loadFromDefinitionsItem.disableProperty().bind(selectedVis.isEqualTo(VisBController.NO_PATH));
				setSelectedAsDefaultItem.disableProperty().bind(selectedVis.isNull().or(selectedVis.isEqualTo(defaultVis)));
				deleteSelectedItem.disableProperty().bind(selectedVis.isNull());
				saveCurrentItem.disableProperty().bind(currentVis.isNull().or(currentVis.isEqualTo(selectedVis)));
				editCurrentExternalItem.disableProperty().bind(currentVis.isNull().or(currentVis.isEqualTo(VisBController.NO_PATH)));
				cbVisualisations.itemsProperty().bind(to.getVisBVisualisations());
			}
		});

		this.visBController.visBVisualisationProperty().subscribe(to-> {
			loadingStatus.set(to == null ? VisBView.LoadingStatus.NONE_LOADED : VisBView.LoadingStatus.LOADING);
			visBController.getAttributeValues().clear();
			if (to != null) {
				this.loadVisualisationIntoWebView(to);
			}
		});

		this.loadingStatus.subscribe(to -> this.updateView(to, currentTrace.get()));
		this.currentTrace.subscribe((from, to) -> {
			if (to != null && (from == null || from.getStateSpace() != to.getStateSpace())) {
				cbVisualisations.getSelectionModel().selectFirst();
			}
			this.updateView(loadingStatus.get(), currentTrace.get());
		});

		visBController.executingEventProperty().addListener(o -> this.updateInProgress());
		updatingVisualisation.addListener(o -> this.updateInProgress());

		initButton.disableProperty().bind(disablePropertyController.disableProperty());

		this.reloadVisualisationButton.disableProperty().bind(visBController.absoluteVisBPathProperty().isNull());

		exportHistoryItem.setOnAction(e -> performHtmlExport(false, VisBExportOptions.DEFAULT_HISTORY));
		exportHistoryWithSourceItem.setOnAction(e -> performHtmlExport(false, VisBExportOptions.DEFAULT_HISTORY.withShowSource(true)));
		exportHistoryCustomItem.setOnAction(e -> performCustomisableHTMLExport(false));
		exportCurrentStateItem.setOnAction(e -> performHtmlExport(true, VisBExportOptions.DEFAULT_STATES));
		exportCurrentStateWithSourceItem.setOnAction(e -> performHtmlExport(true, VisBExportOptions.DEFAULT_STATES.withShowSource(true)));
		exportCurrentStateCustomItem.setOnAction(e -> performCustomisableHTMLExport(true));

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

	public void addVisBVisualisationFromAbsolutePath(Path absolutePath) {
		Path relativePath = VisBController.relativizeVisBPath(this.currentProject.getLocation(), absolutePath);
		this.addVisBVisualisationFromRelativePath(relativePath);
	}

	public void addVisBVisualisationFromRelativePath(Path relativePath) {
		ObservableList<Path> visBVisualisations = this.currentProject.getCurrentMachine().getVisBVisualisations();
		if (!visBVisualisations.contains(relativePath)) {
			visBVisualisations.add(relativePath);
		}
		this.cbVisualisations.getSelectionModel().select(relativePath);
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

	/**
	 * Create and initialize the {@link #webView} if it hasn't already been done.
	 * This is done lazily when the first VisB visualisation is loaded to improve overall startup time,
	 * because creating a {@link WebView} for the first time can be a bit slow:
	 * 600 ms on a fast system (Apple M1 Pro processor) and sometimes noticeably longer on older/slower systems.
	 */
	private void ensureWebViewCreated() {
		if (this.webView != null) {
			return;
		}

		LOGGER.debug("Creating VisB WebView...");
		this.webView = new WebView();
		LOGGER.debug("JavaFX WebView user agent: {}", this.webView.getEngine().getUserAgent());
		stageManager.initWebView(this.webView);

		this.webView.visibleProperty().bind(this.placeholder.visibleProperty().not());
		this.webView.setOnZoom(z -> webView.setZoom(webView.getZoom() * z.getZoomFactor()));
		this.mainPane.getChildren().add(webView);
		// Enable WebView-related actions only when the WebView is visible.
		exportImageItem.disableProperty().bind(this.placeholder.visibleProperty());
		exportSvgItem.disableProperty().bind(this.placeholder.visibleProperty());
		zoomInButton.disableProperty().bind(this.placeholder.visibleProperty());
		zoomOutButton.disableProperty().bind(this.placeholder.visibleProperty());
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
		this.ensureWebViewCreated();
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

	private JSObject getJSWindow() {
		return (JSObject)this.webView.getEngine().executeScript("window");
	}

	private void showPlaceholder(String placeholderLabelText) {
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
			fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.visBVisualisation", "json", "def")
		);
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		if (path != null) {
			this.addVisBVisualisationFromAbsolutePath(path);
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
	private void exportSvg() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("visb.stage.filechooser.export.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getSvgFilter());
		Path path = fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		exportSvgWithPath(path);
	}

	public void exportSvgWithPath(Path path) {
		if (path != null) {
			try {
				String svgContent = (String) webView.getEngine().executeScript(
						"new XMLSerializer().serializeToString(document.getElementById('visb_html_svg_content').firstElementChild)");
				Files.writeString(path, svgContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (Exception e) {
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
	private void loadFromDefinitions() {
		ObservableList<Path> visBVisualisations = this.currentProject.getCurrentMachine().getVisBVisualisations();
		if (!visBVisualisations.contains(VisBController.NO_PATH)) {
			visBVisualisations.add(VisBController.NO_PATH);
		}
		this.cbVisualisations.getSelectionModel().select(VisBController.NO_PATH);
	}

	@FXML
	private void setSelectedAsDefault() {
		Path selected = this.cbVisualisations.getSelectionModel().getSelectedItem();
		ObservableList<Path> visBVisualisations = this.currentProject.getCurrentMachine().getVisBVisualisations();
		visBVisualisations.remove(selected);
		visBVisualisations.add(0, selected);
	}

	@FXML
	private void deleteSelected() {
		Path selected = this.cbVisualisations.getSelectionModel().getSelectedItem();
		ObservableList<Path> visBVisualisations = this.currentProject.getCurrentMachine().getVisBVisualisations();
		visBVisualisations.remove(selected);
		this.cbVisualisations.getSelectionModel().clearSelection();
	}

	@FXML
	private void saveCurrent() {
		Path current = this.visBController.getRelativeVisBPath();
		if (current == null || VisBController.NO_PATH.equals(current)) {
			return;
		}

		ObservableList<Path> visBVisualisations = this.currentProject.getCurrentMachine().getVisBVisualisations();
		if (!visBVisualisations.contains(current)) {
			visBVisualisations.add(current);
		}
		this.cbVisualisations.getSelectionModel().select(current);
	}

	@FXML
	private void editCurrentExternal() {
		Path current = this.visBController.getAbsoluteVisBPath();
		if (current == null || VisBController.NO_PATH.equals(current)) {
			return;
		}

		this.externalEditor.open(current);
	}

	void performHtmlExport(final boolean onlyCurrentState, final VisBExportOptions options) {
		Trace trace = this.currentTrace.get();
		if (trace == null) {
			return;
		}
		Path path = this.showHtmlExportFileChooser();
		if (path == null) {
			return;
		}

		if (options.isShowSequenceChart() && this.plantUmlLocator.findPlantUmlJar().isEmpty()) {
			return;
		}

		// makes UI responsive, but we can't do anything with the model during export anyway...
		this.cliExecutor.submit(() -> {
			Platform.runLater(() -> this.showInProgress(this.i18n.translate("visb.inProgress.htmlExport")));
			trace.getStateSpace().execute(onlyCurrentState
					                              ? new ExportVisBHtmlForStates(trace.getCurrentState(), options, path)
					                              : new ExportVisBForHistoryCommand(trace, options, path));
		}).handleAsync((res, ex) -> {
			this.hideInProgress();
			if (ex != null) {
				if (options.isShowSequenceChart()) {
					this.plantUmlLocator.reset(); // could be an error with the plantuml jar, so clear the cached file
				}
				this.stageManager.showUnhandledExceptionAlert(ex, this.getScene().getWindow());
			}
			return res;
		}, this.fxExecutor);
	}

	private Path showHtmlExportFileChooser() {
		final FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter htmlFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.html", "html");
		fileChooser.getExtensionFilters().setAll(htmlFilter);
		fileChooser.setTitle(i18n.translate("common.fileChooser.save.title"));
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName());

		return fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, this.getScene().getWindow());
	}

	private void performCustomisableHTMLExport(boolean onlyCurrentState) {
		VisBHTMLConfigDialog dialog = injector.getInstance(VisBHTMLConfigDialog.class);
		dialog.initialiseForOptions(onlyCurrentState);
		dialog.showAndWait();
	}
}
