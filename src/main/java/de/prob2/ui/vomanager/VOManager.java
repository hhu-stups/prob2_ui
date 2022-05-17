package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class VOManager {
	@Inject
	public VOManager() {}

	public boolean requirementIsValid(String name, String text) {
		//isBlank() requires Java version >= 11
		String nameWithoutWhiteSpaces = name.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		String textWithoutWhiteSpaces = text.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		return nameWithoutWhiteSpaces.length() > 0 && textWithoutWhiteSpaces.length() > 0;
	}

	public boolean voIsValid(String name, Requirement requirement) {
		//isBlank() requires Java version >= 11
		if(requirement == null) {
			return false;
		}
		String nameWithoutWhiteSpaces = name.replaceAll("\t", "").replaceAll(" ", "").replaceAll("\n", "");
		return nameWithoutWhiteSpaces.length() > 0;
	}

}
