package de.prob2.ui.history;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.FormalismType;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.MachineCreator;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableIntegerValue;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

@FXMLInjected
@Singleton
public final class HistoryView extends VBox {
	private final class TransitionRow extends TableRow<HistoryItem> {
		private TransitionRow() {
			super();

			this.setOnMouseClicked(event -> {
				final Trace trace = currentTrace.get();
				if (!this.isEmpty() && trace != null && MouseButton.PRIMARY.equals(event.getButton())) {
					currentTrace.set(trace.gotoPosition(this.getItem().getIndex()));
				}
			});
		}

		@Override
		protected void updateItem(HistoryItem item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll(Arrays.asList("past", "present", "future"));
			if (!empty) {
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
			} else {
				this.setCursor(Cursor.DEFAULT);
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
	private Button saveTraceButton;
	@FXML
	private HelpButton helpButton;

	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final CurrentProject currentProject;

	@Inject
	private HistoryView(StageManager stageManager, CurrentTrace currentTrace, Injector injector,
			CurrentProject currentProject) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "history_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("history", null);

		historyTableView.setRowFactory(item -> new TransitionRow());
		historyTableView.getSelectionModel().setCellSelectionEnabled(true);
		positionColumn.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getIndex() + 1));
		transitionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().toPrettyString()));

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
			historyTableView.getItems().clear();
			if (to != null) {
				historyTableView.getItems().addAll(HistoryItem.itemsForTrace(to));
				historyTableView.sort();
			}
		};
		traceChangeListener.changed(currentTrace, null, currentTrace.get());
		currentTrace.addListener(traceChangeListener);

		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);

		saveTraceButton.disableProperty()
				.bind(partOfDisableBinding.or(currentProject.isNotNull().and(currentTrace.isNotNull()).not()));
	}

	public NumberBinding getCurrentHistoryPositionProperty() {
		return Bindings.createIntegerBinding(
				() -> currentTrace.get() == null ? 0 : currentTrace.get().getCurrent().getIndex() + 1, currentTrace);
	}

	public ObservableIntegerValue getObservableHistorySize() {
		return Bindings.createIntegerBinding(() -> Math.max(this.historyTableView.itemsProperty().get().size() - 1, 0),
				historyTableView.itemsProperty().get());
	}

	@FXML
	private void saveTrace() {

		TraceFileHandler traceSaver = injector.getInstance(TraceFileHandler.class);
		Trace copyTrace = currentTrace.get();
		if (currentTrace.get() != null) {
			try {


				Path machinePath = currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation());
				LoadedMachine loadedMachine = injector.getInstance(MachineCreator.class).load(machinePath);
				traceSaver.save(
					new PersistentTrace(currentTrace.get(), currentTrace.get().getCurrent().getIndex() + 1),
					currentProject.getCurrentMachine(),
					loadedMachine);
			} catch (Exception e) {
				TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, "history.buttons.saveTrace.error.msg", TraceReplayErrorAlert.Trigger.TRIGGER_HISTORY_VIEW, Collections.EMPTY_LIST);
				alert.initOwner(this.getScene().getWindow());
				alert.setCopyTrace(copyTrace);
				alert.setErrorMessage();
			}
		}
	}
}
