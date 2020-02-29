package de.prob2.ui.visb;

import com.google.inject.Injector;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.project.Project;
import de.prob2.ui.visb.exceptions.VisBException;
import de.prob2.ui.visb.help.UserManualStage;
import de.prob2.ui.visb.ui.ListViewEvent;
import de.prob2.ui.visb.ui.ListViewItem;
import de.prob2.ui.visb.visbobjects.VisBEvent;
import de.prob2.ui.visb.visbobjects.VisBItem;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;
import de.prob2.ui.error.ExceptionAlert;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.nio.file.Path;
import java.util.ResourceBundle;

/**
 * Created by
 * @author Michelle Werth
 * @since 21.03.2019
 * @version 0.1.0
 *
 * This class holds the main user interface and interacts with the {@link VisBController} and {@link VisBConnector} classes.
 * */

@Singleton
public class VisBStage extends Stage {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisBStage.class);
    private Injector injector;
    private ResourceBundle bundle;
    private StageManager stageManager;
    private CurrentProject currentProject;
    private ChangeListener<Worker.State> stateListener = null;
    private boolean connectorSet = false;
	private FileChooserManager fileChooserManager;

    @FXML
    private Button button_loadVis;
    @FXML
    private StackPane zoomingPane;
    @FXML
    private WebView webView;
    @FXML
    private ListView<VisBItem> visBItems;
    @FXML
    private ListView<VisBEvent> visBEvents;
	@FXML
	private MenuItem editMenu_reload;
	@FXML
	private MenuItem editMenu_close;
    @FXML
    private MenuItem fileMenu_close;
    @FXML
    private MenuItem fileMenu_visB;
    @FXML
    private MenuItem viewMenu_zoomIn;
    @FXML
    private MenuItem viewMenu_zoomOut;
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
    public VisBStage(final Injector injector, final StageManager stageManager, final CurrentProject currentProject, final ResourceBundle bundle, final FileChooserManager fileChooserManager) {
        super();
        stageManager.loadFXML(this, "vis_plugin_stage.fxml");
        this.injector = injector;
        this.bundle = bundle;
        this.stageManager = stageManager;
        this.currentProject = currentProject;
        this.fileChooserManager = fileChooserManager;
    }

    /**
     * With this method a visible stage with an empty WebView and an empty ListView is initialised.
     */
    @FXML
    public void initialize(){
        this.helpMenu_userManual.setOnAction(e -> injector.getInstance(UserManualStage.class).show());
        this.button_loadVis.setOnAction(e -> loadVisBFile());
        this.fileMenu_visB.setOnAction(e -> loadVisBFile());
        this.fileMenu_close.setOnAction(e -> sendCloseRequest());
        this.editMenu_reload.setOnAction(e -> injector.getInstance(VisBController.class).reloadVisualisation());
        this.editMenu_close.setOnAction(e -> injector.getInstance(VisBController.class).closeCurrentVisualisation());
        this.viewMenu_zoomIn.setOnAction(e -> webView.setZoom(webView.getZoom()*1.2));
        this.viewMenu_zoomOut.setOnAction(e -> webView.setZoom(webView.getZoom()/1.2));
        this.visBItems.setCellFactory(lv -> new ListViewItem(stageManager));
        this.visBEvents.setCellFactory(lv -> new ListViewEvent(stageManager));
        this.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
        	injector.getInstance(VisBController.class).closeCurrentVisualisation();
		});
    }

    private void sendCloseRequest(){
    	this.fireEvent(new WindowEvent(
				this,
				WindowEvent.WINDOW_CLOSE_REQUEST)
		);
	}

    /**
     * After loading the svgFile and preparing it in the {@link VisBController} the WebView is initialised.
     * @param svgFile the image/ svg, that should to be loaded into the context of the WebView
     */
    void initialiseWebView(String svgFile) {
		if (svgFile != null) {
			this.placeholder.setVisible(false);
			this.webView.setVisible(true);
			String htmlFile = "<!DOCTYPE html>\n" +
					"<html>\n" +
					"<head>\n" +
					//This is for zooming in and out and scaling the image to the right width and height
					"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
					"<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js\"></script>\n" +
					"<script>\n" +
					"function changeAttribute(id, attribute, value){\n" +
					"  $(document).ready(function(){\n" +
					"    $(id).attr(attribute, value);\n" +
					"  });\n" +
					"};" +
					"</script>\n" +
					"</head>\n" +
					"<body>\n" +
					"\n" +
					"<div text-align=\"center\">" +
					svgFile +
					"\n" +
					"</div>" +
					"</body>\n" +
					"</html>";
			this.webView.getEngine().loadContent(htmlFile);
			LOGGER.debug("HTML was loaded into WebView");
			addVisBConnector();
		}
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
				}
			});
			this.webView.getEngine().setOnError(e -> {
				alert(new Exception(), "visb.exception.header", "visb.stage.alert.webview.error");
			});
			LOGGER.debug("VisBConnector was set into globals.");
		}
	}

    /**
     * After loading the JSON/ VisB file and preparing it in the {@link VisBController} the ListViews are initialised.
     * @param visBVisualisation is needed to display the items and events in the ListViews
     */
    void initialiseListViews(VisBVisualisation visBVisualisation){
        //Preparing the Items
        this.visBItems.setItems(FXCollections.observableArrayList(visBVisualisation.getVisBItems()));
        //Preparing the Events
        this.visBEvents.setItems(FXCollections.observableArrayList(visBVisualisation.getVisBEvents()));

    }

    /**
     * This method clears our the WebView and the ListView and removes possible listeners, so that the VisBStage no longer interacts with anything.
     */
    void clear(){
    	LOGGER.debug("Clear the stage!");
        if(connectorSet && stateListener != null) {
        	LOGGER.debug("Remove State Listener - VisBConnector - from VisBStage");
            this.webView.getEngine().getLoadWorker().stateProperty().removeListener(stateListener);
            stateListener = null;
        }
        this.webView.setVisible(false);
        this.placeholder.setVisible(true);
        this.visBEvents.setItems(null);
        this.visBItems.setItems(null);
    }

    /**
     * In here, the onClick functions for the SVG image objects, which are declared in the JSON/ VisB file are loaded.
     * @param onClickEventQuery onClick functions to be executed
     */
    void loadOnClickFunctions(String onClickEventQuery){
        stateListener = (ov, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                webView.getEngine().executeScript(onClickEventQuery);
                LOGGER.debug("On click functions for visBEvents have been loaded.");
            }
        };
        this.webView.getEngine().getLoadWorker().stateProperty().addListener(stateListener);
    }

    /**
     * This method runs the jQuery script in the WebView.
     * @param jQuery script to be run
     */
    void runScript(String jQuery) {
        if(webView.getEngine().getLoadWorker().getState().equals(Worker.State.RUNNING)){
            this.webView.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    this.webView.getEngine().executeScript(jQuery);
                }
            });
        } else {
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
    private void loadVisBFile() {
    	clear();
        if(!currentProject.exists() || currentProject.getCurrentMachine() == null){
        	LOGGER.debug("Tried to start visualisation when no machine was loaded.");
            alert(new VisBException(),  "visb.stage.alert.load.machine.header", "visb.exception.no.machine");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("visb.stage.filechooser.title"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Visualisation", "*.json")
        );
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		if(path != null) {
			File visBfile = path.toFile();
			injector.getInstance(VisBController.class).setupVisBFile(visBfile);
		}
    }

    /**
     * This method throws an ProB2-UI ExceptionAlert
     */
    private void alert(Throwable ex, String header, String body, Object... params){
		this.stageManager.makeExceptionAlert(ex, header, body, params).showAndWait();
    }
}


