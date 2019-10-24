package de.prob2.ui.animation.symbolic;

import de.prob.statespace.Trace;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationFormulaExtractor;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SymbolicAnimationFormulaItem extends SymbolicFormulaItem {

	private static final String MAX_DEPTH = "maxDepth";

	private static final String LEVEL = "level";

	private static final String OPERATIONS = "operations";

	private transient ListProperty<Trace> examples;
	
	private BooleanProperty testCase;

	private Map<String, Object> additionalInformation;

	public SymbolicAnimationFormulaItem(String name, SymbolicExecutionType type) {
		super(name, type);
		this.examples = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.additionalInformation = new HashMap<>();
		this.testCase = new SimpleBooleanProperty(type == SymbolicExecutionType.MCDC || type == SymbolicExecutionType.COVERED_OPERATIONS);
	}

	public SymbolicAnimationFormulaItem(String name, SymbolicExecutionType type, Map<String, Object> additionalInformation) {
		super(name, type);
		this.examples = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.additionalInformation = additionalInformation;
		this.testCase = new SimpleBooleanProperty(type == SymbolicExecutionType.MCDC || type == SymbolicExecutionType.COVERED_OPERATIONS);
	}

	public SymbolicAnimationFormulaItem(int maxDepth, int level) {
		super("MCDC:" + level + "/" + "DEPTH:" + maxDepth, SymbolicExecutionType.MCDC);
		this.examples = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.additionalInformation = new HashMap<>();
		additionalInformation.put(MAX_DEPTH, maxDepth);
		additionalInformation.put(LEVEL, level);
		this.testCase = new SimpleBooleanProperty(true);
	}

	public SymbolicAnimationFormulaItem(int maxDepth, List<String> operations) {
		super("OPERATION:" + String.join(",", operations) + "/" + "DEPTH:" + maxDepth, SymbolicExecutionType.COVERED_OPERATIONS);
		this.examples = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.additionalInformation = new HashMap<>();
		additionalInformation.put(MAX_DEPTH, maxDepth);
		additionalInformation.put(OPERATIONS, operations);
		this.testCase = new SimpleBooleanProperty(true);
	}

	@Override
	public void replaceMissingWithDefaults() {
		super.replaceMissingWithDefaults();
		if(this.examples == null) {
			this.examples = new SimpleListProperty<>(FXCollections.observableArrayList());
		} else {
			this.examples.setValue(FXCollections.observableArrayList());
		}
		if(this.additionalInformation == null) {
			this.additionalInformation = new HashMap<>();
		}
		if(this.testCase == null) {
			this.testCase = new SimpleBooleanProperty(type == SymbolicExecutionType.MCDC || type == SymbolicExecutionType.COVERED_OPERATIONS);
		}
		if(type == SymbolicExecutionType.MCDC) {
			replaceMissingMCDCOptionsByDefaults();
		} else if(type == SymbolicExecutionType.COVERED_OPERATIONS) {
			replaceMissingCoveredOperationsOptionsByDefaults();
		}
	}
	
	private void replaceMissingMCDCOptionsByDefaults() {
		if(additionalInformation.get(MAX_DEPTH) == null || additionalInformation.get(LEVEL) == null) {
			int depth = TestCaseGenerationFormulaExtractor.extractDepth(this.code);
			int level = TestCaseGenerationFormulaExtractor.extractLevel(this.code);
			additionalInformation.put(MAX_DEPTH, depth);
			additionalInformation.put(LEVEL, level);
		}
	}
	
	private void replaceMissingCoveredOperationsOptionsByDefaults() {
		if(additionalInformation.get(MAX_DEPTH) == null || additionalInformation.get(OPERATIONS) == null) {
			int depth = TestCaseGenerationFormulaExtractor.extractDepth(this.code);
			List<String> operations = TestCaseGenerationFormulaExtractor.extractOperations(this.code);
			additionalInformation.put(MAX_DEPTH, depth);
			additionalInformation.put(OPERATIONS, operations);
		}
	}

	public ObservableList<Trace> getExamples() {
		return examples.get();
	}

	public ListProperty<Trace> examplesProperty() {
		return examples;
	}
	
	public BooleanProperty isTestCase() {
		return testCase;
	}
	
	@Override
	public void reset() {
		this.initialize();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof SymbolicAnimationFormulaItem)) {
			return false;
		}
		SymbolicAnimationFormulaItem otherItem = (SymbolicAnimationFormulaItem) obj;
		return otherItem.getName().equals(this.getName()) &&
				otherItem.getCode().equals(this.getCode()) &&
				otherItem.getType().equals(this.getType());
	}
	
	@Override
	public int hashCode() {
		 return Objects.hash(name, code, type);
	}

	public void setData(String name, String description, String code, SymbolicExecutionType type, Map<String, Object> additionalInformation) {
		super.setData(name, description, code, type);
		this.additionalInformation = additionalInformation;
	}

	public Object getAdditionalInformation(String key) {
		return additionalInformation.get(key);
	}

	public void putAdditionalInformation(String key, Object value) {
		additionalInformation.put(key, value);
	}
}
