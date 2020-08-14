package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.prob.json.JsonManager;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TestCaseGenerationItem extends AbstractCheckableItem {
	public static final String LEVEL = "level";
	public static final String OPERATIONS = "operations";
	
	public static final JsonDeserializer<TestCaseGenerationItem> JSON_DESERIALIZER = TestCaseGenerationItem::new;
	
	private int maxDepth;
	
	private final transient ListProperty<Trace> examples = new SimpleListProperty<>(this, "examples", FXCollections.observableArrayList());
	private final transient ObservableList<TraceInformationItem> traceInformation = FXCollections.observableArrayList();
	private final transient ObservableList<TraceInformationItem> uncoveredOperations = FXCollections.observableArrayList();
	
	private Map<String, Object> additionalInformation;
	
	private TestCaseGenerationType type;
	
	
	public TestCaseGenerationItem(int maxDepth, int level) {
		super(getMcdcName(maxDepth, level), TestCaseGenerationType.MCDC.getName(), "");
		this.type = TestCaseGenerationType.MCDC;
		this.maxDepth = maxDepth;
		this.additionalInformation = new HashMap<>();
		additionalInformation.put(LEVEL, level);
	}

	public TestCaseGenerationItem(int maxDepth, List<String> operations) {
		super(getCoveredOperationsName(maxDepth, operations), TestCaseGenerationType.COVERED_OPERATIONS.getName(), "");
		this.type = TestCaseGenerationType.COVERED_OPERATIONS;
		this.maxDepth = maxDepth;
		this.additionalInformation = new HashMap<>();
		additionalInformation.put(OPERATIONS, operations);
	}
	
	private TestCaseGenerationItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
		final JsonObject object = json.getAsJsonObject();
		this.maxDepth = JsonManager.checkDeserialize(context, object, "maxDepth", int.class);
		this.additionalInformation = JsonManager.checkDeserialize(context, object, "additionalInformation", new TypeToken<Map<String, Object>>() {}.getType());
		this.type = JsonManager.checkDeserialize(context, object, "type", TestCaseGenerationType.class);
	}
	
	private static String getMcdcName(final int maxDepth, final int level) {
		return "MCDC:" + level + "/" + "DEPTH:" + maxDepth;
	}
	
	private static String getCoveredOperationsName(final int maxDepth, final List<String> operations) {
		return "OPERATION:" + String.join(",", operations) + "/" + "DEPTH:" + maxDepth;
	}
	
	@Override
	public void reset() {
		super.reset();
		this.examples.clear();
		this.getTraceInformation().clear();
		this.getUncoveredOperations().clear();
	}
	
	public int getMcdcLevel() {
		//An element in the values set of additionalInformation can be from any type. GSON casts an integer to double when saving the project file.
		return (int)Double.parseDouble(this.additionalInformation.get(LEVEL).toString());
	}
	
	public List<String> getCoverageOperations() {
		@SuppressWarnings("unchecked")
		final List<String> operations = (List<String>)this.additionalInformation.get(OPERATIONS);
		return operations;
	}
	
	public TestCaseGenerationType getType() {
		return type;
	}

	public int getMaxDepth() {
		return maxDepth;
	}
	
	public ListProperty<Trace> examplesProperty() {
		return examples;
	}
	
	public ObservableList<Trace> getExamples() {
		return examples.get();
	}
	
	public ObservableList<TraceInformationItem> getTraceInformation() {
		return this.traceInformation;
	}
	
	public ObservableList<TraceInformationItem> getUncoveredOperations() {
		return this.uncoveredOperations;
	}
	
	public void setData(final int maxDepth, final int level) {
		this.setData(getMcdcName(maxDepth, level), TestCaseGenerationType.MCDC.getName(), "");
		this.type = TestCaseGenerationType.MCDC;
		this.maxDepth = maxDepth;
		this.additionalInformation.clear();
		this.additionalInformation.put(LEVEL, level);
	}
	
	public void setData(final int maxDepth, final List<String> operations) {
		this.setData(getCoveredOperationsName(maxDepth, operations), TestCaseGenerationType.COVERED_OPERATIONS.getName(), "");
		this.type = TestCaseGenerationType.COVERED_OPERATIONS;
		this.maxDepth = maxDepth;
		this.additionalInformation.clear();
		this.additionalInformation.put(OPERATIONS, operations);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TestCaseGenerationItem)) {
			return false;
		}
		TestCaseGenerationItem otherItem = (TestCaseGenerationItem) obj;
		return otherItem.getName().equals(this.getName()) &&
				otherItem.getCode().equals(this.getCode()) &&
				otherItem.getType().equals(this.getType());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.getName(), this.getCode(), type);
	}
}
