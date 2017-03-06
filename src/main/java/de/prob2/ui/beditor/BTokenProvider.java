package de.prob2.ui.beditor;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import de.be4.classicalb.core.parser.BLexer;
import de.be4.classicalb.core.parser.lexer.LexerException;
import de.be4.classicalb.core.parser.node.*;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.web.WebEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netscape.javascript.JSObject;


public class BTokenProvider {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BTokenProvider.class);
	
	private static final Map<Class<? extends Token>, String> syntaxClasses = new HashMap<>();
	
	private LinkedList<Token> tokens;
								
	static {
			addTokens("b-type", TIdentifierLiteral.class);
			addTokens("b-assignment-logical", TAssign.class, TOutputParameters.class, TDoubleVerticalBar.class, TAssert.class,
					TClosure.class, TClosure1.class, TConjunction.class, TDirectProduct.class, TDivision.class, TEmptySet.class, TDoubleColon.class,
					TDoubleEqual.class, TEqual.class, TElementOf.class, TEquivalence.class, TGreaterEqual.class, TLessEqual.class, TNotEqual.class,
					TGreater.class, TLess.class, TImplies.class, TLogicalOr.class, TInterval.class, TUnion.class, TOr.class, TNonInclusion.class,
					TTotalBijection.class, TTotalFunction.class, TTotalInjection.class, TTotalRelation.class, TTotalSurjection.class,
					TTotalSurjectionRelation.class, TPartialBijection.class, TPartialFunction.class, TPartialInjection.class, TPartialSurjection.class, TSetRelation.class,
					TFin.class, TFin1.class, TPerm.class, TSeq.class, TSeq1.class, TIseq.class,
					TIseq1.class, TBool.class, TNat.class, TNat1.class, TNatural.class, TNatural1.class, TStruct.class,
					TInteger.class, TInt.class, TString.class, TEither.class);
			addTokens("b-type", TStringLiteral.class);
			addTokens("b-unsupported", TTree.class, TLeft.class, TRight.class, TInfix.class, TArity.class,
					TSubtree.class, TPow.class, TPow1.class,
					TSon.class, TFather.class, TRank.class, TMirror.class, TSizet.class, TPostfix.class, TPrefix.class,
					TSons.class, TTop.class, TConst.class, TBtree.class);
		
			addTokens("b-controlkeyword", TSkip.class, TLet.class, TBe.class, TVar.class, TIn.class, TAny.class,
					TWhile.class,
					TDo.class, TVariant.class, TElsif.class, TIf.class, TThen.class, TElse.class,
					TCase.class, TSelect.class, TAssert.class, TAssertions.class, TWhen.class, TPre.class, TBegin.class,
					TChoice.class, TWhere.class, TOf.class, TEnd.class);
		
			addTokens("b-keyword", TMachine.class, TRefinement.class, TImplementation.class,
					TOperations.class, TAssertions.class, TInitialisation.class, TSees.class, TPromotes.class,
					TUses.class, TIncludes.class, TImports.class, TRefines.class, TExtends.class, TSystem.class,
					TModel.class,
					TInvariant.class, TConcreteVariables.class, TAbstractVariables.class, TVariables.class,
					TProperties.class,
					TConstants.class, TAbstractConstants.class, TConcreteConstants.class, TConstraints.class, TSets.class,
					TDefinitions.class);
			addTokens("b-comment", TComment.class, TCommentBody.class, TCommentEnd.class);
		}
	
	public BTokenProvider(WebEngine engine) {
		this.tokens = new LinkedList<>();
		JSObject jsobj = (JSObject) engine.executeScript("window");
		jsobj.setMember("blexer", this);
	}
	
	@SafeVarargs
	private static void addTokens(String syntaxclass, Class<? extends Token>... tokens) {
		for (Class<? extends Token> c : tokens) {
			syntaxClasses.put(c, syntaxclass);
		}
	}
	
	public void computeHighlighting(String text, String line) {
		tokens.clear();
		BLexer lexer = new BLexer(new PushbackReader(new StringReader(text), text.length()));
		int currentLine = Integer.parseInt(line);
		try {
			Token t;
			do {
				t = lexer.next();			
				if (!"\n".equals(t.getText())) {
					if(t.getLine() -1 >= currentLine) {
						tokens.add(t);
					}
				}
			} while (!(t instanceof EOF));
		} catch (LexerException | IOException e) {
			LOGGER.error("Failed to lex", e);
		}
	}

	public Token getNextToken() {
		return tokens.poll();
	}
	
	public String getStyleClassFromToken(Token t) {
		String clazz = syntaxClasses.get(t.getClass());
		if (clazz == null) {
			clazz = "b-nothing";
		}
		return clazz;
	}
	
	public void jslog(String msg) {
		LOGGER.debug(msg);
	}
	
	public int getLength() {
		return tokens.size();
	}
}
