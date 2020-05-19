package de.prob2.ui.animation.tracereplay;

import java.nio.file.Path;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.config.FileChooserManager.Kind;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ISelectableCheckingView;
import de.prob2.ui.verifications.ItemSelectedFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import org.controlsfx.glyphfont.FontAwesome;

@FXMLInjected
@Singleton
public class TraceReplayView extends ScrollPane implements ISelectableCheckingView {
	@FXML
	private TableView<ReplayTrace> traceTableView;
	@FXML
	private TableColumn<ReplayTrace, Node> statusColumn;
	@FXML
	private TableColumn<ReplayTrace, String> nameColumn;
	@FXML
	private Button checkButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Button loadTraceButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	@FXML
	private SplitPane splitPane;

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final TraceChecker traceChecker;
	private final ResourceBundle bundle;
	private final FileChooserManager fileChooserManager;
	private final Injector injector;
	private final CheckBox selectAll;
	private boolean showDescription;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject,
			final CurrentTrace currentTrace, final TraceChecker traceChecker, final ResourceBundle bundle,
			final FileChooserManager fileChooserManager, final Injector injector) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.traceChecker = traceChecker;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
		this.injector = injector;
		this.selectAll = new CheckBox();
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	private static void updateStatusIcon(final BindableGlyph iconView, final Checked status) {
		switch (status) {
			case SUCCESS:
				iconView.setIcon(FontAwesome.Glyph.CHECK);
				iconView.setTextFill(Color.GREEN);
				break;

			case FAIL:
				iconView.setIcon(FontAwesome.Glyph.REMOVE);
				iconView.setTextFill(Color.RED);
				break;

			case NOT_CHECKED:
				iconView.setIcon(FontAwesome.Glyph.QUESTION_CIRCLE);
				iconView.setTextFill(Color.BLUE);
				break;

			default:
				throw new AssertionError("Unhandled status: " + status);
		}
	}

	@FXML
	private void initialize() {
		helpButton.setHelpContent("animation", "Trace");

		initTableColumns();
		initTableRows();

		final SetChangeListener<Path> listener = c -> {
			if (c.wasAdded()) {
				ReplayTrace replayTrace = new ReplayTrace(c.getElementAdded(),injector);
				traceTableView.getItems().add(replayTrace);
				this.traceChecker.check(replayTrace, true);
			}
			if (c.wasRemoved()) {
				removeFromTraceTableView(c.getElementRemoved());
			}
		};

		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if (from != null) {
				from.getTraceFiles().removeListener(listener);
			}
			traceTableView.getItems().clear();
			if (to != null) {
				to.getTraceFiles().forEach(tracePath -> traceTableView.getItems().add(new ReplayTrace(tracePath, injector)));
				to.getTraceFiles().addListener(listener);
			}
		});

		loadTraceButton.disableProperty().bind(currentProject.currentMachineProperty().isNull());
		cancelButton.disableProperty().bind(traceChecker.currentJobThreadsProperty().emptyProperty());
		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if (to != null) {
				injector.getInstance(DisablePropertyController.class).addDisableProperty(checkButton.disableProperty(), currentTrace.stateSpaceProperty().isNull().or(to.tracesProperty().emptyProperty()));
			}
		});
		traceTableView.disableProperty().bind(currentTrace.stateSpaceProperty().isNull());
	}

	private void initTableColumns() {
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(CheckingType.REPLAY, this));

		selectAll.setSelected(true);
		selectAll.selectedProperty().addListener((observable, from, to) -> {
			if(!to) {
				checkButton.disableProperty().unbind();
				checkButton.setDisable(true);
			} else {
				injector.getInstance(DisablePropertyController.class).addDisableProperty(checkButton.disableProperty(), currentProject.getCurrentMachine().tracesProperty().emptyProperty());
			}
		});
		selectAll.setOnAction(e -> {
			for (ReplayTrace item : traceTableView.getItems()) {
				item.setSelected(selectAll.isSelected());
				traceTableView.refresh();
			}
		});
		shouldExecuteColumn.setGraphic(selectAll);

		statusColumn.setCellValueFactory(features -> {
			final ReplayTrace trace = features.getValue();

			final BindableGlyph statusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
			statusIcon.getStyleClass().add("status-icon");
			statusIcon.bindableFontSizeProperty().bind(injector.getInstance(FontSize.class).fontSizeProperty());
			trace.statusProperty().addListener((o, from, to) -> updateStatusIcon(statusIcon, to));
			updateStatusIcon(statusIcon, trace.getChecked());

			final ProgressIndicator replayProgress = new ProgressBar();
			replayProgress.progressProperty().bind(trace.progressProperty());

			return Bindings.when(trace.progressProperty().isEqualTo(-1)).<Node>then(statusIcon)
					.otherwise(replayProgress);
		});

		nameColumn.setCellValueFactory(
				features -> new SimpleStringProperty(features.getValue().getLocation().getFileName().toString()));
	}

	private void initTableRows() {
		this.traceTableView.setRowFactory(param -> {
			final TableRow<ReplayTrace> row = new TableRow<>();

			final MenuItem replayTraceItem = new MenuItem(
					bundle.getString("animation.tracereplay.view.contextMenu.replayTrace"));
			replayTraceItem.setOnAction(event -> this.traceChecker.check(row.getItem(), true));
			replayTraceItem.setDisable(true);

			final MenuItem showErrorItem = new MenuItem(
					bundle.getString("animation.tracereplay.view.contextMenu.showError"));
			showErrorItem.setOnAction(event -> {
				TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, row.getItem().getErrorMessageBundleKey(), row.getItem().getErrorMessageParams());
				alert.initOwner(stageManager.getCurrent());
				alert.show();
			});
			showErrorItem.setDisable(true);

			final MenuItem deleteTraceItem = new MenuItem(
					bundle.getString("animation.tracereplay.view.contextMenu.removeTrace"));
			deleteTraceItem.setOnAction(
					event -> currentProject.getCurrentMachine().removeTraceFile(row.getItem().getLocation()));

			final MenuItem showDescriptionItem = new MenuItem(
				bundle.getString("animation.tracereplay.view.contextMenu.showDescription"));
			showDescriptionItem.setOnAction(
				event -> showDescription(row.getItem()));


			final MenuItem openInExternalEditorItem = new MenuItem(
				bundle.getString("animation.tracereplay.view.contextMenu.openInExternalEditor"));
			openInExternalEditorItem.setOnAction(
				event -> injector.getInstance(ExternalEditor.class).open(currentProject.getLocation().resolve(row.getItem().getLocation())));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(new ContextMenu(replayTraceItem, showErrorItem, new SeparatorMenuItem(), showDescriptionItem, deleteTraceItem, new SeparatorMenuItem(),openInExternalEditorItem)));
			
			row.itemProperty().addListener((observable, from, to) -> {
				showErrorItem.disableProperty().unbind();
				if (to != null) {
					injector.getInstance(DisablePropertyController.class).addDisableProperty(replayTraceItem.disableProperty(), row.getItem().selectedProperty().not());
					showErrorItem.disableProperty().bind(to.statusProperty().isNotEqualTo(Checked.FAIL));
					row.setTooltip(new Tooltip(row.getItem().getLocation().toString()));
				}
			});

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

	@FXML
	private void checkMachine() {
		traceChecker.checkAll(traceTableView.getItems());
	}

	@FXML
	public synchronized void cancel() {
		traceChecker.cancelReplay();
	}

	@FXML
	private void loadTraceFromFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.loadTrace.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Trace", TraceFileHandler.TRACE_FILE_EXTENSION));
		Path traceFile = fileChooserManager.showOpenFileChooser(fileChooser, Kind.TRACES, stageManager.getCurrent());
		if (traceFile != null) {
			Path relative = currentProject.getLocation().relativize(traceFile);
			currentProject.getCurrentMachine().addTraceFile(relative);
		}
	}
	
	public void refresh() {
		ObservableList<ReplayTrace> traces = FXCollections.observableArrayList(traceTableView.getItems());
		traceTableView.getItems().clear();
		traceChecker.cancelReplay();
		traces.forEach(trace -> trace.setChecked(Checked.NOT_CHECKED));
		traceTableView.setItems(traces);
		traceTableView.refresh();
	}

	private void removeFromTraceTableView(Path tracePath) {
		traceTableView.getItems().removeIf(trace -> trace.getLocation().equals(tracePath));
	}

	public void updateSelectViews() {
		boolean anySelected = false;
		for(ReplayTrace item : traceTableView.getItems()) {
			if(item.selected()) {
				anySelected = true;
			}
		}
		selectAll.setSelected(anySelected);
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
