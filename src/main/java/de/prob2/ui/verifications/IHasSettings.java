package de.prob2.ui.verifications;

public interface IHasSettings {

	/**
	 * This method should be used to check equality for serialisation and editing.
	 * All of these properties should be final!
	 * Properties that are mutable, transient, derived or only used for caching should be ignored.
	 *
	 * <ul>
	 *     <li>two tasks which have different IDs but are otherwise the same should return false</li>
	 *     <li>two tasks which have different "checked" status but are otherwise the same should return true</li>
	 * </ul>
	 *
	 * @param other other object
	 * @return true iff this == that wrt the constraints above
	 */
	boolean settingsEqual(Object other);

}
