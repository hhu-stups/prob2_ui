package de.prob2.ui.verifications.modelchecking;


public class ModelCheckingHandleItem {

	public enum HandleType {
		ADD, CHANGE
	}

	private HandleType handleType;

	private ModelCheckingItem item;

	public ModelCheckingHandleItem(HandleType handleType, ModelCheckingItem item) {
		this.handleType = handleType;
		this.item = item;
	}

	public HandleType getHandleType() {
		return handleType;
	}

	public ModelCheckingItem getItem() {
		return item;
	}
}
