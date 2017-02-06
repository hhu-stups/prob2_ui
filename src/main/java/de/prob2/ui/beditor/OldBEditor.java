package de.prob2.ui.beditor;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.be4.classicalb.core.parser.BLexer;
import de.be4.classicalb.core.parser.lexer.LexerException;
import de.be4.classicalb.core.parser.node.*;
import javafx.concurrent.Task;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class OldBEditor extends StyleClassedTextArea {
	
	private static final Logger logger = LoggerFactory.getLogger(OldBEditor.class);

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	private static final Map<Class<? extends Token>, String> syntaxClasses = new HashMap<>();
		
	static {
		addTokens("editor_identifier", TIdentifierLiteral.class, TIntegerLiteral.class, TPragmaIdentifierLiteral.class);
		addTokens("editor_assignment_logical", TAssign.class, TOutputParameters.class, TDoubleVerticalBar.class, TAssert.class, 
				TClosure.class, TClosure1.class, TConjunction.class, TDirectProduct.class, TDivision.class, TEmptySet.class, TDoubleColon.class,
				TDoubleEqual.class, TEqual.class, TElementOf.class, TEquivalence.class, TGreaterEqual.class, TLessEqual.class, TNotEqual.class,
				TGreater.class, TLess.class, TImplies.class,  TLogicalOr.class, TInterval.class, TUnion.class, TOr.class, TNonInclusion.class, 
				TTotalBijection.class, TTotalFunction.class, TTotalInjection.class, TTotalRelation.class, TTotalSurjection.class, 
				TTotalSurjectionRelation.class, TPartialBijection.class, TPartialFunction.class, TPartialInjection.class, TPartialSurjection.class, TSetRelation.class,
				TFin.class, TFin1.class, TPerm.class, TSeq.class, TSeq1.class, TIseq.class,
				TIseq1.class, TBool.class, TNat.class, TNat1.class, TNatural.class, TNatural1.class, TStruct.class,
				TInteger.class, TInt.class, TString.class, TEither.class, TAssertions.class);
		addTokens("editor_string", TStringLiteral.class);
		addTokens("editor_unsupported", TTree.class, TLeft.class, TRight.class, TInfix.class, TArity.class,
				TSubtree.class, TPow.class, TPow1.class, 
				TSon.class, TFather.class, TRank.class, TMirror.class, TSizet.class, TPostfix.class, TPrefix.class,
				TSons.class, TTop.class, TConst.class, TBtree.class);

		addTokens("editor_ctrlkeyword", TSkip.class, TLet.class, TBe.class, TVar.class, TIn.class, TAny.class,
				TWhile.class,
				TDo.class, TVariant.class, TElsif.class, TIf.class, TThen.class, TElse.class, 
				TCase.class, TSelect.class, TWhen.class, TPre.class, TBegin.class,
				TChoice.class, TWhere.class, TOf.class, TEnd.class);

		addTokens("editor_keyword", TMachine.class, TRefinement.class, TImplementation.class,
				TOperations.class, TInitialisation.class, TSees.class, TPromotes.class,
				TUses.class, TIncludes.class, TImports.class, TRefines.class, TExtends.class, TSystem.class,
				TModel.class,
				TInvariant.class, TConcreteVariables.class, TAbstractVariables.class, TVariables.class,
				TProperties.class,
				TConstants.class, TAbstractConstants.class, TConcreteConstants.class, TConstraints.class, TSets.class,
				TDefinitions.class);
		addTokens("editor_comment", TComment.class, TCommentBody.class, TCommentEnd.class);
	}
	
	
	public OldBEditor() {
		this.richChanges()
		.filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
		.successionEnds(Duration.ofMillis(500))
		.supplyTask(this::computeHighlightingAsync)
		.awaitLatest(this.richChanges())
		.filterMap(t -> {
			if (t.isSuccess()) {
				return Optional.of(t.get());
			} else {
				t.getFailure().printStackTrace();
				return Optional.empty();
			}
		}).subscribe(this::applyHighlighting);
	}
	
	@SafeVarargs
	private static void addTokens(String syntaxclass, Class<? extends Token>... tokens) {
		for (Class<? extends Token> c : tokens) {
			syntaxClasses.put(c, syntaxclass);
		}
	}
	
	private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
		this.setStyleSpans(0, highlighting);
	}
	
	private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
		String text = this.getText();
		Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
			@Override
			protected StyleSpans<Collection<String>> call() {
				return computeHighlighting(text);
			}
		};
		executor.execute(task);
		return task;
	}
	
	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
		BLexer lexer = new BLexer(new PushbackReader(new StringReader(text), text.length()));
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		try {
			Token t;
			do {
				t = lexer.next();
				String string = syntaxClasses.get(t.getClass());
				int length = t.getText().length();
				if (t instanceof TStringLiteral) {
					length += 2;
				}
				spansBuilder.add(Collections.singleton(string == null ? "default" : string), length);
			} while (!(t instanceof EOF));
		} catch (LexerException | IOException e) {
			logger.error("BLexer Exception: ", e);
		}
		return spansBuilder.create();
	}
	

	
}
