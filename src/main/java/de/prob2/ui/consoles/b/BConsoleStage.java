package de.prob2.ui.consoles.b;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.ResourceBundle;

@Singleton
public final class BConsoleStage extends Stage {

	@Inject
	private BConsoleStage(StageManager stageManager, BConsole bConsole, ResourceBundle bundle) {
		this.setTitle(bundle.getString("consoles.b.title"));
		bConsole.getStyleClass().add("console");
		this.setScene(new Scene(new StackPane(new VirtualizedScrollPane<>(bConsole)), 800, 600));
		stageManager.register(this, this.getClass().getName());
	}

}
