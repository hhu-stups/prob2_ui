package de.prob2.ui.visb;

import com.google.inject.Injector;
import de.prob2.ui.Main;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.animation.tracereplay.TraceSaver;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.MustacheTemplateManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.TraceSelectionView;
import de.prob2.ui.visb.exceptions.VisBException;
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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


/**
 * This class holds the main user interface and interacts with the {@link VisBController} and {@link VisBConnector} classes.
 */
@Singleton
public class VisBStage extends Stage {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisBStage.class);
    private Injector injector;
    private ResourceBundle bundle;
    private StageManager stageManager;
    private CurrentProject currentProject;
    private CurrentTrace currentTrace;
    private boolean connectorSet = false;
    private FileChooserManager fileChooserManager;
    private ObjectProperty<Path> visBPath;

    @FXML
    private MenuBar visbMenuBar;
    @FXML
    private Button loadVisualisationButton;
    @FXML
    private Button manageDefaultVisualisationButton;
    @FXML
    private Button openTraceSelectionButton;
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
                     final CurrentTrace currentTrace, final ResourceBundle bundle, final FileChooserManager fileChooserManager) {
        super();
        this.injector = injector;
        this.bundle = bundle;
        this.stageManager = stageManager;
        this.currentProject = currentProject;
        this.currentTrace = currentTrace;
        this.fileChooserManager = fileChooserManager;
        this.visBPath = new SimpleObjectProperty<>(this, "visBPath", null);
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
        this.titleProperty().bind(Bindings.createStringBinding(() -> visBPath.isNull().get() ? bundle.getString("visb.title") : String.format(bundle.getString("visb.currentVisualisation"), currentProject.getLocation().relativize(visBPath.get()).toString()), visBPath));
        this.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
            visBPath.set(null);
            injector.getInstance(VisBController.class).closeCurrentVisualisation();
        });
        //Load VisB file from machine, when window is opened and set listener on the current machine

        updateUIOnMachine(currentProject.getCurrentMachine());
        loadVisBFileFromMachine(currentProject.getCurrentMachine());
        this.currentProject.currentMachineProperty().addListener((observable, from, to) -> {
            openTraceSelectionButton.disableProperty().unbind();
            manageDefaultVisualisationButton.disableProperty().unbind();
            updateUIOnMachine(to);
            loadVisBFileFromMachine(to);
        });
    }

    private void updateUIOnMachine(Machine machine) {
        final BooleanBinding openTraceDefaultDisableProperty = currentProject.currentMachineProperty().isNull();
        manageDefaultVisualisationButton.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(visBPath.isNull()));
        if(machine != null) {
            openTraceSelectionButton.disableProperty().bind(machine.tracesProperty().emptyProperty());
        } else {
            openTraceSelectionButton.disableProperty().bind(openTraceDefaultDisableProperty);
        }
    }

    private void loadVisBFileFromMachine(Machine machine) {
        clear();
        visBPath.set(null);
        if(machine != null) {
            Path visBVisualisation = machine.getVisBVisualisation();
            visBPath.set(visBVisualisation == null ? null : currentProject.getLocation().resolve(visBVisualisation));
            if(currentTrace.getStateSpace() != null) {
                Platform.runLater(this::setupMachineVisBFile);
            } else {
                this.currentTrace.stateSpaceProperty().addListener((observable, from, to) -> {
                    if (to != null && (from == null || !from.getLoadedMachine().equals(to.getLoadedMachine())) && visBPath.isNotNull().get()) {
                        this.setupMachineVisBFile();
                    }
                });
            }
        }
    }

    private void setupMachineVisBFile() {
        if(visBPath.isNotNull().get()) {
            VisBController visBController = injector.getInstance(VisBController.class);
            File visBfile = visBPath.get().toFile();
            visBController.setupVisualisation(visBfile);
        }
    }

    private void sendCloseRequest(){
        this.fireEvent(new WindowEvent(
                this,
                WindowEvent.WINDOW_CLOSE_REQUEST)
        );
    }

    /**
     * After loading the svgFile and preparing it in the {@link VisBController} the WebView is initialised.
     * @param svgContent the image/ svg, that should to be loaded into the context of the WebView
     */
    void initialiseWebView(File file, List<VisBOnClickMustacheItem> clickEvents, File jsonFile, String svgContent) {
        if (svgContent != null) {
            this.placeholder.setVisible(false);
            this.webView.setVisible(true);
            String jqueryLink = Main.class.getResource("jquery.js").toExternalForm();
            String htmlFile = generateHTMLFileWithSVG(jqueryLink, clickEvents, jsonFile, svgContent);
            this.webView.getEngine().loadContent(htmlFile);
            LOGGER.debug("HTML was loaded into WebView with SVG file "+file);
            addVisBConnector();
            this.webView.getEngine().setOnAlert(event -> showJavascriptAlert(event.getData()));
            this.webView.getEngine().setOnError(event -> treatJavascriptError(event)); // check if we get errors
            // Note: only called while loading page: https://stackoverflow.com/questions/31391736/for-javafxs-webengine-how-do-i-get-notified-of-javascript-errors
            // engine.setConfirmHandler(message -> showConfirm(message));
        }

    }

    private void treatJavascriptError(WebErrorEvent event) {
        LOGGER.debug("JavaScript ERROR: " + event.getMessage());
        alert(event.getException(), "visb.exception.header", "visb.stage.alert.webview.jsalert", event.getMessage());
    }

    private void showJavascriptAlert(String message) {
        LOGGER.debug("JavaScript ALERT: " + message);
        alert(new Exception(), "visb.exception.header", "visb.stage.alert.webview.jsalert", message);
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
            this.webView.getEngine().setOnError(e -> {
                alert(new Exception(), "visb.exception.header", "visb.stage.alert.webview.error");
            });
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
                      new ChangeListener<Worker.State>() {
                        @Override
                        public void changed(
                                    ObservableValue<? extends Worker.State> observable,
                                    Worker.State oldValue, Worker.State newValue) {
                          switch (newValue) {
                            case SUCCEEDED:
                            case FAILED:
                            case CANCELLED:
                              webView
                                .getEngine()
                                .getLoadWorker()
                                .stateProperty()
                                .removeListener(this);
                          }
                          if (newValue != Worker.State.SUCCEEDED) {
                            return;
                          }
                          LOGGER.debug("runScript: "+jQuery+"\n-----");
                          webView.getEngine().executeScript(jQuery);
                        }
                      } );
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

    private void loadDefaultVisualisation() {
        Machine currentMachine = currentProject.getCurrentMachine();
        loadVisBFileFromMachine(currentMachine);
    }

    private void setDefaultVisualisation() {
        Machine currentMachine = currentProject.getCurrentMachine();
        currentMachine.setVisBVisualisation(currentProject.getLocation().relativize(visBPath.get()));
    }

    private void resetDefaultVisualisation() {
        Machine currentMachine = currentProject.getCurrentMachine();
        currentMachine.setVisBVisualisation(null);
    }

    /**
     * On click function for the button and file menu item
     */
    @FXML
    public void loadVisBFile() {
        if(currentProject.getCurrentMachine() == null){
            LOGGER.debug("Tried to start visualisation when no machine was loaded.");
            alert(new VisBException(),  "visb.stage.alert.load.machine.header", "visb.exception.no.machine");
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
            visBPath.set(path);
            File visBfile = path.toFile();
            VisBController visBController = injector.getInstance(VisBController.class);
            visBController.setupVisualisation(visBfile);
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
    public void saveTrace() {
        injector.getInstance(TraceSaver.class).saveTrace(this.getScene().getWindow(), TraceReplayErrorAlert.Trigger.TRIGGER_VISB);
    }

    @FXML
    public void reloadVisualisation() {
        injector.getInstance(VisBController.class).reloadVisualisation();
    }

    @FXML
    public void closeVisualisation() {
        visBPath.set(null);
        injector.getInstance(VisBController.class).closeCurrentVisualisation();
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

    private String generateHTMLFileWithSVG(String jqueryLink, List<VisBOnClickMustacheItem> clickEvents, File jsonFile, String svgContent) {
        try {
            URI uri = VisBStage.class.getResource("visb_html_view.mustache").toURI();
            MustacheTemplateManager templateManager = new MustacheTemplateManager(uri, "visb_html_view");
            templateManager.put("jqueryLink", jqueryLink);
            templateManager.put("clickEvents", clickEvents);
            templateManager.put("jsonFile", jsonFile);
            templateManager.put("svgContent", svgContent);
            return templateManager.apply();
        } catch (URISyntaxException e) {
            LOGGER.error("", e);
            return "";
        }
    }

    @FXML
    public void showVisBItemsAndEvents() {
        injector.getInstance(VisBDebugStage.class).show();
    }

    @FXML
    public void manageDefaultVisualisation() {
        Machine machine = currentProject.getCurrentMachine();
        List<ButtonType> buttons = new ArrayList<>();

        ButtonType loadButton = new ButtonType(bundle.getString("visb.defaultVisualisation.load"));
        ButtonType setButton = new ButtonType(bundle.getString("visb.defaultVisualisation.set"));
        ButtonType resetButton = new ButtonType(bundle.getString("visb.defaultVisualisation.reset"));

        Alert alert;
        if(machine.visBVisualizationProperty().isNotNull().get()) {
            buttons.add(loadButton);
            if(visBPath != null && !currentProject.getLocation().relativize(visBPath.get()).equals(machine.getVisBVisualisation())) {
                buttons.add(setButton);
            }
            buttons.add(resetButton);
            ButtonType buttonTypeCancel = new ButtonType(bundle.getString("common.buttons.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
            buttons.add(buttonTypeCancel);
            alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION, buttons, "visb.defaultVisualisation.header", "visb.defaultVisualisation.text", machine.visBVisualizationProperty().get());
        } else {
            if(visBPath != null) {
                buttons.add(setButton);
            }
            ButtonType buttonTypeCancel = new ButtonType(bundle.getString("common.buttons.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
            buttons.add(buttonTypeCancel);
            alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION, buttons, "visb.defaultVisualisation.header", "visb.noDefaultVisualisation.text");
        }

        alert.initOwner(this);

        Optional<ButtonType> result = alert.showAndWait();
        if(result.isPresent()) {
            if (result.get() == loadButton) {
                loadDefaultVisualisation();
            } else if (result.get() == setButton) {
                setDefaultVisualisation();
            } else if (result.get() == resetButton) {
                resetDefaultVisualisation();
            } else {
               alert.close();
            }
        }
    }

    public void resetJavaScriptInteraction() {
        this.webView.setDisable(true);
    }

    public void activateJavaScriptInteraction() {
        this.webView.setDisable(false);
    }

}


