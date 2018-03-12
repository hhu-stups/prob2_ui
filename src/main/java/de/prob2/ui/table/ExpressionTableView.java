package de.prob2.ui.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.animator.command.GetAllTableCommands;
import de.prob.animator.command.GetTableForVisualizationCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TableData;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob2.ui.internal.DynamicCommandStage;
import de.prob2.ui.internal.DynamicCommandStatusBar;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ExpressionTableView extends DynamicCommandStage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionTableView.class);
	
	@FXML
	private GridPane gpVisualisation;
	
	@FXML
	private DynamicCommandStatusBar statusBar;
	
	
	@Inject
	public ExpressionTableView(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject,
			final ResourceBundle bundle) {
		super(stageManager, currentTrace, currentProject, bundle);
		stageManager.loadFXML(this, "table_view.fxml");
	}
	
	@Override
	protected void fillCommands() {
		super.fillCommands(new GetAllTableCommands(currentTrace.getCurrentState()));
	}
	
	@Override
	protected void visualize(DynamicCommandItem item) {
		List<IEvalElement> formulas = Collections.synchronizedList(new ArrayList<>());
		interrupt();

		Thread thread = new Thread(() -> {
			Platform.runLater(() -> statusBar.setText(bundle.getString("dynamicStatusBar.loading")));
			try {
				if(item.getArity() > 0) {
					formulas.add(new ClassicalB(taFormula.getText()));
				}
				State id = currentTrace.getCurrentState();
				GetTableForVisualizationCommand cmd = new GetTableForVisualizationCommand(id, item, formulas);
				currentTrace.getStateSpace().execute(cmd);
				Platform.runLater(() -> {
					gpVisualisation.getChildren().clear();
					fillTable(cmd.getTable());
				});
				Platform.runLater(() -> statusBar.setText(""));
				currentThread.set(null);
			} catch (ProBError | EvaluationException e) {
				LOGGER.error("Table visualization failed", e);
				Platform.runLater(() -> {
					stageManager.makeExceptionAlert(bundle.getString("dotview.error.message"), e).show();
					gpVisualisation.getChildren().clear();
				});
			}
		});
		currentThread.set(thread);
		thread.start();
	}
	
	private void fillTable(TableData data) {
		List<String> header = data.getHeader();
		for(int i = 0; i < header.size(); i++) {
			Text text = new Text(header.get(i));
			text.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
			gpVisualisation.add(text, i, 0);
		}
		
		List<List<String>> rows = data.getRows();
		for(int i = 0; i < rows.size(); i++) {
			for(int j = 0; j < rows.get(i).size(); j++) {
				gpVisualisation.add(new Label(rows.get(i).get(j)), j, i+1);
			}
		}
	}
	
	@Override
	protected void reset() {
		gpVisualisation.getChildren().clear();
	}
	
}
