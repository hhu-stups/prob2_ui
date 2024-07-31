package de.prob2.ui.stats;

import java.util.List;
import java.util.Locale;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.ComputeCoverageCommand;
import de.prob.animator.command.ComputeStateSpaceStatsCommand;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.StateSpace;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.SimpleStatsView;
import de.prob2.ui.verifications.modelchecking.ModelCheckingStep;
import de.prob2.ui.verifications.modelchecking.ProBModelCheckingItem;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import org.controlsfx.glyphfont.FontAwesome;

@FXMLInjected
@Singleton
public final class StatsView extends ScrollPane {
	@FXML
	private SimpleStatsView simpleStatsView;
	@FXML
	private GridPane stateStats;
	@FXML
	private GridPane transStats;
	@FXML
	private GridPane stateStatsHeader;
	@FXML
	private GridPane transStatsHeader;
	@FXML
	private VBox statsBox;
	@FXML
	private VBox extendedStatsBox;
	@FXML
	private Label noStatsLabel;
	@FXML
	private ToggleButton extendedStatsToggle;
	@FXML
	private AnchorPane numberOfStatesAnchorPane;
	@FXML
	private AnchorPane numberOfTransitionsAnchorPane;
	@FXML
	private HelpButton helpButton;

	private final I18n i18n;
	private final CurrentTrace currentTrace;
	private final FontSize fontSize;

	@Inject
	public StatsView(final I18n i18n, final StageManager stageManager, final CurrentTrace currentTrace, final FontSize fontSize) {
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.fontSize = fontSize;
		stageManager.loadFXML(this, "stats_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("statistics", null);
		extendedStatsBox.visibleProperty().bind(extendedStatsToggle.selectedProperty());
		noStatsLabel.visibleProperty().bind(currentTrace.isNull());
		statsBox.visibleProperty().bind(noStatsLabel.visibleProperty().not());
		extendedStatsBox.managedProperty().bind(extendedStatsBox.visibleProperty());
		statsBox.managedProperty().bind(statsBox.visibleProperty());
		noStatsLabel.managedProperty().bind(noStatsLabel.visibleProperty());

		this.currentTrace.stateSpaceProperty().addListener((o, from, to) -> this.update(to));
		this.currentTrace.addStatesCalculatedListener(newTransitions -> this.update(this.currentTrace.getStateSpace()));
		this.update(currentTrace.getStateSpace());

		((BindableGlyph) helpButton.getGraphic()).bindableFontSizeProperty().bind(fontSize.fontSizeProperty().multiply(1.2));

		numberOfStatesAnchorPane.widthProperty().addListener((observable, from, to) -> {
			stateStats.getColumnConstraints().get(1).setMinWidth(to.doubleValue());
			stateStatsHeader.getColumnConstraints().get(1).setMinWidth(to.doubleValue());
		});
		numberOfTransitionsAnchorPane.widthProperty().addListener((observable, from, to) -> {
			transStats.getColumnConstraints().get(1).setMinWidth(to.doubleValue());
			transStatsHeader.getColumnConstraints().get(1).setMinWidth(to.doubleValue());
		});

	}

	@FXML
	private void handleExtendedStatsToggle() {
		final FontAwesome.Glyph icon;
		final String text;
		if (extendedStatsToggle.isSelected()) {
			this.update(currentTrace.getStateSpace());

			icon = FontAwesome.Glyph.CLOSE;
			text = i18n.translate("stats.statsView.hideExtendedStats");
		} else {
			icon = FontAwesome.Glyph.PLUS_CIRCLE;
			text = i18n.translate("stats.statsView.showExtendedStats");
		}
		((BindableGlyph)extendedStatsToggle.getGraphic()).setIcon(icon);
		extendedStatsToggle.setText(text);
		extendedStatsToggle.setTooltip(new Tooltip(text));
	}

	private void update(final StateSpace stateSpace) {
		if (stateSpace != null) {
			// Don't attempt to update any stats while probcli is busy (i. e. can't execute commands for a while).
			// During model checking, simple stats are still updated,
			// because probcli automatically returns them after every model checking step
			// and Modelchecker reports these stats to StatsView.
			if (!stateSpace.isBusy()) {
				final ComputeStateSpaceStatsCommand stateSpaceStatsCmd = new ComputeStateSpaceStatsCommand();
				stateSpace.execute(stateSpaceStatsCmd);
				updateSimpleStats(stateSpaceStatsCmd.getResult());

				if (extendedStatsToggle.isSelected()) {
					final ComputeCoverageCommand coverageCmd = new ComputeCoverageCommand();
					stateSpace.execute(coverageCmd);
					final ComputeCoverageCommand.ComputeCoverageResult result = coverageCmd.getResult();
					showStats(result.getNodes(), stateStats);
					showStats(result.getOps(), transStats);
				}
			}
		} else {
			updateSimpleStats(null);
		}
	}

	public void updateSimpleStats(StateSpaceStats result) {
		Platform.runLater(() -> simpleStatsView.setStats(result));
	}

	public void updateWhileModelChecking(ProBModelCheckingItem item) {
		item.currentStepProperty().addListener(new ChangeListener<>() {
			@Override
			public void changed(final ObservableValue<? extends ModelCheckingStep> o, final ModelCheckingStep from, final ModelCheckingStep to) {
				if (to == null) {
					// Model check has finished - stop updating stats based on this task.
					o.removeListener(this);
				} else {
					// While the model check is running, probcli is blocked, so StatsView cannot update itself normally.
					// Instead, update it based on the stats returned by the model checker.
					final StateSpaceStats stats = to.getStats();
					if (stats != null) {
						updateSimpleStats(stats);
					}
				}
			}
		});
	}

	private static void showStats(List<String> packedStats, GridPane grid) {
		final Node[][] stats = new Node[packedStats.size()][];

		for (int i = 0; i < packedStats.size(); i++) {
			final String pStat = packedStats.get(i);
			final String woPre = pStat.startsWith("'") ? pStat.substring(1) : pStat;
			final String woSuf = woPre.endsWith("'") ? woPre.substring(0, woPre.length() - 1) : woPre;
			final String[] split = woSuf.split(":");
			final Stat stat;
			if (split.length == 2) {
				stat = new Stat(split[0], split[1]);
			} else if (split.length == 1) {
				stat = new Stat(split[0]);
			} else {
				throw new IllegalArgumentException(String.format(Locale.ROOT,
						"Invalid number of splits (%d, should be 1 or 2) for packed stat: %s", split.length, pStat));
			}
			stats[i] = stat.toFX();
		}

		Platform.runLater(() -> {
			grid.getChildren().clear();
			for (int i = 0; i < stats.length; i++) {
				grid.addRow(i + 1, stats[i]);
			}
		});
	}

	public ReadOnlyObjectProperty<StateSpaceStats> lastResultProperty() {
		return this.simpleStatsView.statsProperty();
	}

	public StateSpaceStats getLastResult() {
		return this.lastResultProperty().get();
	}
}
