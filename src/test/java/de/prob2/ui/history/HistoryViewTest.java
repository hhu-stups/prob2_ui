package de.prob2.ui.history;

import de.prob2.ui.ProjectBuilder;
import de.prob2.ui.TestBase;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static org.testfx.assertions.api.Assertions.assertThat;

public class HistoryViewTest extends TestBase {
	@Override
	public void start(Stage stage) {
		super.start(stage);
		HistoryView historyView = super.injector.getInstance(HistoryView.class);
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
		assertThat(placeholder.getText()).isEqualTo(i18n.translate(ResourceBundle.getBundle("de.prob2.ui.prob2").getString("common.noModelLoaded")));

		MenuButton traceReplayButton = lookup("#TraceReplayMenuButton").query();
		assertThat(traceReplayButton).isDisabled();
	}

	@Nested
	@DisplayName("When a machine is loaded without a trace, ")
	class loadedMachine {

		@BeforeEach
		void setup() throws InterruptedException {
			new ProjectBuilder(injector).fromFile("src/test/resources/Lift.mch").withAnimatedMachine("Lift").build();
		}

		@DisplayName("the table shows exactly one entry")
		@Test
		void test3(){
			TableView<HistoryItem> tableView = lookup("#historyTableView").queryTableView();
			TableColumn<HistoryItem, ?> indexColumn = tableView.getVisibleLeafColumn(0);
			TableColumn<HistoryItem, ?> operationColumn = tableView.getVisibleLeafColumn(1);

			assertThat(tableView).hasExactlyNumRows(1);
			assertThat(indexColumn.getCellObservableValue(0).getValue()).isEqualTo(0);
			assertThat(operationColumn.getCellObservableValue(0).getValue()).isEqualTo("---root---");

		}

		@DisplayName("the save trace button is enabled")
		@Test
		void test4() {
			MenuButton traceReplayButton = lookup("#TraceReplayMenuButton").query();
			assertThat(traceReplayButton).isEnabled();

			MenuButton saveButton = lookup("#saveTraceButton").query();
			assertThat(saveButton).isEnabled();
			clickOn(saveButton);
			List<String> menuItems = saveButton.getItems().stream().map(MenuItem::getText).collect(Collectors.toList());
			assertThat(menuItems).containsExactlyInAnyOrder(i18n.translate(ResourceBundle.getBundle("de.prob2.ui.prob2").getString("common.buttons.saveTrace")),
															i18n.translate(ResourceBundle.getBundle("de.prob2.ui.prob2").getString("history.buttons.saveAsTable")));
		}
	}
}
