package de.prob2.ui.simulation.choice;

import com.google.inject.Singleton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import javafx.beans.NamedArg;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
@FXMLInjected
public class SimulationPropertyChoice extends GridPane {

	public static class SimulationPropertyItem {

		private final SimulationCheckingType checkingType;

		public SimulationPropertyItem(@NamedArg("checkingType") SimulationCheckingType checkingType) {
			this.checkingType = checkingType;
		}

		@Override
		public String toString() {
			return checkingType.name();
		}

		public String getName(I18n i18n) {
			return i18n.translate(checkingType.getKey());
		}

		public SimulationCheckingType getCheckingType() {
			return checkingType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SimulationPropertyItem that = (SimulationPropertyItem) o;
			return checkingType == that.checkingType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(checkingType);
		}
	}

	private final List<SimulationCheckingType> PREDICATE_TYPES = Arrays.asList(SimulationCheckingType.PREDICATE_INVARIANT, SimulationCheckingType.PREDICATE_FINAL, SimulationCheckingType.PREDICATE_EVENTUALLY);

	private final List<SimulationCheckingType> EXPRESSION_TYPES = Arrays.asList(SimulationCheckingType.AVERAGE, SimulationCheckingType.SUM);


	@FXML
	private Label lbMonteCarloTime;

	@FXML
	private TextField tfMonteCarloTime;

	@FXML
	private Label lbPredicate;

	@FXML
	private TextField tfPredicate;

	@FXML
	private Label lbExpression;

	@FXML
	private TextField tfExpression;

	@FXML
	private ChoiceBox<SimulationPropertyChoice.SimulationPropertyItem> checkingChoice;

	private SimulationChoosingStage choosingStage;

	private final I18n i18n;

	@Inject
	private SimulationPropertyChoice(final StageManager stageManager, final I18n i18n) {
		super();
		stageManager.loadFXML(this, "simulation_property_choice.fxml");
		this.i18n = i18n;
	}

	@FXML
	public void initialize() {
		checkingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.getChildren().remove(lbMonteCarloTime);
			this.getChildren().remove(tfMonteCarloTime);
			this.getChildren().remove(lbPredicate);
			this.getChildren().remove(tfPredicate);
			this.getChildren().remove(lbExpression);
			this.getChildren().remove(tfExpression);
			if(to != null) {
				if(to.getCheckingType() == SimulationCheckingType.TIMING) {
					this.add(lbMonteCarloTime, 1, 3);
					this.add(tfMonteCarloTime, 2, 3);
				}
				if(PREDICATE_TYPES.contains(to.getCheckingType())) {
					this.add(lbPredicate, 1, 3);
					this.add(tfPredicate, 2, 3);
				}
				if(EXPRESSION_TYPES.contains(to.getCheckingType())) {
					this.add(lbExpression, 1, 3);
					this.add(tfExpression, 2, 3);
				}
			}
			choosingStage.sizeToScene();
		});

		checkingChoice.setConverter(new StringConverter<SimulationPropertyChoice.SimulationPropertyItem>() {
			@Override
			public String toString(SimulationPropertyChoice.SimulationPropertyItem object) {
				if(object == null) {
					return "";
				}
				return object.getName(i18n);
			}

			@Override
			public SimulationPropertyChoice.SimulationPropertyItem fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to SimulationPropertyChoiceItem not supported");
			}
		});
	}


	public Map<String, Object> extractInformation() {
		Map<String, Object> information = new HashMap<>();
		SimulationPropertyChoice.SimulationPropertyItem checkingChoiceItem = checkingChoice.getSelectionModel().getSelectedItem();
		information.put("CHECKING_TYPE", checkingChoiceItem.getCheckingType());
		if(PREDICATE_TYPES.contains(checkingChoiceItem.getCheckingType())) {
			information.put("PREDICATE", tfPredicate.getText());
		}

		if(EXPRESSION_TYPES.contains(checkingChoiceItem.getCheckingType())) {
			information.put("EXPRESSION", tfExpression.getText());
		}

		if(checkingChoiceItem.getCheckingType() == SimulationCheckingType.TIMING) {
			information.put("TIME", Integer.parseInt(tfMonteCarloTime.getText()));
		}
		return information;
	}

	public void setChoosingStage(SimulationChoosingStage choosingStage) {
		this.choosingStage = choosingStage;
	}

	public void updateCheck(SimulationType simulationType) {
		checkingChoice.getSelectionModel().clearSelection();
		if(simulationType == SimulationType.ESTIMATION) {
			checkingChoice.getItems().addAll(new SimulationPropertyItem(SimulationCheckingType.AVERAGE), new SimulationPropertyItem(SimulationCheckingType.SUM));
			return;
		}
		checkingChoice.getItems().removeAll(new SimulationPropertyItem(SimulationCheckingType.AVERAGE), new SimulationPropertyItem(SimulationCheckingType.SUM));
	}

	public ChoiceBox<SimulationPropertyItem> getCheckingChoice() {
		return checkingChoice;
	}
}
