package de.prob2.ui.consoles;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class ConsoleSearchHandler {
	
	protected boolean searchActive = false;
	protected List<SearchResult> searchResults;
	protected int currentSearchIndex = 0;
	public static final String FOUND = "(backward search) '':";
	public static final String NOTFOUND = "(failed backward search) '':"; 
	private final Console parent;
	private final List<ConsoleInstruction> instructions;
	
	public ConsoleSearchHandler(Console parent, List<ConsoleInstruction> instructions) {
		this.searchResults = new ArrayList<>();
		this.parent = parent;
		this.instructions = instructions;
	}
	
	public boolean isActive() {
		return searchActive;
	}
	
	public void activateSearch() {
		currentSearchIndex = 0;
		searchActive = true;
		if(searchResults.isEmpty()) {
			searchResults.add(new SearchResult(getCurrentSearchResult(), false));
		}
	}
	
	public void deactivateSearch() {
		searchResults.clear();
		currentSearchIndex = 0;
		searchActive = false;
	}
	
	protected void searchResult(String addition) {
		searchResults.clear();
		for(int i = instructions.size() - 1; i >= 0; i--) {
			String key = getSearchCurrent() + addition;
			if("".equals(addition) && !"".equals(key)) {
				key = key.substring(0,key.length() - 1);
			}
			if(instructions.get(i).getInstruction().contains(key)) {
				searchResults.add(new SearchResult(instructions.get(i).getInstruction(),true));
			}
		}
		if(searchResults.isEmpty()) {
			searchResults.add(new SearchResult(getCurrentSearchResult(), false));
		}
	}
	
	public String getSearchCurrent() {
		int posOfFirstQuotation = parent.getCurrentLine().indexOf(39);
		int posOfLastQuotation = parent.getCurrentLine().lastIndexOf(39);
		return parent.getCurrentLine().substring(posOfFirstQuotation + 1, posOfLastQuotation);
	}
	
	public String getCurrentSearchResult() {
		int posOfColon = parent.getCurrentLine().indexOf(':') + parent.getText().lastIndexOf("\n") + 4;
		return parent.getText().substring(posOfColon, parent.getText().length());
	}
	
	protected void handleKey(KeyEvent e) {
		currentSearchIndex = 0;
		if(e.getCode() == KeyCode.BACK_SPACE) {
			searchResult("");
		} else {
			searchResult(e.getText());
		}
		refreshSearch(e.getCharacter());
	}
	
	protected void refreshSearch(String addition) {
		String searchPrefix = FOUND;
		String searchCurrent = getSearchCurrent();
		if(!searchResults.get(0).getFound()) {
			searchPrefix = NOTFOUND;
		}
		int posOfEnter = parent.getText().lastIndexOf("\n");
		String newText = parent.getText().substring(0, posOfEnter + 1);
		newText = new StringBuilder(newText).append(searchPrefix.substring(0,searchPrefix.length() - 2)).toString();
		newText = new StringBuilder(newText).append(searchCurrent + addition + "':" + searchResults.get(currentSearchIndex).getResult()).toString();
		parent.setText(newText);
		int posOfColon = parent.getCurrentLine().indexOf(':') + parent.getText().lastIndexOf("\n") + 3;
		parent.positionCaret(posOfColon -1);
	}
	
	protected void searchNext() {
		currentSearchIndex = Math.min(searchResults.size() - 1, currentSearchIndex + 1);
		refreshSearch("");
	}

}
