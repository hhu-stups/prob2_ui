package de.prob2.ui.history;

import java.io.IOException;
import java.util.Arrays;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.operations.OperationDetailsStage;
import de.prob2.ui.operations.OperationItem;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableIntegerValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;

@FXMLInjected
@Singleton
public final class HistoryView extends BorderPane {
	private final class TransitionRow extends TableRow<HistoryItem> {
		private TransitionRow() {
			super();

			this.setOnMouseClicked(event -> {
				final Trace trace = currentTrace.get();
				if (!this.isEmpty() && trace != null && MouseButton.PRIMARY.equals(event.getButton())) {
					currentTrace.set(trace.gotoPosition(this.getItem().getIndex()));
					lastSelectedIndex = this.getItem().getIndex() + 1;
				}
			});
		}

		@Override
		protected void updateItem(HistoryItem item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll(Arrays.asList("past", "present", "future"));
			if (empty || item == null) {
				this.setCursor(Cursor.DEFAULT);
				this.setContextMenu(null);
				this.setTooltip(null);
			} else {
				this.setCursor(Cursor.HAND);
				final Trace trace = currentTrace.get();
				if (trace != null) {
					final int currentIndex = trace.getCurrent().getIndex();
					if (item.getIndex() < currentIndex) {
						this.getStyleClass().add("past");
					} else if (item.getIndex() > currentIndex) {
						this.getStyleClass().add("future");
					} else {
						this.getStyleClass().add("present");
					}
				}

				OperationItem opItem = item.getOperation();
				final MenuItem showDetailsItem = new MenuItem(i18n.translate("operations.operationsView.contextMenu.items.showDetails"));
				showDetailsItem.setOnAction(event -> {
					final OperationDetailsStage stage = injector.getInstance(OperationDetailsStage.class);
					stage.setItem(opItem);
					stage.show();
				});
				// The root state doesn't have a corresponding operation
				showDetailsItem.setDisable(item.getOperation() == null);

				this.setContextMenu(new ContextMenu(showDetailsItem));
				this.setTooltip(opItem != null && !opItem.getDescription().isEmpty() ? new Tooltip(opItem.getDescription()) : null);
			}
		}
	}

	@FXML
	private TableView<HistoryItem> historyTableView;
	@FXML
	private TableColumn<HistoryItem, Integer> positionColumn;
	@FXML
	private TableColumn<HistoryItem, String> transitionColumn;
	@FXML
	private TableColumn<HistoryItem, String> descriptionColumn;
	@FXML
	private Button openTraceSelectionButton;
	@FXML
	private MenuButton saveTraceButton;
	@FXML
	private HelpButton helpButton;

	private final CurrentTrace currentTrace;
	private final I18n i18n;
	private final Injector injector;
	private final CurrentProject currentProject;
	private final TraceFileHandler traceFileHandler;
	private int lastSelectedIndex = -1;

	@Inject
	private HistoryView(
		StageManager stageManager,
		I18n i18n,
		CurrentTrace currentTrace,
		Injector injector,
		CurrentProject currentProject,
		TraceFileHandler traceFileHandler
	) {
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.currentProject = currentProject;
		this.traceFileHandler = traceFileHandler;
		stageManager.loadFXML(this, "history_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("history", null);

		historyTableView.setRowFactory(item -> new TransitionRow());
		historyTableView.getSelectionModel().setCellSelectionEnabled(true);
		historyTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
		positionColumn.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getIndex() + 1));
		transitionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().toPrettyString()));
		descriptionColumn.setCellValueFactory(cellData ->
				new SimpleStringProperty(cellData.getValue().getOperation() != null
						? cellData.getValue().getOperation().getDescription()
						: ""));
		descriptionColumn.prefWidthProperty().bind(transitionColumn.prefWidthProperty());
		historyTableView.getItems().addListener((ListChangeListener<HistoryItem>) change -> {
			boolean allEmpty = historyTableView.getItems().stream()
					.map(item -> descriptionColumn.getCellData(item))
					.allMatch(value -> value == null || value.isEmpty());
			descriptionColumn.setVisible(!allEmpty);
		});

		this.setOnKeyPressed(event -> {
			final Trace trace = currentTrace.get();
			final HistoryItem selected = historyTableView.getSelectionModel().getSelectedItem();
			if (event.getCode().equals(KeyCode.ENTER) && selected != null && trace != null) {
				currentTrace.set(trace.gotoPosition(selected.getIndex()));
				lastSelectedIndex = selected.getIndex() + 1;
			}
		});

		// disable horizontal scrollbar
		// needs a hack because of a missing feature: https://bugs.openjdk.org/browse/JDK-8090721
		historyTableView.getItems().addListener((ListChangeListener<? super HistoryItem>) c -> {
			if (historyTableView.queryAccessibleAttribute(AccessibleAttribute.HORIZONTAL_SCROLLBAR) instanceof ScrollBar hsb) {
				hsb.setPrefHeight(0);
				hsb.setMaxHeight(0);
				hsb.setMouseTransparent(true);
				hsb.setVisible(false);
			}
		});
		historyTableView.addEventFilter(ScrollEvent.ANY, event -> {
			if (event.getDeltaX() != 0) {
				event.consume();
			}
		});

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
			if (to != null) {
				var newItems = HistoryItem.itemsForTrace(to);
				var current = newItems.get(to.getCurrent().getIndex() + 1);
				var currentIsLast = to.getCurrent().getIndex() + 1 == newItems.size() - 1;
				historyTableView.getItems().setAll(newItems);
				historyTableView.sort();
				historyTableView.getSelectionModel().focus(lastSelectedIndex);
				if (currentIsLast) {
					historyTableView.scrollTo(current);
				}
			} else {
				historyTableView.getItems().clear();
			}
		};
		traceChangeListener.changed(currentTrace, null, currentTrace.get());
		currentTrace.addListener(traceChangeListener);

		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		saveTraceButton.disableProperty()
				.bind(partOfDisableBinding.or(currentProject.isNotNull().and(currentTrace.isNotNull()).not()));
	}

	public ObservableIntegerValue getCurrentHistoryPositionProperty() {
		return Bindings.createIntegerBinding(
				() -> currentTrace.get() == null ? 0 : currentTrace.get().getCurrent().getIndex() + 1, currentTrace);
	}

	public ObservableIntegerValue getObservableHistorySize() {
		return Bindings.createIntegerBinding(() -> Math.max(this.historyTableView.getItems().size() - 1, 0), historyTableView.getItems());
	}

	@FXML
	private void saveTrace() {
		try {
			traceFileHandler.save(currentTrace.get(), currentProject.getCurrentMachine());
		} catch (IOException | RuntimeException e) {
			traceFileHandler.showSaveError(e);
		}
	}

	@FXML
	private void saveTraceAsTable() {
		try {
			traceFileHandler.saveAsTable(currentTrace.get());
		} catch (IOException | RuntimeException e) {
			traceFileHandler.showSaveError(e);
		}
	}
}
