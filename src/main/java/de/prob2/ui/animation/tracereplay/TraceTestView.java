package de.prob2.ui.animation.tracereplay;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.Postcondition;
import de.prob.statespace.FormalismType;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.sharedviews.PredicateBuilderTableItem;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.verifications.Checked;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@FXMLInjected
public class TraceTestView extends Stage {

	private final class TestCell extends TableCell<PersistentTransition, String> {

		private VBox box;

		private TestCell() {
			super();
		}

		@Override
		protected void updateItem(final String item, final boolean empty) {
			super.updateItem(item, empty);

			if (empty || item == null || this.getTableRow() == null || this.getTableRow().getItem() == null) {
				this.setGraphic(null);
			} else {
				final TableRow<PersistentTransition> tableRow = this.getTableRow();
				final PersistentTransition tableItem = tableRow.getItem();
				int index = tableRow.getIndex();

				final VBox box = new VBox();

				final Button btAddTest = new Button();
				btAddTest.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PLUS_CIRCLE));
				btAddTest.setOnAction(e1 -> {
					Postcondition postcondition = new Postcondition(Postcondition.PostconditionKind.PREDICATE);
					postconditions.get(index).add(postcondition);

					int postconditionIndex = postconditions.get(index).size() - 1;

					final HBox innerBox = new HBox();
					final TextField textField = new TextField("");
					HBox.setHgrow(textField, Priority.ALWAYS);
					//TODO
					textField.setPrefHeight(fontSize.getFontSize() * 1.5);
					textField.textProperty().addListener((o, from, to) -> {
						if(to != null) {
							postcondition.setValue(to);
						}
					});

					final Button btRemoveTest = new Button();
					btRemoveTest.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.MINUS_CIRCLE));
					btRemoveTest.setOnAction(e2 -> {
						box.getChildren().remove(innerBox);
						// requires postConditionIndex to remove. Otherwise there will be problems if there are duplicated predicates
						postconditions.get(index).remove(postconditionIndex);
					});

					innerBox.getChildren().add(textField);
					innerBox.getChildren().add(btRemoveTest);


					box.getChildren().add(box.getChildren().size() - 1, innerBox);
				});

				box.getChildren().add(btAddTest);

				for(int i = 0; i < tableItem.getPostconditions().size(); i++) {
					Postcondition postcondition = tableItem.getPostconditions().get(i);
					postconditions.get(index).add(postcondition);
					final HBox innerBox = new HBox();
					final TextField textField = new TextField("");
					HBox.setHgrow(textField, Priority.ALWAYS);
					//TODO
					textField.setText(postcondition.getValue());
					textField.setPrefHeight(fontSize.getFontSize() * 1.5);
					textField.textProperty().addListener((o, from, to) -> {
						if(to != null) {
							postcondition.setValue(to);
						}
					});

					final Button btRemoveTest = new Button();
					btRemoveTest.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.MINUS_CIRCLE));
					int finalI = i;
					btRemoveTest.setOnAction(e2 -> {
						box.getChildren().remove(innerBox);
						// requires postConditionIndex to remove. Otherwise there will be problems if there are duplicated predicates
						postconditions.get(index).remove(finalI);
					});

					innerBox.getChildren().add(textField);
					innerBox.getChildren().add(btRemoveTest);


					box.getChildren().add(box.getChildren().size() - 1, innerBox);
				}

				this.setGraphic(box);
			}
		}
	}

	@FXML
	private TableView<PersistentTransition> traceTableView;
	@FXML
	private TableColumn<PersistentTransition, String> transitionColumn;
	@FXML
	private TableColumn<PersistentTransition, String> testColumn;
	@FXML
	private TableColumn<PersistentTransition, BindableGlyph> statusColumn;
	@FXML
	private SplitPane splitPane;

	private final CurrentProject currentProject;

	private final FontSize fontSize;

	private final Injector injector;

	private ReplayTrace replayTrace;

	private final List<List<Postcondition>> postconditions = new ArrayList<>();

	private final Map<PersistentTransition, Integer> transitionToIndex = new HashMap<>();

	private ChangeListener<Boolean> changedListener;

	@Inject
	public TraceTestView(final CurrentProject currentProject, final StageManager stageManager, final FontSize fontSize,
						 final Injector injector) {
		this.currentProject = currentProject;
		this.fontSize = fontSize;
		this.injector = injector;
		stageManager.loadFXML(this, "trace_test_view.fxml");
	}

	@FXML
	private void initialize() {
		transitionColumn.setCellValueFactory(features -> new SimpleStringProperty(buildTransitionString(features.getValue())));
		testColumn.setCellFactory(param -> new TestCell());
		testColumn.setCellValueFactory(features -> new SimpleStringProperty(""));
		statusColumn.setCellValueFactory(features -> {
			if(replayTrace.getPostconditionStatus().isEmpty()) {
				final BindableGlyph statusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
				statusIcon.getStyleClass().add("status-icon");
				statusIcon.bindableFontSizeProperty().bind(fontSize.fontSizeProperty());
				TraceViewHandler.updateStatusIcon(statusIcon, Checked.NOT_CHECKED);
				return Bindings.createObjectBinding(() -> statusIcon);
			} else {
				final PersistentTransition transition = features.getValue();
				int index = transitionToIndex.get(transition);
				Checked status = replayTrace.getPostconditionStatus().get(index);

				final BindableGlyph statusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
				statusIcon.getStyleClass().add("status-icon");
				statusIcon.bindableFontSizeProperty().bind(fontSize.fontSizeProperty());
				replayTrace.checkedProperty().addListener((o, from, to) -> TraceViewHandler.updateStatusIcon(statusIcon, status));
				TraceViewHandler.updateStatusIcon(statusIcon, status);

				return Bindings.createObjectBinding(() -> statusIcon);
			}
		});

		changedListener = (o, f, t) -> {
			if(t) {
				currentProject.setSaved(false);
			}
		};
	}

	private String buildTransitionString(PersistentTransition persistentTransition) {
		String opName = persistentTransition.getOperationName();
		if(persistentTransition.getParameters().isEmpty()) {
			return opName;
		} else {
			return String.format("%s(%s)", opName, persistentTransition.getParameters().entrySet()
					.stream()
					.map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(", ")));
		}
	}

	public void loadReplayTrace(ReplayTrace replayTrace) {
		this.replayTrace = replayTrace;
		PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
		traceTableView.getItems().clear();
		if(persistentTrace != null) {
			traceTableView.getItems().addAll(persistentTrace.getTransitionList());
			persistentTrace.getTransitionList().forEach(transition -> postconditions.add(new ArrayList<>()));
			this.replayTrace.changedProperty().addListener(changedListener);
			for(int i = 0; i < persistentTrace.getTransitionList().size(); i++) {
				transitionToIndex.put(persistentTrace.getTransitionList().get(i), i);
			}
		}
	}

	@FXML
	private void applyTest() {
		List<PersistentTransition> transitions = replayTrace.getPersistentTrace().getTransitionList();
		for(int i = 0; i < transitions.size(); i++) {
			PersistentTransition transition = transitions.get(i);
			transition.getPostconditions().clear();
			transition.getPostconditions().addAll(postconditions.get(i));
		}
		replayTrace.setChanged(true);
		injector.getInstance(TraceChecker.class).check(replayTrace, true);
		this.close();
	}

	@Override
	public void close() {
		// TODO: implement alert asking whether changes should be discarded
		super.close();
		replayTrace.changedProperty().removeListener(changedListener);
	}

}
