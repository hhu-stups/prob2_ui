package de.prob2.ui.beditor;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.be4.classicalb.core.parser.BLexer;
import de.be4.classicalb.core.parser.lexer.LexerException;
import de.be4.classicalb.core.parser.node.EOF;
import de.be4.classicalb.core.parser.node.TAbstractConstants;
import de.be4.classicalb.core.parser.node.TAbstractVariables;
import de.be4.classicalb.core.parser.node.TAny;
import de.be4.classicalb.core.parser.node.TArity;
import de.be4.classicalb.core.parser.node.TAssert;
import de.be4.classicalb.core.parser.node.TAssertions;
import de.be4.classicalb.core.parser.node.TAssign;
import de.be4.classicalb.core.parser.node.TBe;
import de.be4.classicalb.core.parser.node.TBegin;
import de.be4.classicalb.core.parser.node.TBool;
import de.be4.classicalb.core.parser.node.TBtree;
import de.be4.classicalb.core.parser.node.TCase;
import de.be4.classicalb.core.parser.node.TChoice;
import de.be4.classicalb.core.parser.node.TClosure;
import de.be4.classicalb.core.parser.node.TClosure1;
import de.be4.classicalb.core.parser.node.TComment;
import de.be4.classicalb.core.parser.node.TCommentBody;
import de.be4.classicalb.core.parser.node.TCommentEnd;
import de.be4.classicalb.core.parser.node.TConcreteConstants;
import de.be4.classicalb.core.parser.node.TConcreteVariables;
import de.be4.classicalb.core.parser.node.TConjunction;
import de.be4.classicalb.core.parser.node.TConst;
import de.be4.classicalb.core.parser.node.TConstants;
import de.be4.classicalb.core.parser.node.TConstraints;
import de.be4.classicalb.core.parser.node.TDefinitions;
import de.be4.classicalb.core.parser.node.TDirectProduct;
import de.be4.classicalb.core.parser.node.TDivision;
import de.be4.classicalb.core.parser.node.TDo;
import de.be4.classicalb.core.parser.node.TDoubleColon;
import de.be4.classicalb.core.parser.node.TDoubleEqual;
import de.be4.classicalb.core.parser.node.TDoubleVerticalBar;
import de.be4.classicalb.core.parser.node.TEither;
import de.be4.classicalb.core.parser.node.TElementOf;
import de.be4.classicalb.core.parser.node.TElse;
import de.be4.classicalb.core.parser.node.TElsif;
import de.be4.classicalb.core.parser.node.TEmptySet;
import de.be4.classicalb.core.parser.node.TEnd;
import de.be4.classicalb.core.parser.node.TEqual;
import de.be4.classicalb.core.parser.node.TEquivalence;
import de.be4.classicalb.core.parser.node.TExists;
import de.be4.classicalb.core.parser.node.TExtends;
import de.be4.classicalb.core.parser.node.TFalse;
import de.be4.classicalb.core.parser.node.TFather;
import de.be4.classicalb.core.parser.node.TFin;
import de.be4.classicalb.core.parser.node.TFin1;
import de.be4.classicalb.core.parser.node.TForAny;
import de.be4.classicalb.core.parser.node.TGreater;
import de.be4.classicalb.core.parser.node.TGreaterEqual;
import de.be4.classicalb.core.parser.node.TIdentifierLiteral;
import de.be4.classicalb.core.parser.node.TIf;
import de.be4.classicalb.core.parser.node.TImplementation;
import de.be4.classicalb.core.parser.node.TImplies;
import de.be4.classicalb.core.parser.node.TImports;
import de.be4.classicalb.core.parser.node.TIn;
import de.be4.classicalb.core.parser.node.TIncludes;
import de.be4.classicalb.core.parser.node.TInfix;
import de.be4.classicalb.core.parser.node.TInitialisation;
import de.be4.classicalb.core.parser.node.TInt;
import de.be4.classicalb.core.parser.node.TInteger;
import de.be4.classicalb.core.parser.node.TInterval;
import de.be4.classicalb.core.parser.node.TInvariant;
import de.be4.classicalb.core.parser.node.TIseq;
import de.be4.classicalb.core.parser.node.TIseq1;
import de.be4.classicalb.core.parser.node.TLeft;
import de.be4.classicalb.core.parser.node.TLess;
import de.be4.classicalb.core.parser.node.TLessEqual;
import de.be4.classicalb.core.parser.node.TLet;
import de.be4.classicalb.core.parser.node.TLineComment;
import de.be4.classicalb.core.parser.node.TLogicalOr;
import de.be4.classicalb.core.parser.node.TMachine;
import de.be4.classicalb.core.parser.node.TMirror;
import de.be4.classicalb.core.parser.node.TModel;
import de.be4.classicalb.core.parser.node.TNat;
import de.be4.classicalb.core.parser.node.TNat1;
import de.be4.classicalb.core.parser.node.TNatural;
import de.be4.classicalb.core.parser.node.TNatural1;
import de.be4.classicalb.core.parser.node.TNonInclusion;
import de.be4.classicalb.core.parser.node.TNot;
import de.be4.classicalb.core.parser.node.TNotEqual;
import de.be4.classicalb.core.parser.node.TOf;
import de.be4.classicalb.core.parser.node.TOperations;
import de.be4.classicalb.core.parser.node.TOr;
import de.be4.classicalb.core.parser.node.TOutputParameters;
import de.be4.classicalb.core.parser.node.TPartialBijection;
import de.be4.classicalb.core.parser.node.TPartialFunction;
import de.be4.classicalb.core.parser.node.TPartialInjection;
import de.be4.classicalb.core.parser.node.TPartialSurjection;
import de.be4.classicalb.core.parser.node.TPerm;
import de.be4.classicalb.core.parser.node.TPostfix;
import de.be4.classicalb.core.parser.node.TPow;
import de.be4.classicalb.core.parser.node.TPow1;
import de.be4.classicalb.core.parser.node.TPragmaDescription;
import de.be4.classicalb.core.parser.node.TPragmaEnd;
import de.be4.classicalb.core.parser.node.TPragmaFile;
import de.be4.classicalb.core.parser.node.TPragmaFreeText;
import de.be4.classicalb.core.parser.node.TPragmaGenerated;
import de.be4.classicalb.core.parser.node.TPragmaIdOrString;
import de.be4.classicalb.core.parser.node.TPragmaIgnoredText;
import de.be4.classicalb.core.parser.node.TPragmaImportPackage;
import de.be4.classicalb.core.parser.node.TPragmaLabel;
import de.be4.classicalb.core.parser.node.TPragmaPackage;
import de.be4.classicalb.core.parser.node.TPragmaStart;
import de.be4.classicalb.core.parser.node.TPragmaSymbolic;
import de.be4.classicalb.core.parser.node.TPre;
import de.be4.classicalb.core.parser.node.TPrefix;
import de.be4.classicalb.core.parser.node.TPromotes;
import de.be4.classicalb.core.parser.node.TProperties;
import de.be4.classicalb.core.parser.node.TRank;
import de.be4.classicalb.core.parser.node.TRefinement;
import de.be4.classicalb.core.parser.node.TRefines;
import de.be4.classicalb.core.parser.node.TRight;
import de.be4.classicalb.core.parser.node.TSees;
import de.be4.classicalb.core.parser.node.TSelect;
import de.be4.classicalb.core.parser.node.TSeq;
import de.be4.classicalb.core.parser.node.TSeq1;
import de.be4.classicalb.core.parser.node.TSetRelation;
import de.be4.classicalb.core.parser.node.TSets;
import de.be4.classicalb.core.parser.node.TSizet;
import de.be4.classicalb.core.parser.node.TSkip;
import de.be4.classicalb.core.parser.node.TSon;
import de.be4.classicalb.core.parser.node.TSons;
import de.be4.classicalb.core.parser.node.TString;
import de.be4.classicalb.core.parser.node.TStringLiteral;
import de.be4.classicalb.core.parser.node.TStruct;
import de.be4.classicalb.core.parser.node.TSubtree;
import de.be4.classicalb.core.parser.node.TSystem;
import de.be4.classicalb.core.parser.node.TThen;
import de.be4.classicalb.core.parser.node.TTop;
import de.be4.classicalb.core.parser.node.TTotalBijection;
import de.be4.classicalb.core.parser.node.TTotalFunction;
import de.be4.classicalb.core.parser.node.TTotalInjection;
import de.be4.classicalb.core.parser.node.TTotalRelation;
import de.be4.classicalb.core.parser.node.TTotalSurjection;
import de.be4.classicalb.core.parser.node.TTotalSurjectionRelation;
import de.be4.classicalb.core.parser.node.TTree;
import de.be4.classicalb.core.parser.node.TTrue;
import de.be4.classicalb.core.parser.node.TUnion;
import de.be4.classicalb.core.parser.node.TUnrecognisedPragma;
import de.be4.classicalb.core.parser.node.TUses;
import de.be4.classicalb.core.parser.node.TVar;
import de.be4.classicalb.core.parser.node.TVariables;
import de.be4.classicalb.core.parser.node.TVariant;
import de.be4.classicalb.core.parser.node.TWhen;
import de.be4.classicalb.core.parser.node.TWhere;
import de.be4.classicalb.core.parser.node.TWhile;
import de.be4.classicalb.core.parser.node.Token;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.ModelFactory;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BLexerSyntaxHighlighting {
	private static final Logger LOGGER = LoggerFactory.getLogger(BLexerSyntaxHighlighting.class);

	private static final Map<Class<? extends Token>, String> syntaxClassesForB = new HashMap<>();

	static {
		addBTokens("editor_identifier", TIdentifierLiteral.class);
		addBTokens("editor_assignments", TAssign.class, TOutputParameters.class,
			TDoubleVerticalBar.class, TAssert.class,
			TClosure.class, TClosure1.class, TDirectProduct.class, TDivision.class,
			TEmptySet.class, TDoubleColon.class, TImplies.class, TLogicalOr.class,
			TInterval.class, TUnion.class, TOr.class, TNonInclusion.class,
			TTotalBijection.class, TTotalFunction.class, TTotalInjection.class,
			TTotalRelation.class, TTotalSurjection.class, TFalse.class, TTrue.class,
			TTotalSurjectionRelation.class, TPartialBijection.class, TPartialFunction.class,
			TPartialInjection.class, TPartialSurjection.class, TSetRelation.class,
			TFin.class, TFin1.class, TPerm.class, TSeq.class, TSeq1.class, TIseq.class,
			TIseq1.class, TNot.class);
		addBTokens("editor_logical", TConjunction.class, TForAny.class, TExists.class);
		addBTokens("editor_arithmetic", TDoubleEqual.class, TEqual.class,
			TElementOf.class, TEquivalence.class, TGreaterEqual.class, TLessEqual.class,
			TNotEqual.class, TGreater.class, TLess.class);
		addBTokens("editor_types", TBool.class, TNat.class, TNat1.class, TNatural.class,
			TNatural1.class, TStruct.class, TInteger.class, TInt.class, TString.class);
		addBTokens("editor_string", TStringLiteral.class);
		addBTokens("editor_unsupported", TTree.class, TLeft.class, TRight.class,
			TInfix.class, TArity.class, TSubtree.class, TPow.class, TPow1.class,
			TSon.class, TFather.class, TRank.class, TMirror.class, TSizet.class,
			TPostfix.class, TPrefix.class, TSons.class, TTop.class, TConst.class, TBtree.class);
		addBTokens("editor_ctrlkeyword", TSkip.class, TLet.class, TBe.class,
			TVar.class, TIn.class, TAny.class, TWhile.class,
			TDo.class, TVariant.class, TElsif.class, TIf.class, TThen.class, TElse.class, TEither.class,
			TCase.class, TSelect.class, TAssert.class, TAssertions.class, TWhen.class, TPre.class, TBegin.class,
			TChoice.class, TWhere.class, TOf.class, TEnd.class);
		addBTokens("editor_keyword", TMachine.class, TOperations.class, TRefinement.class, TImplementation.class,
			TOperations.class, TAssertions.class, TInitialisation.class, TSees.class, TPromotes.class,
			TUses.class, TIncludes.class, TImports.class, TRefines.class, TExtends.class, TSystem.class,
			TModel.class, TInvariant.class, TConcreteVariables.class,
			TAbstractVariables.class, TVariables.class, TProperties.class,
			TConstants.class, TAbstractConstants.class, TConcreteConstants.class,
			TConstraints.class, TSets.class, TDefinitions.class);
		addBTokens("editor_comment", TComment.class, TCommentBody.class, TCommentEnd.class,
			TLineComment.class, TPragmaDescription.class, TPragmaEnd.class, TPragmaFile.class,
			TPragmaFreeText.class, TPragmaGenerated.class, TPragmaIdOrString.class, TPragmaIgnoredText.class,
			TPragmaImportPackage.class, TPragmaLabel.class, TPragmaPackage.class, TPragmaStart.class,
			TPragmaSymbolic.class, TUnrecognisedPragma.class);
	}

	private BLexerSyntaxHighlighting() {
		throw new AssertionError("Utility class");
	}

	@SafeVarargs
	private static void addBTokens(final String syntaxclass, final Class<? extends Token>... tokens) {
		for (final Class<? extends Token> c : tokens) {
			syntaxClassesForB.put(c, syntaxclass);
		}
	}

	static StyleSpans<Collection<String>> computeBHighlighting(String text) {
		BLexer lexer = new BLexer(new PushbackReader(new StringReader(text), text.length()));
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		try {
			Token t;
			do {
				t = lexer.next();
				String string = syntaxClassesForB.get(t.getClass());
				spansBuilder.add(string == null ? Collections.emptySet() : Collections.singleton(string), t.getText().length());
			} while (!(t instanceof EOF));
		} catch (LexerException | IOException e) {
			LOGGER.info("Failed to lex", e);
		}
		return spansBuilder.create();
	}
}
