package de.prob2.ui.verifications.ltl;

public class LTLHandleItem<T extends ILTLItem> {

	public enum HandleType {
		ADD, CHANGE
	}
	
	private HandleType handleType;
	
	private T item;
	
	public LTLHandleItem(final HandleType handleType, final T item) {
		this.handleType = handleType;
		this.item = item;
	}
	
	public HandleType getHandleType() {
		return handleType;
	}
	
	public T getItem() {
		return item;
	}
	
}
