package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob.statespace.FormalismType;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.input.MouseButton;

@FXMLInjected
public final class TraceSelectionView extends CheckingViewBase<ReplayTrace> {
	private final class Row extends RowBase {
		private Row() {
			final MenuItem replayTraceItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.replayTrace"));
			replayTraceItem.setDisable(true);
			final MenuItem addTestsItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.editTrace"));
			final MenuItem showDescriptionItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.showDescription"));
			final MenuItem showStatusItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.showStatus"));
			final MenuItem openInExternalEditorItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.openInExternalEditor"));
			final MenuItem revealInExplorerItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.revealInExplorer"));

			// Set listeners for menu items
			traceViewHandler.initializeRow(this.getScene(), this, addTestsItem, replayTraceItem, showStatusItem, openInExternalEditorItem, revealInExplorerItem);
			showDescriptionItem.setOnAction(event -> showDescription(this.getItem()));

			contextMenu.getItems().addAll(replayTraceItem, addTestsItem, showStatusItem, new SeparatorMenuItem(), showDescriptionItem, new SeparatorMenuItem(), openInExternalEditorItem, revealInExplorerItem);

			this.setOnMouseClicked(event -> {
				ReplayTrace item = this.getItem();
				if(item == null) {
					return;
				}
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					traceChecker.check(item, true);
				}
			});
		}
	}

	@FXML
	private TableColumn<ReplayTrace, Node> statusProgressColumn;
	@FXML
	private SplitPane splitPane;

	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final TraceChecker traceChecker;
	private final I18n i18n;
	private final TraceViewHandler traceViewHandler;
	private boolean showDescription;

	@Inject
	public TraceSelectionView(final StageManager stageManager, final DisablePropertyController disablePropertyController, final CurrentTrace currentTrace, final TraceChecker traceChecker, final I18n i18n, final TraceViewHandler traceViewHandler) {
		super(disablePropertyController);
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.traceChecker = traceChecker;
		this.i18n = i18n;
		this.traceViewHandler = traceViewHandler;
		stageManager.loadFXML(this, "trace_selection_view.fxml");
	}

	@FXML
	public void initialize() {
		super.initialize();
		items.bind(traceViewHandler.getTraces());
		initTableColumns();
		initTableRows();

		itemsTable.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if (showDescription) {
				closeDescription();
				if (to != null) {
					showDescription(to);
				}
			}
		});

		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		itemsTable.disableProperty().bind(partOfDisableBinding.or(currentTrace.stateSpaceProperty().isNull()));
	}

	private void initTableColumns() {
		statusProgressColumn.setCellValueFactory(traceViewHandler.getTraceStatusFactory());
	}

	private void initTableRows() {
		this.itemsTable.setRowFactory(table -> new Row());
	}

	@Override
	protected String configurationForItem(final ReplayTrace item) {
		return item.getName();
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
