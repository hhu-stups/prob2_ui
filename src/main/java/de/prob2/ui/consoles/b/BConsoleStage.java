package de.prob2.ui.consoles.b;

import org.fxmisc.flowless.VirtualizedScrollPane;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentStage;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@Singleton
public final class BConsoleStage extends Stage {

	@Inject
	private BConsoleStage(CurrentStage currentStage, BConsole bConsole) {
		this.setTitle("B Console");
		bConsole.getStyleClass().add("console");
		this.setScene(new Scene(new StackPane(new VirtualizedScrollPane<>(bConsole)), 800, 600));
		this.getScene().getStylesheets().add("prob.css");
		currentStage.register(this, this.getClass().getName());
	}

}
