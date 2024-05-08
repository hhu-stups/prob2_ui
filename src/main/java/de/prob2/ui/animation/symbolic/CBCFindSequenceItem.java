package de.prob2.ui.animation.symbolic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

@JsonPropertyOrder({
	"id",
	"selected",
	"operationNames",
})
public final class CBCFindSequenceItem extends SymbolicAnimationItem {
	private final List<String> operationNames;
	
	@JsonCreator
	public CBCFindSequenceItem(
		@JsonProperty("id") String id,
		@JsonProperty("operationNames") List<String> operationNames
	) {
		super(id);
		
		this.operationNames = new ArrayList<>(operationNames);
	}
	
	public List<String> getOperationNames() {
		return Collections.unmodifiableList(this.operationNames);
	}
	
	@Override
	public ValidationTaskType<CBCFindSequenceItem> getTaskType() {
		return BuiltinValidationTaskTypes.CBC_FIND_SEQUENCE;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("animation.type.sequence");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return String.join(";", this.getOperationNames());
	}
	
	@Override
	public boolean settingsEqual(Object other) {
		return super.settingsEqual(other)
			&& other instanceof CBCFindSequenceItem o
			&& this.getOperationNames().equals(o.getOperationNames());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("id", this.getId())
			.add("operationNames", this.getOperationNames())
			.toString();
	}
}
