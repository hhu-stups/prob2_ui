package de.prob2.ui.history;

import static org.testfx.assertions.api.Assertions.assertThat;

import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.TestBase;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class HistoryViewTest extends TestBase {

	HistoryView historyView;

	@Override
	public void start(Stage stage) {
		super.start(stage);
		historyView = injector.getInstance(HistoryView.class);
		Scene scene = new Scene(historyView);
		scene.getStylesheets().add("prob.css");
		historyView.getStyleClass().add("root");
		stage.setScene(scene);
		stage.show();
	}

	@DisplayName("When no project is open, the history table is empty and the options are disabled")
	@Test
	void test1() {
		MenuButton saveButton = lookup("#saveTraceButton").query();
		assertThat(saveButton).isDisabled();

		TableView<HistoryItem> historyTable = lookup("#historyTableView").query();
		assertThat(historyTable).hasExactlyNumRows(0);

		Label placeholder = lookup("#placeholder").query();
		assertThat(placeholder.getText()).isEqualTo("No model loaded");

		MenuButton traceReplayButton = lookup("#TraceReplayMenuButton").query();
		assertThat(traceReplayButton).isDisabled();
	}

	@DisplayName("The tablerows are resizable")
	@Test
	void test2() {
		assertThat(historyView.isResizable()).isTrue();
		TableView<HistoryItem> historyTable = lookup("#historyTableView").query();
		assertThat(historyTable.isResizable()).isTrue();
	}

	@DisplayName("When the machine is initialized with a ReplayTrace, the ReplayTrace-Button is enabled and executes the correct number of transitions")
	@Test
	void test3() throws InterruptedException {
		new ProjectBuilder(injector).fromProjectFile("src/test/resources/Lift.prob2project")
				.withAnimatedMachine("Lift")
				.build();
		MenuButton traceReplayButton = lookup("#TraceReplayMenuButton").query();
		assertThat(traceReplayButton).isEnabled();
		assertThat(traceReplayButton.getItems()).hasSize(1);
		assertThat(traceReplayButton.getItems().get(0)).hasText("Lift");
		traceReplayButton.getItems().get(0).fire();
		Thread.sleep(2000L);

		TableView<HistoryItem> tableView = lookup("#historyTableView").queryTableView();

		TableColumn<HistoryItem, ?> operationColumn = tableView.getColumns().get(1);

		assertThat(tableView.getItems().size()).isEqualTo(7);
		assertThat(operationColumn.getCellObservableValue(6).getValue()).isEqualTo("decrement");

	}

	@Nested
	@DisplayName("When a machine is loaded without a trace, ")
	class loadedMachine {

		@BeforeEach
		void setup() throws InterruptedException {
			new ProjectBuilder(injector).fromMachineFile("src/test/resources/Lift.prob2project")
					.withAnimatedMachine("Lift")
					.build();
		}

		@DisplayName("the table shows exactly one entry")
		@Test
		void test1() {
			TableView<HistoryItem> tableView = lookup("#historyTableView").queryTableView();
			TableColumn<HistoryItem, ?> indexColumn = tableView.getVisibleLeafColumn(0);
			TableColumn<HistoryItem, ?> operationColumn = tableView.getVisibleLeafColumn(1);

			assertThat(tableView).hasExactlyNumRows(1);
			assertThat(indexColumn.getCellObservableValue(0).getValue()).isEqualTo(0);
			assertThat(operationColumn.getCellObservableValue(0).getValue()).isEqualTo("---root---");
		}

		@DisplayName("the save trace button is enabled")
		@Test
		void test2() {
			MenuButton saveButton = lookup("#saveTraceButton").query();
			assertThat(saveButton).isEnabled();
			clickOn(saveButton);
			List<String> menuItems =
					saveButton.getItems().stream().map(MenuItem::getText).collect(Collectors.toList());
			assertThat(menuItems).containsExactlyInAnyOrder("Save Trace", "Save Trace as CSV Table");
		}
	}
}
