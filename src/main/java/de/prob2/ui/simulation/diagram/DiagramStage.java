package de.prob2.ui.simulation.diagram;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.DotCall;
import de.prob.animator.domainobjects.DotOutputFormat;
import de.prob.exception.ProBError;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DiagramStage extends Stage {
	private final boolean islive;

	private final FileChooserManager fileChooserManager;

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
	public DiagramStage(StageManager stageManager, I18n i18n, FileChooserManager fileChooserManager, boolean islive) {
		super();
		this.fileChooserManager = fileChooserManager;
		this.i18n = i18n;
		this.nodesString = null;
		stageManager.loadFXML(this, "activation_Diagram_Stage.fxml", this.getClass().getName());
		this.islive = islive;
	}

	public void updateGraph(String newDiagram){
		nodesString = newDiagram;
		loadGraph(makeGraphString(newDiagram));
	}

	//Calls dotengine to build SVG string to load in FXML 
	private static String makeGraphString(String input) {
		DotCall dotCall = new DotCall("dot")
			.layoutEngine("dot")
			.outputFormat(DotOutputFormat.SVG)
			.input(input);
		try {
			byte[] svgBytes = dotCall.call();
			return new String(svgBytes, StandardCharsets.UTF_8);
		} catch (ProBError |InterruptedException e) {
			LOGGER.error("Could not Visualize Graph with dot input)",e);
			return null;
		}
	}

	//loads diagram into FXML page
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
