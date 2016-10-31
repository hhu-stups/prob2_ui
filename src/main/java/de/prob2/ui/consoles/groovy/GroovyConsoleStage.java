package de.prob2.ui.consoles.groovy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentStage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


@Singleton
public final class GroovyConsoleStage extends Stage {
		
	@Inject
	private GroovyConsoleStage(CurrentStage currentStage, GroovyConsole groovyConsole) {
		this.setTitle("Groovy Console");
		this.setScene(new Scene(new StackPane(groovyConsole), 800, 600));
		this.getScene().getStylesheets().add(getClass().getResource("../../../../../prob.css").toExternalForm());
		this.setOnCloseRequest(e -> groovyConsole.closeObjectStage());
		currentStage.register(this);
	}

}
