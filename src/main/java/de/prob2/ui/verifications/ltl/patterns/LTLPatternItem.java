package de.prob2.ui.verifications.ltl.patterns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.ILTLItem;

@JsonPropertyOrder({
	"name",
	"description",
	"code",
	"selected",
})
public class LTLPatternItem extends AbstractCheckableItem implements ILTLItem {
	private final String description;
	
	@JsonCreator
	public LTLPatternItem(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("code") final String code
	) {
		super(name, code);
		
		this.description = description;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public boolean settingsEqual(final LTLPatternItem other) {
		return this.getName().equals(other.getName());
	}
}
