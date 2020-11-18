package de.prob2.ui.tracediff;

import javafx.scene.control.ListCell;

public class TraceDiffCell extends ListCell<String> {
	private int minSize;
	TraceDiff.TraceDiffList stringList;
	int cnt = 0;

	public TraceDiffCell(TraceDiff.TraceDiffList stringList, int minSize) {
		this.stringList = stringList;
		this.minSize = minSize;
	}

	@Override
	protected void updateItem(String item, boolean empty) {
		if (empty || item == null) {
			setText(null);
			setStyle(null);
		} else if (getText()==null || getText().isEmpty()){
			setText(item);
			cnt++;
			System.out.println("------" + item  + "------");
			for (TraceDiff.IdStringTuple t : stringList) {
				if (t.isNotVisited() && item.equals(t.getString())) {
					System.out.println(t.getId() + " " + t.getString());
					int id = t.getId();
					if (id == minSize) {
						setStyle("-fx-text-fill: red");
						System.out.println("red");
					} else if (id > minSize) {
						setStyle("-fx-text-fill: blue");
						System.out.println("blue");
					} else {
						setStyle("-fx-text-fill: black");
						System.out.println("black");
					}
					t.setVisited();
					break;
				}
			}
			if (cnt >= stringList.size()) {
				cnt = 0;
				stringList.resetVisitation();
				System.out.println("!!!!!!!!!!!!!!reset!!!!!!!!!!!!!!!");
			}
		} else {
			setText(item);
		}
	}
}
