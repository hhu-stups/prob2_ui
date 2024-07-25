package de.prob2.ui.visb;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob2.ui.dynamic.DynamicVisualizationStage;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

@Singleton
public final class VisBDebugStage extends Stage {
	private static final class VisBSelectionCell implements Callback<TableColumn.CellDataFeatures<VisBTableItem, CheckBox>, ObservableValue<CheckBox>> {

		private final CheckBox selectAll;

		public VisBSelectionCell(final TableView<VisBTableItem> tableView, final CheckBox selectAll) {
			this.selectAll = selectAll;

			this.selectAll.setSelected(true);
			// selectAll can be (de)selected manually by the user or automatically when one of the individual checkboxes is (de)selected.
			// We want to update the individual checkboxes only if selectAll was (de)selected by the user, so we use the onAction callback instead of a listener on the selected property.
			this.selectAll.setOnAction(e -> {
				// Changing an item's selected state will automatically (de)select selectAll,
				// which can overwrite the selection change from the user.
				// So we need to remember the selection state immediately after the user has changed it and before it is automatically overwritten in the loop.
				// Once the loop has finished, all items are either selected or deselected,
				// and the selectAll state will automatically update back to what the user has selected.
				final boolean selected = this.selectAll.isSelected();
				for (VisBTableItem it : tableView.getItems()) {
					it.setSelected(selected);
				}
			});
		}

		@Override
		public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<VisBTableItem, CheckBox> param) {
			VisBTableItem item = param.getValue();
			CheckBox checkBox = new CheckBox();
			checkBox.selectedProperty().bindBidirectional(item.selectedProperty());
			item.selectedProperty().addListener(o ->
					this.selectAll.setSelected(param.getTableView().getItems().stream().anyMatch(VisBTableItem::isSelected))
			);
			return new SimpleObjectProperty<>(checkBox);
		}
	}

	private final class VisBTableItemCell extends TableCell<VisBTableItem, VisBItem> {
		@FXML
		private VBox itemBox;
		@FXML
		private Label lbID;
		@FXML
		private Label lbExpression;
		@FXML
		private Label lbAttribute;
		@FXML
		private Label lbValue;

		private VisBTableItemCell() {
			stageManager.loadFXML(this, "visb_debug_item_cell.fxml");
		}

		@FXML
		private void initialize() {
			this.hoverProperty().addListener((observable, from, to) -> {
				if (!this.isEmpty()) {
					var eventsById = visBController.getVisBVisualisation().getEventsById();
					String id = this.getItem().getId();
					if (eventsById.containsKey(id)) {
						for (VisBHover hover : eventsById.get(id).getHovers()) {
							visBView.changeAttribute(hover.getHoverID(), hover.getHoverAttr(), to ? hover.getHoverEnterVal() : hover.getHoverLeaveVal());
						}
					}
				}
			});
		}

		@Override
		protected void updateItem(VisBItem item, boolean empty) {
			super.updateItem(item, empty);

			this.setText("");
			this.setGraphic(this.itemBox);
			if (!empty) {
				this.lbID.setText(item.getId());
				this.lbAttribute.setText(i18n.translate("visb.item.attribute", item.getAttribute()));
				this.lbExpression.setText(i18n.translate("visb.item.expression", item.getExpression()));
				final StringExpression valueBinding = Bindings.stringValueAt(visBController.getAttributeValues(), item.getKey());
				this.lbValue.textProperty().bind(i18n.translateBinding("visb.item.value",
						Bindings.when(valueBinding.isNull())
								.then(i18n.translateBinding("visb.item.value.notInitialized"))
								.otherwise(i18n.translateBinding("common.quoted", valueBinding))
				));
			} else {
				this.lbID.setText("");
				this.lbAttribute.setText("");
				this.lbExpression.setText("");
				this.lbValue.textProperty().unbind();
				this.lbValue.setText("");
			}
		}
	}

	private final class VisBEventCell extends ListCell<VisBEvent> {
		@FXML
		private VBox eventBox;
		@FXML
		private Label lbID;
		@FXML
		private Label lbEvent;
		@FXML
		private Label lbPredicates;

		private VisBEventCell() {
			stageManager.loadFXML(this,"visb_debug_event_cell.fxml");
		}

		@FXML
		private void initialize() {
			this.hoverProperty().addListener((observable, from, to) -> {
				if (!this.isEmpty()) {
					for (VisBHover hover : this.getItem().getHovers()) {
						visBView.changeAttribute(hover.getHoverID(), hover.getHoverAttr(), to ? hover.getHoverEnterVal() : hover.getHoverLeaveVal());
					}
				}
			});
		}

		@Override
		protected void updateItem(VisBEvent visBEvent, boolean empty) {
			super.updateItem(visBEvent, empty);

			this.setText("");
			this.setGraphic(this.eventBox);
			if (visBEvent != null) {
				this.lbID.setText(visBEvent.getId());
				this.lbEvent.setText(i18n.translate("visb.event.event", visBEvent.getEvent()));
				this.lbPredicates.setText(i18n.translate("visb.event.predicates", visBEvent.getPredicates().toString()));
			} else {
				this.lbID.setText("");
				this.lbEvent.setText("");
				this.lbPredicates.setText("");
			}
		}
	}

	private final StageManager stageManager;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final I18n i18n;

	private final VisBController visBController;

	private final VisBView visBView;

	private final Provider<DynamicVisualizationStage> dynamicVisualizationStageProvider;

	@FXML
	private TableView<VisBTableItem> visBItems;

	@FXML
	private TableColumn<VisBTableItem, CheckBox> selectedColumn;

	@FXML
	private TableColumn<VisBTableItem, VisBItem> itemColumn;

	@FXML
	private ListView<VisBEvent> visBEvents;

	private final CheckBox selectAll;

	@Inject
	private VisBDebugStage(
		StageManager stageManager,
		CurrentTrace currentTrace,
		CurrentProject currentProject,
		I18n i18n,
		VisBController visBController,
		VisBView visBView,
		Provider<DynamicVisualizationStage> dynamicVisualizationStageProvider
	) {
		super();
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.i18n = i18n;
		this.visBController = visBController;
		this.visBView = visBView;
		this.dynamicVisualizationStageProvider = dynamicVisualizationStageProvider;

		this.selectAll = new CheckBox();
		this.stageManager.loadFXML(this, "visb_debug_stage.fxml");
	}

	@FXML
	public void initialize() {
		this.selectedColumn.setCellValueFactory(new VisBSelectionCell(visBItems, selectAll));
		this.selectedColumn.setGraphic(selectAll);

		this.itemColumn.setCellFactory(param -> new VisBTableItemCell());
		this.itemColumn.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getVisBItem()));

		this.visBEvents.setCellFactory(lv -> new VisBEventCell());

		visBController.visBVisualisationProperty().addListener((o, from, to) -> this.initialiseListViews(to));
		this.initialiseListViews(visBController.getVisBVisualisation());

		this.currentTrace.addListener((observable, from, to) -> refresh());
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> refresh());
	}

	/**
	 * After loading the JSON/ VisB file and preparing it in the {@link VisBController} the ListViews are initialised.
	 * @param visBVisualisation is needed to display the items and events in the ListViews
	 */
	private void initialiseListViews(VisBVisualisation visBVisualisation){
		clear();
		if (visBVisualisation != null) {
			this.visBEvents.setItems(FXCollections.observableArrayList(visBVisualisation.getEvents()));
			this.visBItems.setItems(FXCollections.observableArrayList());
			for (VisBItem item : visBVisualisation.getItems()) {
				this.visBItems.getItems().add(new VisBTableItem(item));
			}
		}
	}

	public void clear(){
		this.visBEvents.setItems(null);
		this.visBItems.setItems(null);
	}

	private void refresh() {
		visBItems.refresh();
		visBEvents.refresh();
	}

	@FXML
	private void showProjection() {
		VisBVisualisation visBVisualisation = this.visBController.getVisBVisualisation();
		if(visBVisualisation == null) {
			return;
		}
		List<VisBTableItem> visBItems = this.visBItems.getItems();
		String projectionString = visBItems.stream()
				.filter(VisBTableItem::isSelected)
				.map(VisBTableItem::getVisBItem)
				.map(item -> String.format(Locale.ROOT, "\"%s_%s\" |-> %s", item.getId(), item.getAttribute(), item.getExpression()))
				.collect(Collectors.joining(" |-> \n"));
		DynamicVisualizationStage formulaStage = dynamicVisualizationStageProvider.get();
		formulaStage.show();
		formulaStage.toFront();
		formulaStage.visualizeProjection(projectionString);
	}
}
