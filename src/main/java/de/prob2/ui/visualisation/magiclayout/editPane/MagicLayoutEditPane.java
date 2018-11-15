package de.prob2.ui.visualisation.magiclayout.editPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visualisation.magiclayout.MagicComponent;
import de.prob2.ui.visualisation.magiclayout.MagicEdges;
import de.prob2.ui.visualisation.magiclayout.MagicNodes;
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

	private abstract class LineListCell<T> extends ListCell<T> {
		{
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}

		@Override
		protected void updateItem(T t, boolean empty) {
			super.updateItem(t, empty);

			if (t == null || empty) {
				setGraphic(null);
			} else {
				Line line = new Line(0, 15, 50, 15);
				defineLineStyle(line, t);
				Group group = new Group();
				group.getChildren().add(line);
				setGraphic(group);
			}
		}

		protected abstract void defineLineStyle(Line line, T t);
	}

	@FXML
	ListView<MagicComponent> listView;
	@FXML
	TextArea expressionTextArea;
	@FXML
	FlowPane flowPane;

	private ComboBox<List<Double>> lineTypeComboBox;
	private ColorPicker lineColorPicker;
	private ComboBox<Double> lineWidthComboBox;
	private ColorPicker textColorPicker;

	final ResourceBundle bundle;
	final CurrentTrace currentTrace;

	@Inject
	public MagicLayoutEditPane(final StageManager stageManager, final ResourceBundle bundle,
			final CurrentTrace currentTrace) {
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "magic_layout_edit_pane.fxml");
	}

	@FXML
	public void initialize() {
		initListView();

		// add general controls
		lineTypeComboBox = new ComboBox<>();
		lineTypeComboBox.getItems().add(new ArrayList<>());
		lineTypeComboBox.getItems().add(Arrays.asList(new Double[] { 12.0, 4.0, 5.0, 4.0 }));
		lineTypeComboBox.getItems().add(Arrays.asList(new Double[] { 2.0, 2.0, 2.0, 2.0 }));
		initLineTypeComboBox();

		lineColorPicker = new ColorPicker(Color.BLACK);

		lineWidthComboBox = new ComboBox<>();
		lineWidthComboBox.getItems().addAll(0.5, 1.0, 2.0, 5.0);
		initLineWidthComboBox();

		textColorPicker = new ColorPicker(Color.BLACK);

		flowPane.getChildren().addAll(
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.linetype"), lineTypeComboBox),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.linecolor"), lineColorPicker),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.linewidth"), lineWidthComboBox),
				wrapInVBox(bundle.getString("visualisation.magicLayout.editPane.labels.textcolor"), textColorPicker));
	}

	void updateValues() {
		if (!listView.getItems().isEmpty() && listView.getSelectionModel().getSelectedItem() == null) {
			listView.getSelectionModel().selectFirst();
		}
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

	void addMagicComponent(MagicComponent component) {
		listView.getItems().add(component);
		listView.getSelectionModel().select(component);
	}

	void addEvalElementsAsGroups(List<IEvalElement> evalElements) {

		Map<IEvalElement, AbstractEvalResult> resultMap = currentTrace.getCurrentState().evalFormulas(evalElements);

		for (IEvalElement element : resultMap.keySet()) {
			MagicComponent magicComponent = (this instanceof MagicLayoutEditNodes)
					? new MagicNodes(element.toString(), resultMap.get(element).toString(), false, true)
					: new MagicEdges(element.toString(), resultMap.get(element).toString(), false);

			if (listView.getItems().contains(magicComponent)) {
				MagicComponent existingComponent = listView.getItems().get(listView.getItems().indexOf(magicComponent));
				existingComponent.expressionProperty().unbind(); // a bound value cannot be set
				existingComponent.expressionProperty().set(resultMap.get(element).toString());
			} else {
				listView.getItems().add(magicComponent);
			}
		}
		this.updateValues();
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

			// define ContextMenu for ListCell
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
				cell.setContextMenu(new ContextMenu());
				cell.setEditable(!to && cell.getItem().isEditable());

				if (cell.isEditable()) {
					cell.getContextMenu().getItems().addAll(editItem, deleteItem, new SeparatorMenuItem());
				}

				cell.getContextMenu().getItems()
						.add((this instanceof MagicLayoutEditNodes) ? newNodesItem : newEdgesItem);

			});

			// set ContextMenu for cells, which are empty from the beginning
			cell.setContextMenu((this instanceof MagicLayoutEditNodes) ? new ContextMenu(newNodesItem)
					: new ContextMenu(newEdgesItem));

			return cell;
		});
		// add ContextMenu to empty ListView
		final MenuItem newNodesItem = new MenuItem(
				bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.newNodes"));
		newNodesItem.setOnAction(event -> ((MagicLayoutEditNodes) this).addNodes());
		final MenuItem newEdgesItem = new MenuItem(
				bundle.getString("visualisation.magicLayout.editPane.listView.contextMenu.newEdges"));
		newEdgesItem.setOnAction(event -> ((MagicLayoutEditEdges) this).addEdges());
		listView.setContextMenu(
				(this instanceof MagicLayoutEditNodes) ? new ContextMenu(newNodesItem) : new ContextMenu(newEdgesItem));

		listView.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> updateValues());
	}

	private void initLineTypeComboBox() {
		// show different line types in ComboBox
		lineTypeComboBox.setCellFactory((ListView<List<Double>> lv) -> new LineListCell<List<Double>>() {
			@Override
			protected void defineLineStyle(Line line, List<Double> style) {
				line.getStrokeDashArray().addAll(style);
				line.setStroke(Color.WHITE);
			}
		});

		lineTypeComboBox.setButtonCell(new LineListCell<List<Double>>() {
			@Override
			protected void defineLineStyle(Line line, List<Double> style) {
				line.getStrokeDashArray().addAll(style);
				line.setStroke(Color.rgb(55, 55, 60));
			}
		});

		lineTypeComboBox.getSelectionModel().selectFirst();
	}

	private void initLineWidthComboBox() {
		// show different line thicknesses in ComboBox (not just double values)
		lineWidthComboBox.setCellFactory((ListView<Double> lv) -> new LineListCell<Double>() {
			@Override
			protected void defineLineStyle(Line line, Double width) {
				line.setStrokeWidth(width);
				line.setStroke(Color.WHITE);
			}
		});

		lineWidthComboBox.setButtonCell(new LineListCell<Double>() {
			@Override
			protected void defineLineStyle(Line line, Double width) {
				line.setStrokeWidth(width);
				line.setStroke(Color.rgb(55, 55, 60));
			}
		});

		lineWidthComboBox.getSelectionModel().select(1.0);
	}
}
