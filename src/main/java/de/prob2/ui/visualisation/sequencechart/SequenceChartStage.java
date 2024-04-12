package de.prob2.ui.visualisation.sequencechart;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

@Singleton
public final class SequenceChartStage extends Stage {

	private final StageManager stageManager;
	private final CurrentTrace trace;
	private final JavaLocator javaLocator;
	private final PlantUmlLocator plantUmlLocator;
	private final BackgroundUpdater updater;

	@FXML
	private WebView dotView;

	@Inject
	private SequenceChartStage(StageManager stageManager, StopActions stopActions, CurrentTrace trace, JavaLocator javaLocator, PlantUmlLocator plantUmlLocator) {
		super();
		this.stageManager = stageManager;
		this.trace = trace;
		this.javaLocator = javaLocator;
		this.plantUmlLocator = plantUmlLocator;
		this.updater = new BackgroundUpdater("Sequence Chart Updater");

		stopActions.add(this.updater::shutdownNow);
		stageManager.loadFXML(this, "sequence_chart_stage.fxml", this.getClass().getName());
	}

	@FXML
	@SuppressWarnings("unused")
	private void initialize() {
		this.showingProperty().addListener(o -> this.refresh());
		this.trace.addListener(o -> this.refresh());
		this.refresh();
	}

	private void refresh() {
		Trace trace = this.trace.get();
		if (trace == null || !this.isShowing()) {
			this.showPlaceholder();
			return;
		}

		this.visualize("@startuml\n" +
				               "Alice -> Bob: " + trace.getCurrent().toString() + "\n" +
				               "Bob --> Alice: Authentication Response\n" +
				               "Alice -> Bob: Another authentication Request\n" +
				               "Alice <-- Bob: Another authentication Response\n" +
				               "@enduml");
	}

	private void showPlaceholder() {
		this.visualize("""
				@startuml
				version
				@enduml""");
	}

	private void visualize(String uml) {
		String javaExecutable = this.javaLocator.getJavaExecutable();
		Optional<Path> optPlantUmlJar = this.plantUmlLocator.findPlantUmlJar();
		if (optPlantUmlJar.isEmpty()) {
			Platform.runLater(() -> {
				this.stageManager.makeAlert(
						Alert.AlertType.ERROR,
						"visualisation.sequenceChart.error.noPlantUml.header",
						"visualisation.sequenceChart.error.noPlantUml.message",
						"https://plantuml.com/download",
						this.plantUmlLocator.getDirectory()
				).show();
				this.close();
			});
			return;
		}
		Path plantUmlJar = optPlantUmlJar.get();

		this.updater.cancel(true);
		if (this.updater.isShutdown()) {
			return;
		}

		this.updater.execute(() -> {
			String output;
			try {
				PlantUmlCall c = new PlantUmlCall(javaExecutable, plantUmlJar).outputFormat(PlantUmlCall.SVG);
				if (this.trace.getStateSpace() != null) {
					String dot = this.trace.getStateSpace().getCurrentPreference("DOT");
					c.dotExecutable(dot);
				}
				byte[] data = c.input(uml).call();
				output = new String(data, StandardCharsets.UTF_8);
			} catch (InterruptedException ignored) {
				return;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			Platform.runLater(() -> this.dotView.getEngine().loadContent("<center>" + output + "</center>"));
		});
	}
}
