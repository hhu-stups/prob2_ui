package de.prob2.ui.visualisation.magiclayout.editpane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visualisation.magiclayout.MagicComponent;
import de.prob2.ui.visualisation.magiclayout.MagicGraphI;
import de.prob2.ui.visualisation.magiclayout.MagicLayoutSettings;
import de.prob2.ui.visualisation.magiclayout.MagicLineType;
import de.prob2.ui.visualisation.magiclayout.MagicLineWidth;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.StringConverter;

public abstract class MagicLayoutEditPane<T extends MagicComponent> extends VBox {

	private abstract class LineListCell<S> extends ListCell<S> {
		{
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}

		@Override
		protected void updateItem(S s, boolean empty) {
			super.updateItem(s, empty);

			if (s == null || empty) {
				setGraphic(null);
			} else {
				Line line = new Line(0, 15, 50, 15);
				defineLineStyle(line, s);
				Group group = new Group();
				group.getChildren().add(line);
				setGraphic(group);
			}
		}

		protected abstract void defineLineStyle(Line line, S s);
	}

	@FXML
	ListView<T> listView;
	@FXML
	TextArea expressionTextArea;
	@FXML
	FlowPane flowPane;

	private ComboBox<MagicLineType> lineTypeComboBox;
	private ColorPicker lineColorPicker;
	private ComboBox<MagicLineWidth> lineWidthComboBox;
	private ColorPicker textColorPicker;

	final StageManager stageManager;
	final ResourceBundle bundle;
	final CurrentTrace currentTrace;
	final MagicGraphI magicGraph;

	@Inject
	public MagicLayoutEditPane(final StageManager stageManager, final ResourceBundle bundle,
			final CurrentTrace currentTrace, final MagicGraphI magicGraph) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.magicGraph = magicGraph;
		stageManager.loadFXML(this, "magic_layout_edit_pane.fxml");
	}

	@FXML
	public void initialize() {
		initListView();

		// add general controls
		lineTypeComboBox = new ComboBox<>();
		lineTypeComboBox.getItems().addAll(magicGraph.getSupportedLineTypes());
		initLineTypeComboBox();

		lineColorPicker = new ColorPicker(Color.BLACK);

		lineWidthComboBox = new ComboBox<>();
		lineWidthComboBox.getItems().addAll(magicGraph.getSupportedLineWidths());
		initLineWidthComboBox();

		textColorPicker = new ColorPicker(Color.BLACK);

		flowPane.getChildren().addAll(
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.linetype"), lineTypeComboBox),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.linecolor"), lineColorPicker),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.linewidth"), lineWidthComboBox),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.textcolor"), textColorPicker));

		// clear listview when the model changes and add machine elements
		currentTrace.modelProperty().addListener((observable, from, to) -> {
			listView.getItems().clear();
			addMachineElements();
			updateValues();
		});
	}

	void updateValues() {
		if (!listView.getItems().isEmpty() && listView.getSelectionModel().getSelectedItem() == null) {
			listView.getSelectionModel().selectFirst();
		}
		updateValues(listView.getSelectionModel().getSelectedItem());
	}

	void updateValues(MagicComponent selectedComponent) {
		listView.getItems().forEach(T::unbindAll);

		if (selectedComponent != null) {
			expressionTextArea.setText(selectedComponent.getExpression());
			selectedComponent.expressionProperty().bind(expressionTextArea.textProperty());

			lineTypeComboBox.setValue(selectedComponent.getLineType());
			selectedComponent.lineTypeProperty().bind(lineTypeComboBox.valueProperty());

			lineColorPicker.setValue(selectedComponent.getLineColor());
			selectedComponent.lineColorProperty().bind(lineColorPicker.valueProperty());

			lineWidthComboBox.setValue(selectedComponent.getLineWidth());
			selectedComponent.lineWidthProperty().bind(lineWidthComboBox.valueProperty());

			textColorPicker.setValue(selectedComponent.getTextColor());
			selectedComponent.textColorProperty().bind(textColorPicker.valueProperty());
		} else {
			expressionTextArea.setText("");
			lineTypeComboBox.getSelectionModel().selectFirst();
			lineColorPicker.setValue(Color.BLACK);
			lineWidthComboBox.getSelectionModel().selectFirst();
			textColorPicker.setValue(Color.BLACK);
		}
	}

	void addMagicComponent(T component) {
		listView.getItems().add(component);
		listView.getSelectionModel().select(component);
	}

	VBox wrapInVBox(String caption, Control control) {
		VBox vbox = new VBox();
		Label label = new Label(caption);
		control.setPrefWidth(115);
		vbox.getChildren().addAll(label, control);
		VBox.setMargin(label, new Insets(0, 2, 0, 2));
		return vbox;
	}

	private void initListView() {
		listView.setEditable(true);
		listView.setCellFactory(lv -> {
			TextFieldListCell<T> cell = new TextFieldListCell<T>() {

				@Override
				public void commitEdit(T component) {
					if (!isEditing()) {
						return;
					}
					if (!listView.getItems().contains(component)
							|| listView.getItems().indexOf(component) == this.getIndex()) {
						super.commitEdit(component);
					}
				}
			};

			cell.setConverter(new StringConverter<T>() {

				@Override
				public String toString(T component) {
					return component.toString();
				}

				@Override
				public T fromString(String string) {
					T component = getInstance(cell.getItem());
					component.nameProperty().set(string);
					return component;
				}
			});

			// define ContextMenu for ListCell
			final MenuItem editItem = new MenuItem(
					bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.rename"));
			editItem.setOnAction(event -> cell.startEdit());

			final MenuItem deleteItem = new MenuItem(
					bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.delete"));
			deleteItem.setOnAction(event -> {
				List<ButtonType> buttons = new ArrayList<>();
				buttons.add(ButtonType.YES);
				buttons.add(ButtonType.NO);
				Optional<ButtonType> result = stageManager.makeAlert(AlertType.CONFIRMATION, buttons, "",
						"visualisation.magicLayout.editPane.alerts.confirmDeleteComponent.content",
						cell.getItem().getName()).showAndWait();
				if (result.isPresent() && result.get().equals(ButtonType.YES)) {
					listView.getItems().remove(cell.getItem());
				}
			});

			final MenuItem newNodesItem = new MenuItem(
					bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.newNodes"));
			newNodesItem.setOnAction(event -> ((MagicLayoutEditNodes) this).addNewNodegroup());

			final MenuItem newEdgesItem = new MenuItem(
					bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.newEdges"));
			newEdgesItem.setOnAction(event -> ((MagicLayoutEditEdges) this).addNewEdgegroup());

			cell.emptyProperty().addListener((observable, from, to) -> {
				cell.setContextMenu(new ContextMenu());
				cell.setEditable(!to);

				if (cell.isEditable()) {
					cell.getContextMenu().getItems().addAll(editItem, deleteItem, new SeparatorMenuItem());
				}

				cell.getContextMenu().getItems()
						.add((this instanceof MagicLayoutEditNodes) ? newNodesItem : newEdgesItem);

			});

			// set ContextMenu for cells, which are empty from the beginning
			cell.setContextMenu((this instanceof MagicLayoutEditNodes) ? new ContextMenu(newNodesItem)
					: new ContextMenu(newEdgesItem));

			// init drag and drop for reordering list cells
			cell.setOnDragDetected(event -> {
				if (cell.getItem() == null) {
					return;
				}

				Dragboard dragboard = cell.startDragAndDrop(TransferMode.MOVE);

				ClipboardContent content = new ClipboardContent();
				content.putString(cell.getItem().getName());
				dragboard.setContent(content);

				dragboard.setDragView(cell.snapshot(null, null));

				event.consume();
			});

			cell.setOnDragOver(event -> {
				if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
					event.acceptTransferModes(TransferMode.MOVE);
				}

				event.consume();
			});

			cell.setOnDragEntered(event -> {
				if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
					cell.setTextFill(Color.GREY);
				}
				event.consume();
			});

			cell.setOnDragExited(event -> {
				cell.setTextFill(Color.BLACK);
				event.consume();
			});

			cell.setOnDragDropped(event -> {
				Dragboard dragboard = event.getDragboard();

				boolean success = false;
				if (dragboard.hasString() && !cell.isEmpty()) {
					T draggedComponent = null;
					for (T component : listView.getItems()) {
						if (component.getName().equals(dragboard.getString())) {
							draggedComponent = getInstance(component);
							break;
						}
					}
					
					if (draggedComponent == null) {
						return;
					}
					int targetIndex = cell.getIndex();
					int sourceIndex = listView.getItems().indexOf(draggedComponent);
					if (targetIndex > sourceIndex) {
						// move all items between source and target one up
						for (int i = sourceIndex; i < targetIndex; i++) {
							T component = listView.getItems().get(i + 1);
							listView.getItems().set(i, component);
						}
					} else {
						// move all items between source and target one down
						for (int i = sourceIndex; i > targetIndex; i--) {
							T component = listView.getItems().get(i - 1);
							listView.getItems().set(i, component);
						}
					}
					listView.getItems().set(targetIndex, draggedComponent);
					success = true;
				}
				event.setDropCompleted(success);

				event.consume();
			});

			return cell;
		});

		// add ContextMenu to empty ListView
		final MenuItem newNodesItem = new MenuItem(
				bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.newNodes"));
		newNodesItem.setOnAction(event -> ((MagicLayoutEditNodes) this).addNewNodegroup());
		final MenuItem newEdgesItem = new MenuItem(
				bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.newEdges"));
		newEdgesItem.setOnAction(event -> ((MagicLayoutEditEdges) this).addNewEdgegroup());
		listView.setContextMenu(
				(this instanceof MagicLayoutEditNodes) ? new ContextMenu(newNodesItem) : new ContextMenu(newEdgesItem));

		listView.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			updateValues();
			disableControls(to == null);
		});
	}

	void disableControls(boolean disable) {
		expressionTextArea.setDisable(disable);
		lineTypeComboBox.setDisable(disable);
		lineColorPicker.setDisable(disable);
		lineWidthComboBox.setDisable(disable);
		textColorPicker.setDisable(disable);
	}

	private void initLineTypeComboBox() {
		// show different line types in ComboBox
		lineTypeComboBox.setCellFactory((ListView<MagicLineType> lv) -> new LineListCell<MagicLineType>() {
			@Override
			protected void defineLineStyle(Line line, MagicLineType style) {
				line.getStrokeDashArray().addAll(style.getDashArrayList());
				line.setStroke(Color.WHITE);
			}
		});

		lineTypeComboBox.setButtonCell(new LineListCell<MagicLineType>() {
			@Override
			protected void defineLineStyle(Line line, MagicLineType style) {
				line.getStrokeDashArray().addAll(style.getDashArrayList());
				line.setStroke(Color.rgb(55, 55, 60));
			}
		});

		lineTypeComboBox.getSelectionModel().selectFirst();
	}

	private void initLineWidthComboBox() {
		// show different line thicknesses in ComboBox (not just double values)
		lineWidthComboBox.setCellFactory((ListView<MagicLineWidth> lv) -> new LineListCell<MagicLineWidth>() {
			@Override
			protected void defineLineStyle(Line line, MagicLineWidth width) {
				line.setStrokeWidth(width.getWidth());
				line.setStroke(Color.WHITE);
			}
		});

		lineWidthComboBox.setButtonCell(new LineListCell<MagicLineWidth>() {
			@Override
			protected void defineLineStyle(Line line, MagicLineWidth width) {
				line.setStrokeWidth(width.getWidth());
				line.setStroke(Color.rgb(55, 55, 60));
			}
		});

		lineWidthComboBox.getSelectionModel().select(MagicLineWidth.DEFAULT);
	}

	protected abstract T getInstance(T component);

	abstract void addMachineElements();

	public abstract void openLayoutSettings(MagicLayoutSettings layoutSettings);
}
