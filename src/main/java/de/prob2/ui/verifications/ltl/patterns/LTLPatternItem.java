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
	// The pattern name is automatically parsed from the code.
	// We store the parsed name in the project file
	// so that we don't need to re-parse the pattern just to get its name
	// every time the project/machine is loaded.
	private final String name;
	private final String description;
	
	@JsonCreator
	public LTLPatternItem(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("code") final String code
	) {
		super(code);
		
		this.name = name;
		this.description = description;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public boolean settingsEqual(final LTLPatternItem other) {
		return this.getName().equals(other.getName());
	}
}
