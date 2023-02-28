package de.prob2.ui.history;

import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.TestBase;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.testfx.assertions.api.Assertions.assertThat;

public class HistoryViewTest extends TestBase {

	HistoryView historyView;
	@Override
	public void start(Stage stage) {
		super.start(stage);
		historyView = super.injector.getInstance(HistoryView.class);
		stage.setScene(new Scene(historyView));
		stage.show();
	}

	@DisplayName("When no project is open, the history table is empty and the options are disabled")
	@Test
	void test1(){
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
	void test2(){
		assertThat(historyView.isResizable()).isTrue();
		TableView<HistoryItem> historyTable = lookup("#historyTableView").query();
		assertThat(historyTable.isResizable()).isTrue();
	}


	@Nested
	@DisplayName("When a machine is loaded without a trace, ")
	class loadedMachine {

		@BeforeEach
		void setup() throws InterruptedException {
			new ProjectBuilder(injector).fromFile("src/test/resources/Lift.mch")
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
			List<String> menuItems = saveButton.getItems().stream().map(MenuItem::getText).collect(Collectors.toList());
			assertThat(menuItems).containsExactlyInAnyOrder("Save Trace", "Save Trace as CSV Table");
		}
	}


		@DisplayName("When the machine is initialized with a ReplayTrace, the ReplayTrace-Button is enabled and shows the correct number of traces")
		@Test
		void test5() throws InterruptedException {
			new ProjectBuilder(injector).fromFile("src/test/resources/Lift.mch")
					.withAnimatedMachine("Lift")
					.withReplayedTrace("Lift.prob2trace")
					.build();
			MenuButton traceReplayButton = lookup("#TraceReplayMenuButton").query();
			assertThat(traceReplayButton).isEnabled();
			assertThat(traceReplayButton.getItems()).hasSize(1);
			assertThat(traceReplayButton.getItems().get(0)).hasText("Lift");
	}
}
