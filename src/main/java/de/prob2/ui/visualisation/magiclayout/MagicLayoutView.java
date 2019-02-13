package de.prob2.ui.visualisation.magiclayout;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.representation.AbstractModel;
import de.prob.statespace.Trace;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visualisation.magiclayout.editpane.MagicLayoutEditEdges;
import de.prob2.ui.visualisation.magiclayout.editpane.MagicLayoutEditNodes;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.util.StringConverter;

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

	private final StageManager stageManager;
	private final MagicGraphI magicGraph;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final MagicLayoutSettingsManager settingsManager;
	private final ResourceBundle bundle;

	@Inject
	public MagicLayoutView(final StageManager stageManager, final MagicGraphI magicGraph,
			final CurrentTrace currentTrace, final CurrentProject currentProject,
			final MagicLayoutSettingsManager settingsManager, final ResourceBundle bundle) {
		this.stageManager = stageManager;
		this.magicGraph = magicGraph;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.settingsManager = settingsManager;
		this.bundle = bundle;
		stageManager.loadFXML(this, "magic_layout_view.fxml");
	}

	@FXML
	public void initialize() {
		stageManager.setMacMenuBar(this, menuBar);
		helpButton.setHelpContent(this.getClass());

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
		layoutChoiceBox.setConverter(new StringConverter<MagicLayout>() {
				@Override
				public String toString(MagicLayout layout) {
					return bundle.getString(layout.getBundleKey());
				}

				@Override
				public MagicLayout fromString(String string) {
					if (string.equals(bundle.getString(MagicLayout.RANDOM.getBundleKey()))) {
						return MagicLayout.RANDOM;
					} else {
						return MagicLayout.LAYERED;
					}
				}
			});
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
		magicGraphPane.getChildren().setAll(magicGraph.generateMagicGraph(currentTrace.getCurrentState(), layoutChoiceBox.getSelectionModel().getSelectedItem()));
		magicGraph.setGraphStyle(magicLayoutEditNodes.getNodegroups(), magicLayoutEditEdges.getEdgegroups());
	}

	@FXML
	private void updateGraph() {
		magicGraph.updateMagicGraph(currentTrace.getCurrentState());
		magicGraph.setGraphStyle(magicLayoutEditNodes.getNodegroups(), magicLayoutEditEdges.getEdgegroups());
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
		MagicLayoutSettings layoutSettings = new MagicLayoutSettings(currentProject.getCurrentMachine().getName(),
				magicLayoutEditNodes.getNodegroups(), magicLayoutEditEdges.getEdgegroups());
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
			stageManager.makeExceptionAlert(e,
					"visualisation.magicLayout.view.alerts.couldNotSaveGraphAsImage.content");
		}

		// try to open image with external viewer
		try {
			Desktop.getDesktop().open(file);
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "visualisation.magicLayout.view.alerts.couldNotOpenImage.content");
		}
		
		zoom(zoomFactor);
	}
}
