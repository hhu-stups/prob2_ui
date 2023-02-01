package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob.statespace.FormalismType;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

@FXMLInjected
public final class TraceSelectionView extends ScrollPane {
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
	private final I18n i18n;
	private final TraceViewHandler traceViewHandler;
	private boolean showDescription;

	@Inject
	public TraceSelectionView(final StageManager stageManager, final CurrentTrace currentTrace, final TraceChecker traceChecker, final I18n i18n, final TraceViewHandler traceViewHandler) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.traceChecker = traceChecker;
		this.i18n = i18n;
		this.traceViewHandler = traceViewHandler;
		stageManager.loadFXML(this, "trace_selection_view.fxml");
	}

	@FXML
	private void initialize() {
		traceTableView.itemsProperty().bind(traceViewHandler.getTraces());
		initTableColumns();
		initTableRows();

		traceTableView.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if (showDescription) {
				closeDescription();
				if (to != null) {
					showDescription(to);
				}
			}
		});

		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		traceTableView.disableProperty().bind(partOfDisableBinding.or(currentTrace.stateSpaceProperty().isNull()));
	}

	private void initTableColumns() {
		statusColumn.setCellValueFactory(traceViewHandler.getTraceStatusFactory());
		nameColumn.setCellValueFactory(
				features -> new SimpleStringProperty(features.getValue().getName()));
	}

	private void initTableRows() {
		this.traceTableView.setRowFactory(param -> {
			final TableRow<ReplayTrace> row = new TableRow<>();

			final MenuItem replayTraceItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.replayTrace"));
			replayTraceItem.setDisable(true);
			final MenuItem addTestsItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.editTrace"));
			final MenuItem showDescriptionItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.showDescription"));
			final MenuItem showStatusItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.showStatus"));
			final MenuItem openInExternalEditorItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.openInExternalEditor"));
			final MenuItem revealInExplorerItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.revealInExplorer"));

			// Set listeners for menu items
			traceViewHandler.initializeRow(this.getScene(), row, addTestsItem, replayTraceItem, showStatusItem, openInExternalEditorItem, revealInExplorerItem);
			showDescriptionItem.setOnAction(event -> showDescription(row.getItem()));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(new ContextMenu(replayTraceItem, addTestsItem, showStatusItem, new SeparatorMenuItem(), showDescriptionItem, new SeparatorMenuItem(), openInExternalEditorItem, revealInExplorerItem)));

			row.setOnMouseClicked(event -> {
				ReplayTrace item = row.getItem();
				if(item == null) {
					return;
				}
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					this.traceChecker.check(item, true);
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
		splitPane.getItems().add(1, new DescriptionView(trace, this::closeDescription, stageManager, i18n));
		splitPane.setDividerPositions(0.66);
		showDescription = true;
	}
}
