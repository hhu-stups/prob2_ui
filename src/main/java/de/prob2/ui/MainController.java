package de.prob2.ui;

import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.ProjectView;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;

@Singleton
public class MainController extends BorderPane {
	@FXML private TitledPane historyTP;
	@FXML private HistoryView historyView;
	@FXML private TitledPane projectTP;
	@FXML private ProjectView projectView;
	@FXML private SplitPane horizontalSP;
	@FXML private SplitPane verticalSP;
	@FXML private ObservableList<Accordion> accordions;

	private final Injector injector;
	private final StageManager stageManager;
	private final UIState uiState;
	private final ResourceBundle resourceBundle;

	@Inject
	public MainController(Injector injector, StageManager stageManager, UIState uiState, ResourceBundle resourceBundle) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.uiState = uiState;
		this.resourceBundle = resourceBundle;
		refresh();
	}

	@FXML
	private void initialize() {
		accordions.forEach(
				acc -> acc.getPanes().stream().filter(tp -> tp != null && tp.getContent() != null).forEach(tp -> {
					tp.getContent().setVisible(true);
					
					tp.expandedProperty().addListener((observable, from, to) -> {
						if (to) {
							for (TitledPane pane : acc.getPanes()) {
								uiState.getExpandedTitledPanes().remove(pane.getId());
							}
							uiState.getExpandedTitledPanes().add(tp.getId());
						}
					});
				}));
		final ObservableIntegerValue size = historyView.getObservableHistorySize();
		final ObservableValue<Number> current = historyView.getCurrentHistoryPositionProperty();
		this.historyTP.textProperty()
				.bind(Bindings.format(this.resourceBundle.getString("common.views.historyWithState"), current, size));

		Platform.runLater(() -> injector.getInstance(CurrentProject.class).addListener((observable, from, to) -> {
			if (to != null) {
				projectTP.setExpanded(true);
				projectView.showMachines();
			}
		}));
	}

	public void refresh() {
		String guiState = "main.fxml";
		if (!uiState.getGuiState().contains("detached")) {
			guiState = uiState.getGuiState();
		}
		stageManager.loadFXML(this, guiState);
	}

	public double[] getHorizontalDividerPositions() {
		if (horizontalSP != null) {
			return horizontalSP.getDividerPositions();
		}
		return new double[] {};
	}

	public double[] getVerticalDividerPositions() {
		if (verticalSP != null) {
			return verticalSP.getDividerPositions();
		}
		return new double[] {};
	}

	public void setHorizontalDividerPositions(double[] pos) {
		if (horizontalSP != null) {
			horizontalSP.setDividerPositions(pos);
		}
	}

	public void setVerticalDividerPositions(double[] pos) {
		if (verticalSP != null) {
			verticalSP.setDividerPositions(pos);
		}
	}

	public List<Accordion> getAccordions() {
		return Collections.unmodifiableList(accordions);
	}
}
