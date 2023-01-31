package de.prob2.ui.sharedviews;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.ReplayedTraceStatusAlert;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.animation.tracereplay.TraceTestView;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.menu.RevealInExplorer;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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

@Singleton
public class TraceViewHandler {
	private final TraceChecker traceChecker;

	private final CurrentProject currentProject;

	private final Injector injector;

	private final I18n i18n;

	private final ListProperty<ReplayTrace> traces;

	@Inject
	public TraceViewHandler(final TraceChecker traceChecker, final CurrentProject currentProject, final Injector injector, final I18n i18n) {
		this.traceChecker = traceChecker;
		this.currentProject = currentProject;
		this.injector = injector;
		this.i18n = i18n;
		this.traces = new SimpleListProperty<>(this, "replayTraces", FXCollections.observableArrayList());
		initialize();
	}

	private void initialize() {
		currentProject.addListener((observable, from, to) -> {
			//When saving the project, the listener for the current project property is triggered
			//even though it is the same project. This again resets the status of all traces (which is not the desired behavior).
			//So this is why there is the additional check for the locations of the previous and the current project
			//It is also possible for the user to switch to the same project (without saving).
			//In this case, the statuses of all traces are reset by the current machine property (as no machine is chosen after switching the project).
			if(from == null || to == null || !to.getLocation().equals(from.getLocation())) {
				traces.unbind();
				Machine machine = currentProject.getCurrentMachine();
				if (machine != null) {
					traces.bind(machine.tracesProperty());
				} else {
					traces.set(FXCollections.observableArrayList());
				}
			}
		});

		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			traces.unbind();
			if (to != null) {
				traces.bind(to.tracesProperty());
			} else {
				traces.set(FXCollections.observableArrayList());
			}
		});
	}

	public Callback<TableColumn.CellDataFeatures<ReplayTrace, Node>, ObservableValue<Node>> getTraceStatusFactory() {
		return features -> {
			final ReplayTrace trace = features.getValue();

			final BindableGlyph statusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
			statusIcon.getStyleClass().add("status-icon");
			statusIcon.bindableFontSizeProperty().bind(injector.getInstance(FontSize.class).fontSizeProperty());
			trace.checkedProperty().addListener((o, from, to) -> Platform.runLater(() -> updateStatusIcon(statusIcon, to)));
			updateStatusIcon(statusIcon, trace.getChecked());

			final ProgressIndicator replayProgress = new ProgressBar();
			replayProgress.progressProperty().bind(trace.progressProperty());

			return Bindings.when(trace.progressProperty().isEqualTo(-1)).<Node>then(statusIcon)
					.otherwise(replayProgress);
		};
	}

	public void initializeRow(final Scene scene, final TableRow<ReplayTrace> row, final MenuItem addTestsItem, final MenuItem replayTraceItem, final MenuItem showStatusItem, final MenuItem openInExternalEditorItem, final MenuItem revealInExplorerItem) {
		replayTraceItem.setOnAction(event -> this.traceChecker.check(row.getItem(), true));
		addTestsItem.setOnAction(event -> {
			TraceTestView traceTestView = injector.getInstance(TraceTestView.class);
			traceTestView.loadReplayTrace(row.getItem());
			traceTestView.show();
		});
		showStatusItem.setOnAction(event -> {
			ReplayedTraceStatusAlert alert = new ReplayedTraceStatusAlert(injector, row.getItem());
			alert.show();
		});
		openInExternalEditorItem.setOnAction(event ->
			injector.getInstance(ExternalEditor.class).open(row.getItem().getAbsoluteLocation())
		);
		revealInExplorerItem.setOnAction(event ->
			injector.getInstance(RevealInExplorer.class).revealInExplorer(row.getItem().getAbsoluteLocation())
		);
		row.itemProperty().addListener((observable, from, to) -> {
			if (to != null) {
				replayTraceItem.disableProperty().bind(row.getItem().selectedProperty().not().or(injector.getInstance(DisablePropertyController.class).disableProperty()));
				row.setTooltip(new Tooltip(row.getItem().getLocation().toString()));
			}
		});
	}

	public ListProperty<ReplayTrace> getTraces() {
		return traces;
	}

	public MenuItem createReplayTraceItem() {
		final MenuItem replayTraceItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.replayTrace"));
		replayTraceItem.setDisable(true);
		return replayTraceItem;
	}

	public MenuItem createAddTestsItem() {
		final MenuItem addTestsItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.editTrace"));
		return addTestsItem;
	}

	public MenuItem createEditIdItem() {
		return new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.editId"));
	}

	public MenuItem createDeleteTraceItem() {
		return new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.removeTrace"));
	}

	public MenuItem createShowDescriptionItem() {
		return new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.showDescription"));
	}

	public MenuItem createShowStatusItem() {
		return new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.showStatus"));
	}

	public MenuItem createOpenInExternalEditorItem() {
		return new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.openInExternalEditor"));
	}

	public MenuItem createRevealInExplorerItem() {
		return new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.revealInExplorer"));
	}

	public MenuItem createRecheckTraceForChangesItem(){
		return new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.refactorTrace"));
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
}
