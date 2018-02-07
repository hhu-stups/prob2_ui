package de.prob2.ui.history;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.tracereplay.TraceSaver;

import javafx.beans.binding.Bindings;
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
import javafx.scene.layout.AnchorPane;

@Singleton
public final class HistoryView extends AnchorPane {
	private final class TransitionRow extends TableRow<HistoryItem> {
		private TransitionRow() {
			super();

			this.setOnMouseClicked(event -> {
				final Trace trace = currentTrace.get();
				if (trace != null && this.getItem() != null && MouseButton.PRIMARY.equals(event.getButton())) {
					currentTrace.set(trace.gotoPosition(this.getItem().getIndex()));
				}
			});
		}

		@Override
		protected void updateItem(HistoryItem item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll(Arrays.asList("past", "present", "future"));
			if (!empty && item != null) {
				switch (item.getStatus()) {
				case PAST:
					this.getStyleClass().add("past");
					break;

				case FUTURE:
					this.getStyleClass().add("future");
					break;

				default:
					this.getStyleClass().add("present");
					break;
				}
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
	private Button btBack;
	@FXML
	private Button btForward;
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
		helpButton.setHelpContent(this.getClass());

		historyTableView.setRowFactory(item -> new TransitionRow());
		historyTableView.getSelectionModel().setCellSelectionEnabled(true);
		positionColumn.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getIndex() + 1));
		transitionColumn.setCellValueFactory(
				cellData -> new SimpleStringProperty(transitionToString(cellData.getValue().getTransition())));

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
			historyTableView.getItems().clear();
			if (to != null) {
				int currentPos = to.getCurrent().getIndex();
				historyTableView.getItems()
						.add(new HistoryItem(currentPos == -1 ? HistoryStatus.PRESENT : HistoryStatus.PAST, -1));
				List<Transition> transitionList = to.getTransitionList();
				for (int i = 0; i < transitionList.size(); i++) {
					HistoryStatus status = getStatus(i, currentPos);
					historyTableView.getItems().add(new HistoryItem(transitionList.get(i), status, i));
				}
				historyTableView.sort();
			}
		};
		traceChangeListener.changed(currentTrace, null, currentTrace.get());
		currentTrace.addListener(traceChangeListener);

		btBack.disableProperty().bind(currentTrace.canGoBackProperty().not());
		btForward.disableProperty().bind(currentTrace.canGoForwardProperty().not());

		historyTableView.setOnMouseMoved(e -> historyTableView.setCursor(Cursor.HAND));

		btBack.setOnAction(e -> {
			if (currentTrace.exists()) {
				currentTrace.set(currentTrace.back());
			}
		});

		btForward.setOnAction(e -> {
			if (currentTrace.exists()) {
				currentTrace.set(currentTrace.forward());
			}
		});

		saveTraceButton.disableProperty()
				.bind(currentProject.existsProperty().and(currentTrace.existsProperty()).not());
	}

	public static String transitionToString(final Transition transition) {
		if (transition == null) {
			// Root item has no transition
			return "---root---";
		} else {
			// Evaluate the transition so the pretty rep includes argument list
			// and result
			transition.evaluate();
			return transition.getPrettyRep().replace("<--", "←");
		}
	}

	public NumberBinding getCurrentHistoryPositionProperty() {
		return Bindings.createIntegerBinding(
				() -> currentTrace.get() == null ? 0 : currentTrace.get().getCurrent().getIndex() + 1, currentTrace);
	}

	public ObservableIntegerValue getObservableHistorySize() {
		return Bindings.createIntegerBinding(() -> Math.max(this.historyTableView.itemsProperty().get().size() - 1, 0),
				historyTableView.itemsProperty().get());
	}

	private HistoryStatus getStatus(int i, int currentPos) {
		if (i < currentPos) {
			return HistoryStatus.PAST;
		} else if (i > currentPos) {
			return HistoryStatus.FUTURE;
		} else {
			return HistoryStatus.PRESENT;
		}
	}

	@FXML
	private void saveTrace() {
		TraceSaver traceSaver = injector.getInstance(TraceSaver.class);
		if (currentTrace.get() != null) {
			traceSaver.saveTrace(
					new PersistentTrace(currentTrace.get(), currentTrace.get().getCurrent().getIndex() + 1),
					currentProject.getCurrentMachine());
		}

	}

}
