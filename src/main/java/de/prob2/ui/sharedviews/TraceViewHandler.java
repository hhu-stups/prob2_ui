package de.prob2.ui.sharedviews;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.check.tracereplay.TraceReplay;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.animation.tracereplay.TraceTestView;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.prob2fx.CurrentProject;
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

	private ReplayTrace lastTrace;

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
			lastTrace = null;
			if (c.wasAdded()) {
				ReplayTrace replayTrace = new ReplayTrace(c.getElementAdded(), injector);
				lastTrace = replayTrace;
				Machine machine = currentProject.getCurrentMachine();
				if(!machinesToTraces.containsKey(machine)) {
					final ListProperty<ReplayTrace> machineTraces = new SimpleListProperty<>(this, "replayTraces", FXCollections.observableArrayList());
					machinesToTraces.put(machine, machineTraces);
				}
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
			//When saving the project, the listener for the current project property is triggered
			//even though it is the same project. This again resets the status of all traces (which is not the desired behavior).
			//So this is why there is the additional check for the locations of the previous and the current project
			//It is also possible for the user to switch to the same project (without saving).
			//In this case, the statuses of all traces are reset by the current machine property (as no machine is chosen after switching the project).
			if(from == null || to == null || !to.getLocation().equals(from.getLocation())) {
				this.machinesToTraces.clear();
				traces.unbind();
				noTraces.unbind();
				traces.setValue(FXCollections.observableArrayList());
				noTraces.set(true);
				if (to != null) {
					fillMachineToTraces(to);
					bindTraces(currentProject.getCurrentMachine(), listener);
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
			bindTraces(to, listener);
		});
	}

	private void fillMachineToTraces(Project project) {
		project.getMachines().forEach(machine -> {
			final ListProperty<ReplayTrace> machineTraces = new SimpleListProperty<>(this, "replayTraces", FXCollections.observableArrayList());
			machinesToTraces.put(machine, machineTraces);
			machine.getTraceFiles().forEach(tracePath -> machineTraces.add(new ReplayTrace(tracePath, injector)));
			Machine.addCheckingStatusListener(machineTraces, machine.traceReplayStatusProperty());
		});
	}

	private void bindTraces(Machine machine, SetChangeListener<Path> listener) {
		if (machine != null) {
			if(!machinesToTraces.containsKey(machine)) {
				final ListProperty<ReplayTrace> machineTraces = new SimpleListProperty<>(this, "replayTraces", FXCollections.observableArrayList());
				machinesToTraces.put(machine, machineTraces);
			}
			ListProperty<ReplayTrace> machineTraces = machinesToTraces.get(machine);
			traces.bind(machineTraces);
			noTraces.bind(machine.tracesProperty().emptyProperty());
			machine.getTraceFiles().addListener(listener);
		}
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
			if(!row.getItem().getReplayedTrace().getErrors().isEmpty()) {
				// TODO Implement displaying rich error information in TraceReplayErrorAlert (using ErrorTableView) instead of converting the error messages to a string
				final String errorMessage = replayTrace.getReplayedTrace().getErrors().stream()
					.map(ErrorItem::toString)
					.collect(Collectors.joining("\n"));
				TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, "common.literal", TraceReplayErrorAlert.Trigger.TRIGGER_TRACE_REPLAY_VIEW, errorMessage);
				alert.initOwner(scene.getWindow());
				alert.setErrorMessage();
			}
			traceChecker.showTestError(row.getItem().getPersistentTrace(), replayTrace.getPostconditionStatus()
					.stream()
					.map(statuses -> statuses.stream()
							.map(status -> status == Checked.SUCCESS ? TraceReplay.PostconditionResult.SUCCESS :
									       status == Checked.FAIL ? TraceReplay.PostconditionResult.FAIL : TraceReplay.PostconditionResult.PARSE_ERROR)
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
			trace.setReplayedTrace(null);
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
			case PARSE_ERROR:
				iconView.setIcon(FontAwesome.Glyph.WARNING);
				iconView.setTextFill(Color.ORANGE);
				break;
			default:
				throw new AssertionError("Unhandled status: " + status);
		}
	}

	public Map<Machine, ListProperty<ReplayTrace>> getMachinesToTraces() {
		return machinesToTraces;
	}

	public ReplayTrace getLastTrace() {
		return lastTrace;
	}
}
