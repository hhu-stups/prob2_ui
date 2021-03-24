package de.prob2.ui.animation.tracereplay.refactoring;

import java.nio.file.Path;

public class RefactorSetup {


	public final WhatToDo whatToDo;
	public final Path fileAlpha;
	public final Path fileBeta;
	public final Path traceFile;
	public final boolean setResult;

	public RefactorSetup(WhatToDo whatToDo, Path fileAlpha, Path fileBeta, Path traceFile, boolean setResult){
		this.fileAlpha = fileAlpha;
		this.fileBeta = fileBeta;
		this.whatToDo = whatToDo;
		this.traceFile = traceFile;
		this.setResult = setResult;
	}



	public WhatToDo getWhatToDo() {
		return whatToDo;
	}

	public Path getFileAlpha() {
		return fileAlpha;
	}

	public Path getFileBeta() {
		return fileBeta;
	}

	public Path getTraceFile() {
		return traceFile;
	}

	public boolean setResult() {
		return setResult;
	}


	public enum WhatToDo{
		REFACTOR_TRACE, REFINEMENT_REPLAY, OPTION_REPLAY, NOTHING
	}
}
