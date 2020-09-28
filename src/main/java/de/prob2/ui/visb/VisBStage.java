package de.prob2.ui.visb;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Injector;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.visb.exceptions.VisBException;
import de.prob2.ui.visb.help.UserManualStage;
import de.prob2.ui.visb.ui.ListViewEvent;
import de.prob2.ui.visb.ui.ListViewItem;
import de.prob2.ui.visb.visbobjects.VisBEvent;
import de.prob2.ui.visb.visbobjects.VisBItem;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.control.MenuBar;
import javafx.event.EventHandler;
import javafx.scene.web.WebEvent;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


import netscape.javascript.JSObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private ChangeListener<Worker.State> stateListener = null;
	private boolean connectorSet = false;
	private FileChooserManager fileChooserManager;

	@FXML
	private MenuBar visbMenuBar;
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
	public VisBStage(final Injector injector, final StageManager stageManager, final CurrentProject currentProject, final ResourceBundle bundle, final FileChooserManager fileChooserManager) {
		super();
		this.injector = injector;
		this.bundle = bundle;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.fileChooserManager = fileChooserManager;
		this.stageManager.loadFXML(this, "vis_plugin_stage.fxml");
	}

	/**
	 * With this method a visible stage with an empty WebView and an empty ListView is initialised.
	 */
	@FXML
	public void initialize(){
		this.stageManager.setMacMenuBar(this, visbMenuBar);
		this.helpMenu_userManual.setOnAction(e -> injector.getInstance(UserManualStage.class).show());
		this.button_loadVis.setOnAction(e -> loadVisBFile());
		this.fileMenu_visB.setOnAction(e -> loadVisBFile());
		this.fileMenu_close.setOnAction(e -> sendCloseRequest());
		this.fileMenu_export.setOnAction(e -> exportImage());
		this.editMenu_reload.setOnAction(e -> injector.getInstance(VisBController.class).reloadVisualisation());
		this.editMenu_close.setOnAction(e -> injector.getInstance(VisBController.class).closeCurrentVisualisation());
		this.viewMenu_zoomIn.setOnAction(e -> webView.setZoom(webView.getZoom()*1.2));
		this.viewMenu_zoomOut.setOnAction(e -> webView.setZoom(webView.getZoom()/1.2));
		// zoom fonts in/out (but only of those that are not given a fixed size):
		this.viewMenu_zoomFontsIn.setOnAction(e -> webView.setFontScale(webView.getFontScale()*1.25));
		this.viewMenu_zoomFontsOut.setOnAction(e -> webView.setFontScale(webView.getFontScale()/1.25));
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
	void initialiseWebView(File file, String svgFile) {
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
					// Provide debugging if the VisB SVG file contains such a text span:
					//"    $(\"#visb_debug_messages\").text(\"changeAttribute(\" + id + \",\" + attribute + \",\" + value +\")\");\n" +
					"    $(id).attr(attribute, value);\n" +
					// Provide debugging message if an SVG object id cannot be found:
					"    if(!$(id).length) {alert(\"Unknown SVG id: \" + id + \" for attr \"+ attribute + \" and value \" +  value);}\n" +
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
			LOGGER.debug("HTML was loaded into WebView with SVG file "+file);
			addVisBConnector();
			this.webView.getEngine().setOnAlert(event -> showJavascriptAlert(event.getData()));
            // engine.setConfirmHandler(message -> showConfirm(message));
		}

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
						  webView.getEngine().executeScript(jQuery);
						  //LOGGER.debug("runScript: "+jQuery+"\n-----");
						}
					  } );
				    LOGGER.debug("registered runScript as Listener");
		} else {
			this.webView.getEngine().executeScript(jQuery);
			//LOGGER.debug("runScript directly: "+jQuery+"\n-----");
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
			File visBfile = path.toFile();
			injector.getInstance(VisBController.class).setupVisBFile(visBfile);
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
}


