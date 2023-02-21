package de.prob2.ui.history;

import com.google.inject.Guice;
import com.google.inject.Injector;


import de.prob2.ui.ProB2;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.helpsystem.HelpSystem;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.preferences.Preference;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.base.WindowMatchers;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static org.testfx.assertions.api.Assertions.assertThat;


public class HistoryViewTest extends ApplicationTest {

	HistoryView historyView;
	HelpSystem helpSystem;
	I18n i18n;
	ProjectManager projectManager;
	RuntimeOptions runtimeOptions;
	Injector injector;

	@Override
	public void start(Stage stage) {
		runtimeOptions =new RuntimeOptions(null, null, null, null, false, false);
		ProB2Module module = new ProB2Module(new ProB2(), runtimeOptions);

		injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);

		i18n = injector.getInstance(I18n.class);
		projectManager = injector.getInstance(ProjectManager.class);

		historyView = injector.getInstance(HistoryView.class);
		helpSystem = injector.getInstance(HelpSystem.class);
		stage.setScene(new Scene(historyView));
		stage.show();

	}

	@DisplayName("The help-button leads to the history-helppage")
	@Test
	void test1() {
		Button helpButton = lookup("#helpButton").query();
		clickOn(helpButton);
		FxAssert.verifyThat(window(helpSystem), WindowMatchers.isShowing());
		WebView webview = lookup("#webView").query();
		assertThat(webview.getEngine().getTitle()).isEqualTo(i18n.translate(ResourceBundle.getBundle("de.prob2.ui.helpsystem.help_page_titles").getString("history")));
	}

	@DisplayName("When no project is open, the history table is empty and the options are disabled")
	@Test
	void test2(){
		MenuButton saveButton = lookup("#saveTraceButton").query();
		assertThat(saveButton).isDisabled();

		TableView<HistoryItem> historyTable = lookup("#historyTableView").query();
		assertThat(historyTable).hasExactlyNumRows(0);

		Label placeholder = lookup("#placeholder").query();
		assertThat(placeholder.getText()).isEqualTo(i18n.translate(ResourceBundle.getBundle("de.prob2.ui.prob2").getString("common.noModelLoaded")));

		MenuButton traceReplayButton = lookup("#TraceReplayMenuButton").query();
		assertThat(traceReplayButton).isDisabled();

//		TODO: find smartest way to test the navigation-buttons. Test them for each view (e.g. operations, history, visB) separately? Or write a test class for the navigation-buttons?
		Button button = lookup("#fastBackButton").queryButton();
		assertThat(button).isDisabled();
	}

	@DisplayName("When a machine is loaded, the table shows the correct entries")
	@Test
	void test3() throws InterruptedException {

		projectManager.openAutomaticProjectFromMachine(Paths.get("/home/ina/Arbeit/prob2_ui/src/test/resources/Lift.mch"));
		CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		currentProject.startAnimation(currentProject.get().getMachine("Lift"), Preference.DEFAULT);
		Thread.sleep(3000);

		TableView<HistoryItem> tableView = lookup("#historyTableView").queryTableView();
		TableColumn<HistoryItem, ?> indexColumn = tableView.getVisibleLeafColumn(0);
		TableColumn<HistoryItem, ?> operationColumn = tableView.getVisibleLeafColumn(1);

		assertThat(tableView).hasExactlyNumRows(1);
		assertThat(indexColumn.getCellObservableValue(0).getValue()).isEqualTo(0);
		assertThat(operationColumn.getCellObservableValue(0).getValue()).isEqualTo("---root---");

	}

	@DisplayName("When a machine is loaded, the forward-button and the save trace button are enabled")
	@Test
	void test4() throws InterruptedException {
		projectManager.openAutomaticProjectFromMachine(Paths.get("src/test/resources/Lift.mch"));
		CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		currentProject.startAnimation(currentProject.get().getMachine("Lift"), Preference.DEFAULT);
		Thread.sleep(3000);

		MenuButton traceReplayButton = lookup("#TraceReplayMenuButton").query();
		assertThat(traceReplayButton).isEnabled();

		Button button = lookup("#forwardButton").queryButton();
		assertThat(button).isEnabled();

		MenuButton saveButton = lookup("#saveTraceButton").query();
		assertThat(saveButton).isEnabled();
		clickOn(saveButton);
		List<String> menuItems = saveButton.getItems().stream().map(MenuItem::getText).collect(Collectors.toList());
		assertThat(menuItems).containsExactlyInAnyOrder(i18n.translate(ResourceBundle.getBundle("de.prob2.ui.prob2").getString("common.buttons.saveTrace")),
														i18n.translate(ResourceBundle.getBundle("de.prob2.ui.prob2").getString("history.buttons.saveAsTable")));
		}
	}
