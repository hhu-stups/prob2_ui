package de.prob2.ui.visualisation.magiclayout;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.json.JsonMetadata;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.Trace;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.menu.OpenFile;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visualisation.magiclayout.editpane.MagicLayoutEditEdges;
import de.prob2.ui.visualisation.magiclayout.editpane.MagicLayoutEditNodes;

import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;

@Singleton
public class MagicLayoutView extends Stage {

	@FXML
	private MenuBar menuBar;
	@FXML
	private TabPane editTabPane;
	@FXML
	private Tab editNodesTab;
	@FXML
	private Tab editEdgesTab;
	@FXML
	private MagicLayoutEditNodes magicLayoutEditNodes;
	@FXML
	private MagicLayoutEditEdges magicLayoutEditEdges;
	@FXML
	private StackPane magicGraphStackPane; // The StackPane which contains a group which contains the magicGraphPane
	@FXML
	private StackPane magicGraphPane;
	@FXML
	private Button zoomInButton;
	@FXML
	private Button zoomOutButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private Button updateButton;
	@FXML
	private Button layoutButton;
	@FXML
	private ChoiceBox<MagicLayout> layoutChoiceBox;

	private final Injector injector;
	private final StageManager stageManager;
	private final MagicGraphI magicGraph;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final VersionInfo versionInfo;
	private final MagicLayoutSettingsManager settingsManager;
	private final I18n i18n;

	@Inject
	public MagicLayoutView(final Injector injector, final StageManager stageManager, final MagicGraphI magicGraph,
	                       final CurrentTrace currentTrace, final CurrentProject currentProject,
	                       final VersionInfo versionInfo,
	                       final MagicLayoutSettingsManager settingsManager, final I18n i18n) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.magicGraph = magicGraph;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.versionInfo = versionInfo;
		this.settingsManager = settingsManager;
		this.i18n = i18n;
		stageManager.loadFXML(this, "magic_layout_view.fxml");
	}

	@FXML
	public void initialize() {
		stageManager.setMacMenuBar(this, menuBar);
		helpButton.setHelpContent("mainmenu.visualisations.magicLayout", null);

		// make GraphPane zoomable
		magicGraphPane.setOnZoom(event -> zoom(event.getZoomFactor()));
		magicGraphStackPane.setOnZoom(event -> zoom(event.getZoomFactor())); // recognize zoom Motion outside the
																				// magicGraphPane area
		zoomInButton.setOnAction(event -> zoom(1.1));
		zoomOutButton.setOnAction(event -> zoom(0.9));

		// generate new graph whenever the model changes
		ChangeListener<? super AbstractModel> modelChangeListener = (observable, from, to) -> {
			layoutGraph();
			disableButtons(to == null);
		};
		// update existing graph whenever the trace changes
		ChangeListener<? super Trace> traceChangeListener = (observable, from, to) -> updateGraph();

		// only listen to changes when the stage is showing
		showingProperty().addListener((observable, from, to) -> {
			if (to) {
				layoutGraph();
				disableButtons(currentTrace.getModel() == null);
				currentTrace.modelProperty().addListener(modelChangeListener);
				currentTrace.addListener(traceChangeListener);
			} else {
				currentTrace.modelProperty().removeListener(modelChangeListener);
				currentTrace.removeListener(traceChangeListener);
			}
		});
		
		disableButtons(true);
		
		layoutChoiceBox.getItems().setAll(magicGraph.getSupportedLayouts());
		layoutChoiceBox.getSelectionModel().selectFirst();
		layoutChoiceBox.setConverter(i18n.translateConverter());
	}

	private void disableButtons(boolean disable) {
		zoomInButton.setDisable(disable);
		zoomOutButton.setDisable(disable);
		updateButton.setDisable(disable);
		layoutButton.setDisable(disable);
		layoutChoiceBox.setDisable(disable);
	}

	private void zoom(double zoomFactor) {
		magicGraphPane.setScaleX(magicGraphPane.getScaleX() * zoomFactor);
		magicGraphPane.setScaleY(magicGraphPane.getScaleY() * zoomFactor);
	}

	@FXML
	private void layoutGraph() {
		if(isInitializedOrNull()){
			magicGraphPane.getChildren().setAll(
				magicGraph.generateMagicGraph(currentTrace.getCurrentState(),
					layoutChoiceBox.getSelectionModel().getSelectedItem()));
			magicGraph.setGraphStyle(magicLayoutEditNodes.getNodegroups(),
				magicLayoutEditEdges.getEdgegroups());
		}
	}

	@FXML
	private void updateGraph() {
		if(isInitializedOrNull()){
			magicGraph.updateMagicGraph(currentTrace.getCurrentState());
			magicGraph.setGraphStyle(magicLayoutEditNodes.getNodegroups(),
				magicLayoutEditEdges.getEdgegroups());
		}
	}

	private boolean isInitializedOrNull() {
		if (currentTrace.getCurrentState() == null || currentTrace.getCurrentState().isInitialised()) {
			return true;
		} else {
			Alert alert =
				stageManager.makeAlert(Alert.AlertType.WARNING, "visualisation.magicLayout.alert.title",
					"visualisation.magicLayout.alert.content");
			alert.initOwner(this);
			alert.showAndWait();
			return false;
		}
	}

	@FXML
	private void newNodegroup() {
		editTabPane.getSelectionModel().select(editNodesTab);
		magicLayoutEditNodes.addNewNodegroup();
	}

	@FXML
	private void newEdgegroup() {
		editTabPane.getSelectionModel().select(editEdgesTab);
		magicLayoutEditEdges.addNewEdgegroup();
	}

	@FXML
	private void saveLayoutSettings() {
		final String machineName = currentProject.getCurrentMachine().getName();
		final JsonMetadata metadata = MagicLayoutSettings.metadataBuilder()
			.withProBCliVersion(versionInfo.getCliVersion().getShortVersionString())
			.withModelName(machineName)
			.build();
		MagicLayoutSettings layoutSettings = new MagicLayoutSettings(machineName,
				magicLayoutEditNodes.getNodegroups(), magicLayoutEditEdges.getEdgegroups(), metadata);
		settingsManager.save(layoutSettings);
	}

	@FXML
	private void loadLayoutSettings() {
		MagicLayoutSettings layoutSettings = settingsManager.load();

		if (layoutSettings != null) {
			magicLayoutEditNodes.openLayoutSettings(layoutSettings);
			magicLayoutEditEdges.openLayoutSettings(layoutSettings);
		}
	}

	@FXML
	private void saveGraphAsImage() {
		double zoomFactor = magicGraphPane.getScaleX();
		zoom(1.0/zoomFactor);
		
		// scale image for better and sharper quality
		WritableImage image = new WritableImage((int) Math.rint(4 * magicGraphPane.getWidth()),
				(int) Math.rint(4 * magicGraphPane.getHeight()));
		SnapshotParameters params = new SnapshotParameters();
		params.setTransform(Transform.scale(4, 4));
		image = magicGraphPane.snapshot(params, image);

		zoom(zoomFactor);

		String name = currentProject.getCurrentMachine().getName();
		final Path projectFolder = currentProject.get().getLocation();

		// create magic_graph folder if it doesn't exist
		final Path magicImageFolder = projectFolder.resolve("magic_graph");
		if (!magicImageFolder.toFile().exists()) {
			magicImageFolder.toFile().mkdir();
		}

		File file = magicImageFolder.resolve(name + ".png").toFile();

		int i = 1;
		while (file.exists()) {
			file = magicImageFolder.resolve(name + i + ".png").toFile();
			i++;
		}

		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {
			final Alert alert = stageManager.makeExceptionAlert(e,
					"visualisation.magicLayout.view.alerts.couldNotSaveGraphAsImage.content");
			alert.initOwner(this);
			alert.show();
			return;
		}

		injector.getInstance(OpenFile.class).open(file.toPath());
	}
}
