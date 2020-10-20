package de.prob2.ui.sharedviews;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.Checked;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.nio.file.Path;
import java.util.ResourceBundle;

@Singleton
public class TraceViewHandler {

    private final TraceChecker traceChecker;

    private final CurrentProject currentProject;

    private final Injector injector;

    private final ObservableList<ReplayTrace> traces;

    private final BooleanProperty noTraces;

    @Inject
    public TraceViewHandler(final TraceChecker traceChecker, final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle) {
        this.traceChecker = traceChecker;
        this.currentProject = currentProject;
        this.injector = injector;
        this.traces = FXCollections.observableArrayList();
        this.noTraces = new SimpleBooleanProperty();
        initialize();
    }

    private void initialize() {
        final SetChangeListener<Path> listener = c -> {
            if (c.wasAdded()) {
                ReplayTrace replayTrace = new ReplayTrace(c.getElementAdded(), injector);
                traces.add(replayTrace);
                this.traceChecker.check(replayTrace, true);
                this.traceChecker.isNewTrace();
            }
            if (c.wasRemoved()) {
                removeTraceItems(c.getElementRemoved());
            }
        };

        currentProject.currentMachineProperty().addListener((observable, from, to) -> {
            if (from != null) {
                from.getTraceFiles().removeListener(listener);
            }
            traces.clear();
            if (to != null) {
                noTraces.bind(to.tracesProperty().emptyProperty());
                to.getTraceFiles().forEach(tracePath -> traces.add(new ReplayTrace(tracePath, injector)));
                to.getTraceFiles().addListener(listener);
            } else {
                noTraces.unbind();
                noTraces.set(true);
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

    public void initializeRow(final Scene scene, final TableRow<ReplayTrace> row, final MenuItem replayTraceItem, final MenuItem showErrorItem, final MenuItem openInExternalEditorItem) {
        replayTraceItem.setOnAction(event -> this.traceChecker.check(row.getItem(), true));
        showErrorItem.setOnAction(event -> {
            TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, row.getItem().getErrorMessageBundleKey(), TraceReplayErrorAlert.Trigger.TRIGGER_TRACE_REPLAY_VIEW, row.getItem().getErrorMessageParams());
            alert.initOwner(scene.getWindow());
            alert.setErrorMessage();
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
        traces.forEach(trace -> trace.setChecked(Checked.NOT_CHECKED));
    }

    public BooleanProperty getNoTraces() {
        return noTraces;
    }

    public ObservableList<ReplayTrace> getTraces() {
        return traces;
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

}
