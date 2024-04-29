package de.prob2.ui.simulation.diagram;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.DotCall;
import de.prob.animator.domainobjects.DotOutputFormat;
import de.prob.exception.ProBError;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


@Singleton
public class DiagramStage extends Stage {

    private final StageManager stageManager;

	private final boolean islive;

    private final FileChooserManager fileChooserManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final Injector injector;

    private final I18n i18n;

    private String nodesString;

	private static final Logger LOGGER = LoggerFactory.getLogger(DiagramStage.class);
    
    @FXML
    private WebView diagramView; 

    @FXML
    private Button zoomInButton;
    
    @FXML
    private Button zoomOutButton;

    @FXML
    private Button saveButton;

    @Inject
    public DiagramStage(StageManager stageManager, CurrentProject currentProject, CurrentTrace currentTrace,
            Injector injector, I18n i18n, String nodesString, FileChooserManager fileChooserManager, boolean islive) {
		
        super();
        this.stageManager = stageManager;
        this.fileChooserManager = fileChooserManager;
        this.currentProject = currentProject;
        this.currentTrace = currentTrace;
        this.injector = injector;
        this.i18n = i18n;
        this.nodesString = nodesString;
        stageManager.loadFXML(this, "activation_Diagram_Stage.fxml", this.getClass().getName());
		this.islive = islive;
        }


    @FXML
    public void initialize(){
        loadGraph(new String(makeGraphString(), StandardCharsets.UTF_8));
		
    }

	public void updateGraph(String newDiagram){
		nodesString = newDiagram;
		loadGraph(new String(makeGraphString(),StandardCharsets.UTF_8));
	}

    public byte[] makeGraphString(){
        byte[] svgdiagramm=null; 
        DotCall dotCall = new DotCall("dot")
            .layoutEngine("dot")
            .outputFormat(DotOutputFormat.SVG)
            .input(nodesString);
        try {
            svgdiagramm = dotCall.call();
        } catch (ProBError |InterruptedException e) {
            LOGGER.error("Could not Visualize Graph with dot input)",e);
            return null;
        }
        return svgdiagramm;
    }


    public void loadGraph(String svgContent){
            diagramView.getEngine().loadContent("<center>" + svgContent + "</center>");
		    diagramView.setVisible(true);
            
    }

    @FXML
	private void zoomIn() {
		zoomByFactor(1.15);
	}

	@FXML
	private void zoomOut() {
		zoomByFactor(0.85);
	}
    
	private void zoomByFactor(double factor) {
		diagramView.setZoom(diagramView.getZoom() * factor);
	}

    @FXML
	private void save() {
		final FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter svgFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.svg", "svg");
		FileChooser.ExtensionFilter pngFilter = fileChooserManager.getPngFilter();
		FileChooser.ExtensionFilter dotFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.dot", "dot");
		FileChooser.ExtensionFilter pdfFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.pdf", "pdf");
		fileChooser.getExtensionFilters().setAll(svgFilter, pngFilter, dotFilter, pdfFilter);
		fileChooser.setTitle(i18n.translate("common.fileChooser.save.title"));
		final Path path = fileChooserManager.showSaveFileChooser(fileChooser, null, this.getScene().getWindow());
		if (path == null) {
			return;
		}
		FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();
		if (selectedFilter.equals(dotFilter)) {
			saveDot(path);
		} else {
			final String format = getTargetFormat(selectedFilter, svgFilter, pngFilter, pdfFilter);
			saveConverted(format, path);
		}
	}
	

	private String getTargetFormat(FileChooser.ExtensionFilter selectedFilter, FileChooser.ExtensionFilter svgFilter, FileChooser.ExtensionFilter pngFilter, FileChooser.ExtensionFilter pdfFilter) {
		if (selectedFilter.equals(svgFilter)) {
			return DotOutputFormat.SVG;
		} else if (selectedFilter.equals(pngFilter)) {
			return DotOutputFormat.PNG;
		} else if (selectedFilter.equals(pdfFilter)) {
			return DotOutputFormat.PDF;
		} else {
			throw new RuntimeException("Target Format cannot be extracted from selected filter: " + selectedFilter);
		}
	}

	private void saveDot(final Path path) {
		try {
			Files.writeString(path, nodesString);
		} catch (IOException e) {
			LOGGER.error("Failed to save Dot", e);
		}
	}

	private void saveConverted(String format, final Path path) {
		try {
			Files.write(path, new DotCall("dot")
					.layoutEngine("dot")
					.outputFormat(format)
					.input(nodesString)
					.call());
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Failed to save file converted from dot", e);
		}
	}

	public boolean getIsLive(){
		return islive;
	}
}
