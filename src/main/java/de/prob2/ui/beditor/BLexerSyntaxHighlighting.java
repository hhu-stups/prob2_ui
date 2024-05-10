package de.prob2.ui.beditor;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.be4.classicalb.core.parser.BLexer;
import de.be4.classicalb.core.parser.ParseOptions;
import de.be4.classicalb.core.parser.lexer.LexerException;
import de.be4.classicalb.core.parser.node.*;
import de.be4.classicalb.core.parser.util.Utils;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BLexerSyntaxHighlighting {
	private static final Logger LOGGER = LoggerFactory.getLogger(BLexerSyntaxHighlighting.class);

	private static final Map<Class<? extends Token>, String> syntaxClassesForB = new HashMap<>();

	static {
		addBTokens("editor_identifier", TIdentifierLiteral.class);
		addBTokens("editor_assignments", TAssign.class, TSkip.class, TOutputParameters.class,
			TDoubleVerticalBar.class,
			TClosure.class, TClosure1.class, TIterate.class,
			TId.class,
			TEmptySet.class, TDoubleColon.class,
			TFalse.class, TTrue.class,
			TDom.class, TRan.class,
			TFnc.class, TRel.class,
			TQuantifiedInter.class, TQuantifiedUnion.class,
			TGeneralizedInter.class, TGeneralizedUnion.class,
			TFinite.class,
			TLessGreater.class, // empty sequence
			TRev.class,
			TFirst.class, TLast.class, TFront.class, TTail.class, TConc.class
			);
		addBTokens("editor_predicates", 
			TNotEqual.class, TGreater.class, TLess.class, 
			TElementOf.class, TNotBelonging.class,
			TInclusion.class, TNonInclusion.class, TStrictInclusion.class, TStrictNonInclusion.class
			); // short ASCII/Unicode predicates
		addBTokens("editor_operators",
			TMaplet.class, TPrj1.class, TPrj2.class,
			TRec.class,
			TDomainRestriction.class, TDomainSubtraction.class,
			TUnion.class, TIntersection.class, TSetSubtraction.class,
			TRangeRestriction.class, TRangeSubtraction.class,
			TDirectProduct.class,
			TTotalBijection.class, TTotalFunction.class, TTotalInjection.class,
			TTotalRelation.class, TTotalSurjection.class, 
			TTotalSurjectionRelation.class, TPartialBijection.class, TPartialFunction.class,
			TPartialInjection.class, TPartialSurjection.class, TSetRelation.class, TSurjectionRelation.class,
			TOverwriteRelation.class,
			TTilde.class,
			TLambda.class, TSemicolon.class,
			TConcatSequence.class,
			TRestrictHeadSequence.class, TRestrictTailSequence.class,
			TInsertEndSequence.class, TInsertStartSequence.class
			); // short ASCII/Unicode operators
		addBTokens("editor_logical", TConjunction.class, TForAny.class, TExists.class,
			TImplies.class, TLogicalOr.class, TEquivalence.class, TNot.class,
			TBoolCast.class, TBfalse.class,
			TTruthPredicate.class // btrue
			);
		addBTokens("editor_arithmetic", TDoubleEqual.class, TEqual.class,
			TGreaterEqual.class, TLessEqual.class, 
			TSucc.class, TPred.class,
			TMinus.class, TProduct.class, // ambiguous
			TMod.class, TDivision.class, TPowerOf.class, TPlus.class,
			TInterval.class, 
			TCard.class, TSize.class,
			TMaxInt.class, TMinInt.class,
			TMin.class, TMax.class,
			TSigma.class, TPi.class,
			TIntegerLiteral.class, THexLiteral.class, TRealLiteral.class,
			TConvertIntCeiling.class, TConvertIntFloor.class, TConvertReal.class);
		addBTokens("editor_types", TBool.class, TNat.class, TNat1.class, TNatural.class,
			TNatural1.class, TStruct.class,
			TPow.class, TPow1.class, TFin.class, TFin1.class, 
			TPerm.class, TSeq.class, TSeq1.class, TIseq.class, TIseq1.class,
			TInteger.class, TInt.class, TString.class, TReal.class, TFloat.class);
		addBTokens("editor_string", TStringLiteral.class,
			TMultilineStringStart.class, TMultilineStringContent.class, TMultilineStringEnd.class);
		addBTokens("editor_unsupported", TUnrecognisedPragma.class);
		addBTokens("editor_ctrlkeyword", TLet.class, TBe.class,
			TVar.class, TIn.class, TAny.class, TWhile.class,
			TDo.class, TVariant.class, TElsif.class, TIf.class, TThen.class, TElse.class, TEither.class,
			TCase.class, TSelect.class, TAssert.class, TWhen.class, TPre.class, TBegin.class,
			TChoice.class, TOr.class,
			TWhere.class, TOf.class, TEnd.class);
		addBTokens("editor_keyword", TMachine.class, TOperations.class, TLocalOperations.class, 
			TRefinement.class, TImplementation.class,
			TAssertions.class, TInitialisation.class, TSees.class, TPromotes.class,
			TUses.class, TIncludes.class, TImports.class, TRefines.class, TExtends.class, TSystem.class,
			TModel.class, TInvariant.class, TConcreteVariables.class,
			TAbstractVariables.class, TVariables.class, TProperties.class,
			TConstants.class, TAbstractConstants.class, TConcreteConstants.class,
			TConstraints.class, TSets.class, TDefinitions.class, TValue.class,
			TKwFreetypes.class, TKwReferences.class, TExpressions.class, TPredicates.class);
		addBTokens("editor_comment", TComment.class, TCommentBody.class, TCommentEnd.class,
			TLineComment.class, TShebang.class, TStar.class);
		addBTokens("editor_pragma", 
			TPragmaDescription.class, TPragmaEnd.class, TPragmaFile.class,
			TPragmaFreeText.class, TPragmaGenerated.class, TPragmaIdOrString.class,
			TPragmaImportPackage.class, TPragmaLabel.class, TPragmaPackage.class,
			TPragmaSymbolic.class);
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
		ParseOptions parseOptions = new ParseOptions();
		parseOptions.setIgnoreCheckingValidCombinations(true);
		parseOptions.setIgnoreUselessTokens(false); // the highlighter currently needs all tokens
		lexer.setParseOptions(parseOptions);
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		try {
			Token t;
			do {
				t = lexer.next();
				String syntaxClass;
				if (t instanceof TIdentifierLiteral && Utils.isProBSpecialDefinitionName(t.getText())) {
					// Recognize and highlight special identifiers (e. g. ANIMATION_FUNCTION, VISB_JSON_FILE)
					syntaxClass = "editor_special_identifier";
				} else {
					syntaxClass = syntaxClassesForB.get(t.getClass());
				}

				Set<String> style = syntaxClass == null ? Collections.emptySet() : Collections.singleton(syntaxClass);
				spansBuilder.add(style, t.getText().length());
			} while (!(t instanceof EOF) && !Thread.currentThread().isInterrupted());
		} catch (LexerException | IOException e) {
			LOGGER.info("Failed to lex", e);
		}

		try {
			return spansBuilder.create();
		} catch (IllegalStateException ignored) {
			return StyleSpans.singleton(Collections.emptySet(), text.length());
		}
	}
}
