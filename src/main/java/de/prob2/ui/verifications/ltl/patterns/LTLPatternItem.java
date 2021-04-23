package de.prob2.ui.verifications.ltl.patterns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.ILTLItem;

public class LTLPatternItem extends AbstractCheckableItem implements ILTLItem {
	@JsonCreator
	public LTLPatternItem(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("code") final String code
	) {
		super(name, description, code);
	}
	
	public boolean settingsEqual(final LTLPatternItem other) {
		return this.getName().equals(other.getName());
	}
}
