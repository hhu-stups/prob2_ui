package de.prob2.ui.visualisation.magiclayout;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Callback;

public class MagicLayoutEditPane extends VBox {

	@FXML
	ListView<String> listView;
	@FXML
	FlowPane flowPane;
	@FXML
	private ComboBox<String> lineTypeComboBox;
	@FXML
	private ComboBox<Double> lineThicknessComboBox;

	@Inject
	public MagicLayoutEditPane(final StageManager stageManager) {
		stageManager.loadFXML(this, "magic_layout_edit_pane.fxml");
	}

	@FXML
	public void initialize() {
		lineTypeComboBox.getSelectionModel().selectFirst();
		initLineTypeComboBox();

		lineThicknessComboBox.getSelectionModel().selectFirst();
		initLineThicknessComboBox();
	}

	private void initLineTypeComboBox() {
		// show different line types in ComboBox (not just strings)
		lineTypeComboBox.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> l) {
				return new ListCell<String>() {
					{
						setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
					}

					@Override
					protected void updateItem(String style, boolean empty) {
						super.updateItem(style, empty);

						if (style == null || empty) {
							setGraphic(null);
						} else {
							Line line = new Line(0, 15, 50, 15);
							line.setStyle(style);
							line.setStroke(Color.WHITE);
							Group group = new Group();
							group.getChildren().add(line);
							setGraphic(group);
						}
					}
				};
			}
		});
		lineTypeComboBox.setButtonCell(new ListCell<String>() {
			{
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			}

			@Override
			protected void updateItem(String style, boolean empty) {
				super.updateItem(style, empty);

				if (style == null || empty) {
					setGraphic(null);
				} else {
					Line line = new Line(0, 15, 50, 15);
					line.setStyle(style);
					line.setStroke(Color.rgb(55, 55, 60));
					Group group = new Group();
					group.getChildren().add(line);
					setGraphic(group);
				}
			}
		});
	}
	
	private void initLineThicknessComboBox() {
		// show different line thicknesses in ComboBox (not just double values)
		lineThicknessComboBox.setCellFactory(new Callback<ListView<Double>, ListCell<Double>>() {
			@Override
			public ListCell<Double> call(ListView<Double> l) {
				return new ListCell<Double>() {
					{
						setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
					}

					@Override
					protected void updateItem(Double width, boolean empty) {
						super.updateItem(width, empty);

						if (width == null || empty) {
							setGraphic(null);
						} else {
							Line line = new Line(0, 15, 50, 15);
							line.setStrokeWidth(width);
							line.setStroke(Color.WHITE);
							Group group = new Group();
							group.getChildren().add(line);
							setGraphic(group);
						}
					}
				};
			}
		});
		lineThicknessComboBox.setButtonCell(new ListCell<Double>() {
			{
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			}

			@Override
			protected void updateItem(Double width, boolean empty) {
				super.updateItem(width, empty);

				if (width == null || empty) {
					setGraphic(null);
				} else {
					Line line = new Line(0, 15, 50, 15);
					line.setStrokeWidth(width);
					line.setStroke(Color.rgb(55, 55, 60));
					Group group = new Group();
					group.getChildren().add(line);
					setGraphic(group);
				}
			}
		});
	}
	
	VBox wrapInVBox(String caption, Control control) {
		VBox vbox = new VBox();
		Label label = new Label(caption);
		control.setPrefWidth(110);
		vbox.getChildren().addAll(label, control);
		VBox.setMargin(label, new Insets(0, 2, 0, 2));
		return vbox;
	}
}
