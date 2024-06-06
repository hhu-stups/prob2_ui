package de.prob2.ui.visb;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob2.ui.dynamic.DynamicVisualizationStage;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.ui.ListViewEvent;
import de.prob2.ui.visb.ui.VisBTableItemCell;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

	private final StageManager stageManager;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final I18n i18n;

	private final VisBController visBController;

	private final Injector injector;

	@FXML
	private TableView<VisBTableItem> visBItems;

	@FXML
	private TableColumn<VisBTableItem, CheckBox> selectedColumn;

	@FXML
	private TableColumn<VisBTableItem, String> itemColumn;

	@FXML
	private ListView<VisBEvent> visBEvents;

	private final Map<String, VisBEvent> eventsById;

	private final CheckBox selectAll;

	@Inject
	public VisBDebugStage(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject, final I18n i18n, final VisBController visBController, final Injector injector) {
		super();
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.i18n = i18n;
		this.visBController = visBController;
		this.injector = injector;
		this.eventsById = new HashMap<>();
		this.selectAll = new CheckBox();
		this.stageManager.loadFXML(this, "visb_debug_stage.fxml");
	}

	@FXML
	public void initialize() {
		visBController.visBVisualisationProperty().addListener((o, from, to) -> this.initialiseListViews(to));
		
		ChangeListener<VisBTableItem> listener = (observable, from, to) -> {
			if(from != null) {
				removeHighlighting(from);
			}
			if(to != null) {
				applyHighlighting(to);
			}
		};

		this.selectedColumn.setCellValueFactory(new VisBSelectionCell(visBItems, selectAll));
		this.selectedColumn.setGraphic(selectAll);

		this.itemColumn.setCellFactory(param -> new VisBTableItemCell(stageManager, i18n, injector, eventsById, visBController.getAttributeValues()));
		this.itemColumn.setCellValueFactory(features -> new SimpleStringProperty(""));

		this.visBEvents.setCellFactory(lv -> new ListViewEvent(stageManager, i18n, injector));
		this.currentTrace.addListener((observable, from, to) -> refresh());
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> refresh());
		this.visBItems.getSelectionModel().selectedItemProperty().addListener(listener);
		this.setOnCloseRequest(e -> this.visBItems.getSelectionModel().clearSelection());
	}

	private void removeHighlighting(VisBTableItem item) {
		String id = item.getVisBItem().getId();
		if(eventsById.containsKey(id)) {
			for (VisBHover hover : eventsById.get(id).getHovers()) {
				injector.getInstance(VisBView.class).changeAttribute(hover.getHoverID(), hover.getHoverAttr(), hover.getHoverLeaveVal());
			}
		}
	}

	private void applyHighlighting(VisBTableItem item) {
		String id = item.getVisBItem().getId();
		if(eventsById.containsKey(id)) {
			for (VisBHover hover : eventsById.get(id).getHovers()) {
				injector.getInstance(VisBView.class).changeAttribute(hover.getHoverID(), hover.getHoverAttr(), hover.getHoverEnterVal());
			}
		}
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
			this.eventsById.putAll(visBVisualisation.getEventsById());
		}
	}

	public void clear(){
		this.visBEvents.setItems(null);
		this.visBItems.setItems(null);
		this.eventsById.clear();
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
		DynamicVisualizationStage formulaStage = injector.getInstance(DynamicVisualizationStage.class);
		formulaStage.show();
		formulaStage.toFront();
		formulaStage.visualizeProjection(projectionString);
	}
}
