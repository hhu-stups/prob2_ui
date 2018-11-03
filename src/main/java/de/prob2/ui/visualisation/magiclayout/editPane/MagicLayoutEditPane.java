package de.prob2.ui.visualisation.magiclayout.editPane;

import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visualisation.magiclayout.MagicComponent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.StringConverter;

public class MagicLayoutEditPane extends VBox {

	@FXML
	ListView<MagicComponent> listView;
	@FXML
	TextArea expressionTextArea;
	@FXML
	FlowPane flowPane;
	@FXML
	private ComboBox<String> lineTypeComboBox;
	@FXML
	private ColorPicker lineColorPicker;
	@FXML
	private ComboBox<Double> lineWidthComboBox;

	final ResourceBundle bundle;

	@Inject
	public MagicLayoutEditPane(final StageManager stageManager, final ResourceBundle bundle) {
		this.bundle = bundle;
		stageManager.loadFXML(this, "magic_layout_edit_pane.fxml");
	}

	@FXML
	public void initialize() {
		listView.setEditable(true);
		listView.setCellFactory(lv -> {
			TextFieldListCell<MagicComponent> cell = new TextFieldListCell<>();

			cell.setConverter(new StringConverter<MagicComponent>() {

				@Override
				public String toString(MagicComponent component) {
					return component.toString();
				}

				@Override
				public MagicComponent fromString(String string) {
					MagicComponent component = cell.getItem();
					component.nameProperty().set(string);
					return component;
				}

			});

			final MenuItem editItem = new MenuItem(
					bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.rename"));
			editItem.setOnAction(event -> cell.startEdit());

			final MenuItem deleteItem = new MenuItem(
					bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.delete"));
			deleteItem.setOnAction(event -> listView.getItems().remove(cell.getItem()));

			final MenuItem newNodesItem = new MenuItem(
					bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.newNodes"));
			newNodesItem.setOnAction(event -> ((MagicLayoutEditNodes) this).addNodes());

			final MenuItem newEdgesItem = new MenuItem(
					bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.newEdges"));
			newEdgesItem.setOnAction(event -> ((MagicLayoutEditEdges) this).addEdges());

			cell.emptyProperty().addListener((observable, from, to) -> {
				if (to) {
					cell.setContextMenu((this instanceof MagicLayoutEditNodes) ? new ContextMenu(newNodesItem)
							: new ContextMenu(newEdgesItem));
				} else {
					cell.setContextMenu(new ContextMenu(editItem, deleteItem, new SeparatorMenuItem()));
					cell.getContextMenu().getItems()
							.add((this instanceof MagicLayoutEditNodes) ? newNodesItem : newEdgesItem);

				}
			});

			// set ContextMenu for cells, which are empty from the beginning
			cell.setContextMenu((this instanceof MagicLayoutEditNodes) ? new ContextMenu(newNodesItem)
					: new ContextMenu(newEdgesItem));

			return cell;
		});
		listView.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> updateValues());

		lineTypeComboBox.getSelectionModel().selectFirst();
		initLineTypeComboBox();

		lineWidthComboBox.getSelectionModel().select(1.0);
		initLineWidthComboBox();
	}

	private void initLineTypeComboBox() {
		// show different line types in ComboBox (not just strings)
		lineTypeComboBox.setCellFactory((ListView<String> lv) -> new ListCell<String>() {
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

	private void initLineWidthComboBox() {
		// show different line thicknesses in ComboBox (not just double values)
		lineWidthComboBox.setCellFactory((ListView<Double> lv) -> new ListCell<Double>() {
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
		});
		lineWidthComboBox.setButtonCell(new ListCell<Double>() {
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
		control.setPrefWidth(115);
		vbox.getChildren().addAll(label, control);
		VBox.setMargin(label, new Insets(0, 2, 0, 2));
		return vbox;
	}

	@FXML
	private void updateValues() {
		updateValues(listView.getSelectionModel().getSelectedItem());
	}

	void updateValues(MagicComponent selectedComponent) {
		listView.getItems().forEach(i -> i.unbindAll());

		if (selectedComponent != null) {
			expressionTextArea.setText(selectedComponent.getExpression());
			selectedComponent.expressionProperty().bind(expressionTextArea.textProperty());

			lineTypeComboBox.setValue(selectedComponent.getLineType());
			selectedComponent.lineTypeProperty().bind(lineTypeComboBox.valueProperty());

			lineColorPicker.setValue(selectedComponent.getLineColor());
			selectedComponent.lineColorProperty().bind(lineColorPicker.valueProperty());

			lineWidthComboBox.setValue(selectedComponent.getLineWidth());
			selectedComponent.lineWidthProperty().bind(lineWidthComboBox.valueProperty());
		}
	}

	void addMagicComponent(MagicComponent component) {
		listView.getItems().add(component);
		listView.getSelectionModel().select(component);
	}
}
