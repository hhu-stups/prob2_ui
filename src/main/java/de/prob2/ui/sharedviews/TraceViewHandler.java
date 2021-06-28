package de.prob2.ui.sharedviews;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.animation.tracereplay.TraceTestView;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.SetChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.controlsfx.glyphfont.FontAwesome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Singleton
public class TraceViewHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceViewHandler.class);

	private final TraceChecker traceChecker;

	private final CurrentProject currentProject;

	private final Injector injector;

	private final ResourceBundle bundle;

	private final ListProperty<ReplayTrace> traces;

	private final Map<Machine, ListProperty<ReplayTrace>> machinesToTraces;

	private final BooleanProperty noTraces;

	@Inject
	public TraceViewHandler(final TraceChecker traceChecker, final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle) {
		this.traceChecker = traceChecker;
		this.currentProject = currentProject;
		this.injector = injector;
		this.bundle = bundle;
		this.traces = new SimpleListProperty<>(this, "replayTraces", FXCollections.observableArrayList());
		this.machinesToTraces = new HashMap<>();
		this.noTraces = new SimpleBooleanProperty();
		initialize();
	}

	private void initialize() {
		final SetChangeListener<Path> listener = c -> {
			if (c.wasAdded()) {
				ReplayTrace replayTrace = new ReplayTrace(c.getElementAdded(), injector);
				Machine machine = currentProject.getCurrentMachine();
				ListProperty<ReplayTrace> machineTraces = machinesToTraces.get(machine);
				if(!machineTraces.contains(replayTrace)) {
					machineTraces.add(replayTrace);
				}
				this.traceChecker.check(machineTraces.get(machineTraces.indexOf(replayTrace)), true);
			}
			if (c.wasRemoved()) {
				removeTraceItems(c.getElementRemoved());
			}
		};

		currentProject.addListener((observable, from, to) -> {
			this.machinesToTraces.clear();
			traces.unbind();
			noTraces.unbind();
			traces.setValue(FXCollections.observableArrayList());
			noTraces.set(true);
			if(to != null) {
				to.getMachines().forEach(machine -> {
					final ListProperty<ReplayTrace> machineTraces = new SimpleListProperty<>(this, "replayTraces", FXCollections.observableArrayList());
					machinesToTraces.put(machine, machineTraces);
					machine.getTraceFiles().forEach(tracePath -> machineTraces.add(new ReplayTrace(tracePath, injector)));
					Machine.addCheckingStatusListener(machineTraces, machine.traceReplayStatusProperty());
				});
				if(currentProject.getCurrentMachine() != null) {
					final ListProperty<ReplayTrace> machineTraces = machinesToTraces.get(currentProject.getCurrentMachine());
					traces.bind(machineTraces);
					noTraces.bind(currentProject.getCurrentMachine().tracesProperty().emptyProperty());
					currentProject.getCurrentMachine().getTraceFiles().addListener(listener);
				}
			}
		});

		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if (from != null) {
				from.getTraceFiles().removeListener(listener);
			}
			traces.unbind();
			noTraces.unbind();
			traces.setValue(FXCollections.observableArrayList());
			noTraces.set(true);
			if (to != null) {
				final ListProperty<ReplayTrace> machineTraces = machinesToTraces.get(to);
				traces.bind(machineTraces);
				noTraces.bind(to.tracesProperty().emptyProperty());
				to.getTraceFiles().addListener(listener);
			}
		});
	}

	public Callback<TableColumn.CellDataFeatures<ReplayTrace, Node>, ObservableValue<Node>> getTraceStatusFactory() {
		return features -> {
			final ReplayTrace trace = features.getValue();

			final BindableGlyph statusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
			statusIcon.getStyleClass().add("status-icon");
			statusIcon.bindableFontSizeProperty().bind(injector.getInstance(FontSize.class).fontSizeProperty());
			trace.checkedProperty().addListener((o, from, to) -> updateStatusIcon(statusIcon, to));
			updateStatusIcon(statusIcon, trace.getChecked());

			final ProgressIndicator replayProgress = new ProgressBar();
			replayProgress.progressProperty().bind(trace.progressProperty());

			return Bindings.when(trace.progressProperty().isEqualTo(-1)).<Node>then(statusIcon)
					.otherwise(replayProgress);
		};
	}

	public void initializeRow(final Scene scene, final TableRow<ReplayTrace> row, final MenuItem addTestsItem, final MenuItem replayTraceItem, final MenuItem showErrorItem, final MenuItem openInExternalEditorItem) {
		replayTraceItem.setOnAction(event -> this.traceChecker.check(row.getItem(), true));
		addTestsItem.setOnAction(event -> {
			TraceTestView traceTestView = injector.getInstance(TraceTestView.class);
			traceTestView.loadReplayTrace(row.getItem());
			traceTestView.show();
		});
		showErrorItem.setOnAction(event -> {
			ReplayTrace replayTrace = row.getItem();
			if(row.getItem().getErrorMessageBundleKey() != null) {
				TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, replayTrace.getErrorMessageBundleKey(), TraceReplayErrorAlert.Trigger.TRIGGER_TRACE_REPLAY_VIEW, replayTrace.getErrorMessageParams());
				alert.initOwner(scene.getWindow());
				alert.setErrorMessage();
			}
			traceChecker.showTestError(row.getItem().getPersistentTrace(), replayTrace.getPostconditionStatus()
					.stream()
					.map(statuses -> statuses.stream()
							.map(status -> status == Checked.SUCCESS)
							.collect(Collectors.toList()))
					.collect(Collectors.toList()));
		});
		openInExternalEditorItem.setOnAction(
				event -> injector.getInstance(ExternalEditor.class).open(currentProject.getLocation().resolve(row.getItem().getLocation())));
		row.itemProperty().addListener((observable, from, to) -> {
			showErrorItem.disableProperty().unbind();
			if (to != null) {
				replayTraceItem.disableProperty().bind(row.getItem().selectedProperty().not().or(injector.getInstance(DisablePropertyController.class).disableProperty()));

				showErrorItem.disableProperty().bind(to.checkedProperty().isNotEqualTo(Checked.FAIL));
				row.setTooltip(new Tooltip(row.getItem().getLocation().toString()));
			}
		});
	}

	private void removeTraceItems(Path tracePath) {
		traces.removeIf(trace -> trace.getLocation().equals(tracePath));
	}

	public void reset() {
		traceChecker.cancelReplay();
		traces.forEach(trace -> {
			trace.setChecked(Checked.NOT_CHECKED);
			trace.setPostconditionStatus(new ArrayList<>());
		});
	}

	public BooleanProperty getNoTraces() {
		return noTraces;
	}

	public ListProperty<ReplayTrace> getTraces() {
		return traces;
	}

	public MenuItem createReplayTraceItem() {
		final MenuItem replayTraceItem = new MenuItem(bundle.getString("animation.tracereplay.view.contextMenu.replayTrace"));
		replayTraceItem.setDisable(true);
		return replayTraceItem;
	}

	public MenuItem createAddTestsItem() {
		final MenuItem addTestsItem = new MenuItem(bundle.getString("animation.tracereplay.view.contextMenu.editTrace"));
		return addTestsItem;
	}

	public MenuItem createDeleteTraceItem() {
		return new MenuItem(bundle.getString("animation.tracereplay.view.contextMenu.removeTrace"));
	}

	public MenuItem createShowDescriptionItem() {
		return new MenuItem(bundle.getString("animation.tracereplay.view.contextMenu.showDescription"));
	}

	public MenuItem createShowErrorItem() {
		final MenuItem showErrorItem = new MenuItem(
				bundle.getString("animation.tracereplay.view.contextMenu.showError"));
		showErrorItem.setDisable(true);
		return showErrorItem;
	}

	public MenuItem createOpenInExternalEditorItem() {
		return new MenuItem(bundle.getString("animation.tracereplay.view.contextMenu.openInExternalEditor"));
	}

	public MenuItem createRecheckTraceForChangesItem(){
		return new MenuItem(bundle.getString("animation.tracereplay.view.contextMenu.refactorTrace"));
	}

	public static void updateStatusIcon(final BindableGlyph iconView, final Checked status) {
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

	public Map<Machine, ListProperty<ReplayTrace>> getMachinesToTraces() {
		return machinesToTraces;
	}

}
