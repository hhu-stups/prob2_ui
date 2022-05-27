package de.prob2.ui.sharedviews;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.statespace.FormalismType;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public class TraceSelectionView extends Stage {
	@FXML
	private TableView<ReplayTrace> traceTableView;
	@FXML
	private TableColumn<ReplayTrace, Node> statusColumn;
	@FXML
	private TableColumn<ReplayTrace, String> nameColumn;
	@FXML
	private SplitPane splitPane;

	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final TraceChecker traceChecker;
	private final Injector injector;
	private final TraceViewHandler traceViewHandler;
	private boolean showDescription;

	@Inject
	public TraceSelectionView(final StageManager stageManager, final CurrentTrace currentTrace, final TraceChecker traceChecker,
			final Injector injector, final TraceViewHandler traceViewHandler) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.traceChecker = traceChecker;
		this.injector = injector;
		this.traceViewHandler = traceViewHandler;
		stageManager.loadFXML(this, "trace_selection_view.fxml");
	}

	@FXML
	private void initialize() {
		traceTableView.itemsProperty().bind(traceViewHandler.getTraces());
		initTableColumns();
		initTableRows();
		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		traceTableView.disableProperty().bind(partOfDisableBinding.or(currentTrace.stateSpaceProperty().isNull()));
	}

	private void initTableColumns() {
		statusColumn.setCellValueFactory(injector.getInstance(TraceViewHandler.class).getTraceStatusFactory());
		nameColumn.setCellValueFactory(
				features -> new SimpleStringProperty(features.getValue().getLocation().getFileName().toString()));
	}

	private void initTableRows() {
		this.traceTableView.setRowFactory(param -> {
			final TableRow<ReplayTrace> row = new TableRow<>();

			final MenuItem replayTraceItem = traceViewHandler.createReplayTraceItem();
			final MenuItem addTestsItem = traceViewHandler.createAddTestsItem();
			final MenuItem showDescriptionItem = traceViewHandler.createShowDescriptionItem();
			final MenuItem showErrorItem = traceViewHandler.createShowErrorItem();
			final MenuItem openInExternalEditorItem = traceViewHandler.createOpenInExternalEditorItem();
			final MenuItem revealInExplorerItem = traceViewHandler.createRevealInExplorerItem();

			// Set listeners for menu items
			traceViewHandler.initializeRow(this.getScene(), row, addTestsItem, replayTraceItem, showErrorItem, openInExternalEditorItem, revealInExplorerItem);
			showDescriptionItem.setOnAction(event -> showDescription(row.getItem()));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(new ContextMenu(replayTraceItem, addTestsItem, showErrorItem, new SeparatorMenuItem(), showDescriptionItem, new SeparatorMenuItem(), openInExternalEditorItem, revealInExplorerItem)));

			row.setOnMouseClicked(event -> {
				ReplayTrace item = row.getItem();
				if(item == null) {
					return;
				}
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					this.traceChecker.check(item, true);
				} else if(showDescription && event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 1) {
					showDescription(row.getItem());
					row.updateSelected(true);
				}
			});

			return row;
		});
	}

	public void closeDescription() {
		if (showDescription) {
			splitPane.getItems().remove(1);
			showDescription = false;
		}
	}

	private void showDescription(ReplayTrace trace) {
		if(showDescription) {
			closeDescription();
		}
		splitPane.getItems().add(1, new DescriptionView(trace, this::closeDescription, stageManager, injector));
		splitPane.setDividerPositions(0.66);
		showDescription = true;
	}
}
