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
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

@Singleton
public final class SequenceChartStage extends Stage {

	private final CurrentTrace trace;
	private final JavaLocator javaLocator;
	private final PlantUmlLocator plantUmlLocator;
	private final BackgroundUpdater updater;

	@FXML
	private WebView dotView;

	@Inject
	private SequenceChartStage(StageManager stageManager, StopActions stopActions, CurrentTrace trace, JavaLocator javaLocator, PlantUmlLocator plantUmlLocator) {
		super();
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
		ChangeListener<? super Trace> listener = (o, from, to) -> {
			if (to == null) {
				this.visualize("""
					@startuml
					version
					@enduml""");
				return;
			}

			this.visualize("@startuml\n" +
				"Alice -> Bob: " + to.getCurrent().toString() + "\n" +
				"Bob --> Alice: Authentication Response\n" +
				"Alice -> Bob: Another authentication Request\n" +
				"Alice <-- Bob: Another authentication Response\n" +
				"@enduml");
		};
		this.trace.addListener(listener);
		listener.changed(this.trace, null, this.trace.get());
	}

	private void visualize(String uml) {
		this.updater.cancel(true);
		this.updater.execute(() -> {
			String output;
			try {
				Optional<Path> plantUmlJar = this.plantUmlLocator.findPlantUmlJar();
				if (plantUmlJar.isEmpty()) {
					return;
				}

				PlantUmlCall c = new PlantUmlCall(this.javaLocator.getJavaExecutable(), plantUmlJar.get()).outputFormat(PlantUmlCall.SVG);
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
