package de.prob2.ui.visualisation.magiclayout;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
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

	@Inject
	public MagicLayoutEditPane(final StageManager stageManager) {
		stageManager.loadFXML(this, "magic_layout_edit_pane.fxml");
	}

	@FXML
	public void initialize() {
		// show different line types in ComboBox (not just strings)
		lineTypeComboBox.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> p) {
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
}
