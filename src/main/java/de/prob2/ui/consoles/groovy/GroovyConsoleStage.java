package de.prob2.ui.consoles.groovy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentStage;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.fxmisc.flowless.VirtualizedScrollPane;

@Singleton
public final class GroovyConsoleStage extends Stage {
		
	@Inject
	private GroovyConsoleStage(CurrentStage currentStage, GroovyConsole groovyConsole) {
		this.setTitle("Groovy Console");
		// Needs to be wrapped in a Pane subclass (VirtualizedScrollPane is not actually a Pane) for the Mac menu bar to work
		groovyConsole.getStyleClass().add("console");
		this.setScene(new Scene(new StackPane(new VirtualizedScrollPane<>(groovyConsole)), 800, 600));
		this.getScene().getStylesheets().add("prob.css");
		this.setOnCloseRequest(e -> groovyConsole.closeObjectStage());
		currentStage.register(this);
	}

}
