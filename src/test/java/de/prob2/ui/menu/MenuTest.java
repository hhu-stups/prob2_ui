package de.prob2.ui.menu;

import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.TestBase;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.NewProjectStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.matcher.base.WindowMatchers;
import org.testfx.util.NodeQueryUtils;
import org.testfx.util.WaitForAsyncUtils;

public class MenuTest extends TestBase {

	NewProjectStage newProjectStage;

	StageManager stageManager;

	Stage stage;
	MenuController menucontroller;


	@Override
	public void start(Stage stage) {
		this.stage = stage;
		super.start(stage);
		menucontroller = injector.getInstance(MenuController.class);
		newProjectStage = injector.getInstance(NewProjectStage.class);
		stageManager = injector.getInstance(StageManager.class);
		stage.setScene(new Scene(menucontroller));
		stage.show();
	}

	@Test
	void newProjectTest() throws TimeoutException {
		clickOn("#fileMenu");
		WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS, () ->
				lookup("#newProjectItem").match(NodeQueryUtils.isVisible()).tryQuery().isPresent());
		clickOn("#newProjectItem");
		FxAssert.verifyThat(window("New ProB Project"), WindowMatchers.isShowing());
	}


	@Test
	void openProjectTest() throws TimeoutException {
		clickOn("#fileMenu");
		WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS, () ->
				lookup("#openProjectItem").match(NodeQueryUtils.isVisible()).tryQuery().isPresent());
		clickOn("#openProjectItem");
		//TODO
	}

	@Test
	void closeWindowMenu() throws TimeoutException {
		clickOn("#fileMenu");
		WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS, () ->
				lookup("#closeWindowMenuItem").match(NodeQueryUtils.isVisible()).tryQuery().isPresent());
		clickOn("#closeWindowMenuItem");
		//TODO
	}

	@Test
	void preferencesMenu() throws TimeoutException {
		clickOn("#fileMenu");
		WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS, () ->
				lookup("#preferencesItem").match(NodeQueryUtils.isVisible()).tryQuery().isPresent());
		clickOn("#preferencesItem");
		FxAssert.verifyThat(window("Preferences"), WindowMatchers.isShowing());
	}


	@Nested
	class withLoadedProject {

		@BeforeEach
		void setup() {
			Platform.runLater(() -> {
				try {
					new ProjectBuilder(injector).fromFile("src/test/resources/Lift.mch")
							.withAnimatedMachine("Lift")
							.build();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			});
		}

		@Test
		void saveProjectMenuIsEnabled() throws TimeoutException {
			clickOn("#fileMenu");
			WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS, () ->
					lookup("#saveProjectItem").match(NodeQueryUtils.isVisible()).tryQuery().isPresent());
			Node menuItem = lookup("#saveProjectItem").query();
			// TODO
		}

		@Test
		@DisplayName("Unmodified Machines can't be saved")
		void saveMachineMenu() throws TimeoutException {
			clickOn("#fileMenu");

			WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS, () ->
					lookup("#saveMachineItem").match(NodeQueryUtils.isVisible()).tryQuery().isPresent());
			Node menuItem = lookup("#saveMachineItem").query();
			//TODO
		}

		@Test
		void reloadMachine() throws TimeoutException {
			clickOn("#fileMenu");
			WaitForAsyncUtils.waitFor(2, TimeUnit.SECONDS, () ->
					lookup("#reloadMachineItem").match(NodeQueryUtils.isVisible()).tryQuery().isPresent());
			clickOn("#reloadMachineItem");

			//TODO
		}

	}
}
