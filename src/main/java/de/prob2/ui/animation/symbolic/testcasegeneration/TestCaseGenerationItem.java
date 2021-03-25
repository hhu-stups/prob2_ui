package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

public class TestCaseGenerationItem extends AbstractCheckableItem {
	// AdditionalInformation and its subclasses are only public for Gson type adapter registration.
	// Otherwise they should not be used outside this class.
	public abstract static class AdditionalInformation {}
	
	public static final class McdcInformation extends AdditionalInformation {
		public static final JsonDeserializer<McdcInformation> JSON_DESERIALIZER = McdcInformation::new;
		
		private final int level;
		
		private McdcInformation(final int level) {
			this.level = level;
		}
		
		private McdcInformation(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
			final JsonObject object = json.getAsJsonObject();
			this.level = JsonManager.checkDeserialize(context, object, "level", int.class);
		}
		
		private int getLevel() {
			return this.level;
		}
	}
	
	public static final class CoveredOperationsInformation extends AdditionalInformation {
		public static final JsonDeserializer<CoveredOperationsInformation> JSON_DESERIALIZER = CoveredOperationsInformation::new;
		
		private final List<String> operations;
		
		private CoveredOperationsInformation(final List<String> operations) {
			this.operations = new ArrayList<>(operations);
		}
		
		private CoveredOperationsInformation(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
			final JsonObject object = json.getAsJsonObject();
			this.operations = JsonManager.checkDeserialize(context, object, "operations", new TypeToken<List<String>>() {}.getType());
		}
		
		private List<String> getOperations() {
			return Collections.unmodifiableList(this.operations);
		}
	}
	
	public static final JsonDeserializer<TestCaseGenerationItem> JSON_DESERIALIZER = TestCaseGenerationItem::new;
	
	private final int maxDepth;
	
	private final transient ListProperty<Trace> examples = new SimpleListProperty<>(this, "examples", FXCollections.observableArrayList());
	private final transient ObservableList<TraceInformationItem> traceInformation = FXCollections.observableArrayList();
	private final transient ObservableList<TraceInformationItem> uncoveredOperations = FXCollections.observableArrayList();
	
	private final AdditionalInformation additionalInformation;
	
	private final TestCaseGenerationType type;
	
	
	public TestCaseGenerationItem(int maxDepth, int level) {
		super(getMcdcName(maxDepth, level), TestCaseGenerationType.MCDC.getName(), "");
		this.type = TestCaseGenerationType.MCDC;
		this.maxDepth = maxDepth;
		this.additionalInformation = new McdcInformation(level);
	}

	public TestCaseGenerationItem(int maxDepth, List<String> operations) {
		super(getCoveredOperationsName(maxDepth, operations), TestCaseGenerationType.COVERED_OPERATIONS.getName(), "");
		this.type = TestCaseGenerationType.COVERED_OPERATIONS;
		this.maxDepth = maxDepth;
		this.additionalInformation = new CoveredOperationsInformation(operations);
	}
	
	private TestCaseGenerationItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
		final JsonObject object = json.getAsJsonObject();
		this.maxDepth = JsonManager.checkDeserialize(context, object, "maxDepth", int.class);
		this.type = JsonManager.checkDeserialize(context, object, "type", TestCaseGenerationType.class);
		if (this.type == TestCaseGenerationType.MCDC) {
			this.additionalInformation = JsonManager.checkDeserialize(context, object, "additionalInformation", McdcInformation.class);
		} else if (this.type == TestCaseGenerationType.COVERED_OPERATIONS) {
			this.additionalInformation = JsonManager.checkDeserialize(context, object, "additionalInformation", CoveredOperationsInformation.class);
		} else {
			throw new AssertionError("Unhandled type: " + this.type);
		}
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
		return ((McdcInformation)this.additionalInformation).getLevel();
	}
	
	public List<String> getCoverageOperations() {
		return ((CoveredOperationsInformation)this.additionalInformation).getOperations();
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
	
	public boolean settingsEqual(final TestCaseGenerationItem other) {
		return this.getName().equals(other.getName())
			&& this.getCode().equals(other.getCode())
			&& this.getType().equals(other.getType());
	}

	public String createdByForMetadata(int index) {
		return "Test Case Generation: " + getName() + "; " + getTraceInformation().get(index);
	}
}
