package de.prob2.ui.animation.tracereplay.refactoring;

import java.nio.file.Path;

import de.prob2.ui.internal.Translatable;

public class RefactorSetup {

	public final WhatToDo whatToDo;
	public final Path fileAlpha;
	public final Path fileBeta;
	public final Path traceFile;
	public final boolean setResult;

	public final int maxDepth;

	public final int maxBreadth;


	public RefactorSetup(WhatToDo whatToDo, Path fileAlpha, Path fileBeta, Path traceFile, boolean setResult, int maxDepth, int maxBreadth) {
		this.fileAlpha = fileAlpha;
		this.fileBeta = fileBeta;
		this.whatToDo = whatToDo;
		this.traceFile = traceFile;
		this.setResult = setResult;
		this.maxDepth = maxDepth;
		this.maxBreadth = maxBreadth;
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


	public int getMaxDepth() {
		return maxDepth;
	}

	public int getMaxBreadth() {
		return maxBreadth;
	}


	public enum WhatToDo implements Translatable {
		REFINEMENT_REPLAY("traceModification.traceRefactorSetup.whatToDo.refine"),
		OPTION_REPLAY("traceModification.traceRefactorSetup.whatToDo.replay"),
		NOTHING("traceModification.traceRefactorSetup.whatToDo.nothing"),
		;

		public static WhatToDo[] validValues() {
			return new WhatToDo[]{REFINEMENT_REPLAY, OPTION_REPLAY};
		}

		private final String translationKey;

		WhatToDo(String translationKey) {
			this.translationKey = translationKey;
		}

		@Override
		public String getTranslationKey() {
			return translationKey;
		}
	}
}
