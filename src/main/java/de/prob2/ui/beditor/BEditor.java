package de.prob2.ui.beditor;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.be4.classicalb.core.parser.BLexer;
import de.be4.classicalb.core.parser.lexer.LexerException;
import de.be4.classicalb.core.parser.node.*;
import de.prob2.ui.layout.FontSize;
import javafx.concurrent.Task;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class BEditor extends CodeArea {
    private static final Logger LOGGER = LoggerFactory.getLogger(BEditor.class);

    private ExecutorService executor;

    private static final Map<Class<? extends Token>, String> syntaxClasses = new HashMap<>();

    static {
        addTokens("editor_identifier", TIdentifierLiteral.class);
        addTokens("editor_assignments", TAssign.class, TOutputParameters.class,
                TDoubleVerticalBar.class, TAssert.class,
                TClosure.class, TClosure1.class, TDirectProduct.class, TDivision.class,
                TEmptySet.class, TDoubleColon.class, TImplies.class,  TLogicalOr.class,
                TInterval.class, TUnion.class, TOr.class, TNonInclusion.class,
                TTotalBijection.class, TTotalFunction.class, TTotalInjection.class,
                TTotalRelation.class, TTotalSurjection.class, TFalse.class, TTrue.class,
                TTotalSurjectionRelation.class, TPartialBijection.class, TPartialFunction.class,
                TPartialInjection.class, TPartialSurjection.class, TSetRelation.class,
                TFin.class, TFin1.class, TPerm.class, TSeq.class, TSeq1.class, TIseq.class,
                TIseq1.class, TNot.class);
        addTokens("editor_logical", TConjunction.class, TForAny.class, TExists.class);
        addTokens("editor_arithmetic", TDoubleEqual.class, TEqual.class,
                TElementOf.class, TEquivalence.class, TGreaterEqual.class, TLessEqual.class,
                TNotEqual.class, TGreater.class, TLess.class);
        addTokens("editor_types", TBool.class, TNat.class, TNat1.class, TNatural.class,
                TNatural1.class, TStruct.class, TInteger.class, TInt.class, TString.class);
        addTokens("editor_string", TStringLiteral.class);
        addTokens("editor_unsupported", TTree.class, TLeft.class, TRight.class,
                TInfix.class, TArity.class, TSubtree.class, TPow.class, TPow1.class,
                TSon.class, TFather.class, TRank.class, TMirror.class, TSizet.class,
                TPostfix.class, TPrefix.class, TSons.class, TTop.class, TConst.class, TBtree.class);
        addTokens("editor_ctrlkeyword", TSkip.class, TLet.class, TBe.class,
                TVar.class, TIn.class, TAny.class, TWhile.class,
                TDo.class, TVariant.class, TElsif.class, TIf.class, TThen.class, TElse.class, TEither.class,
                TCase.class, TSelect.class, TAssert.class, TAssertions.class, TWhen.class, TPre.class, TBegin.class,
                TChoice.class, TWhere.class, TOf.class, TEnd.class);
        addTokens("editor_keyword", TMachine.class, TOperations.class, TRefinement.class, TImplementation.class,
                TOperations.class, TAssertions.class, TInitialisation.class, TSees.class, TPromotes.class,
                TUses.class, TIncludes.class, TImports.class, TRefines.class, TExtends.class, TSystem.class,
                TModel.class, TInvariant.class, TConcreteVariables.class,
                TAbstractVariables.class, TVariables.class, TProperties.class,
                TConstants.class, TAbstractConstants.class, TConcreteConstants.class,
                TConstraints.class, TSets.class, TDefinitions.class);
        addTokens("editor_comment", TComment.class, TCommentBody.class, TCommentEnd.class,
                TLineComment.class);
    }
    
    private FontSize fontSize;


    @Inject
    private BEditor(final FontSize fontSize, final ResourceBundle bundle) {
    	this.fontSize = fontSize;
        initialize();
        initializeContextMenu(bundle);
    }

    private void initializeContextMenu(ResourceBundle bundle){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem = new MenuItem(bundle.getString("common.contextMenu.undo"));
        menuItem.setOnAction(e -> this.getUndoManager().undo());
        contextMenu.getItems().add(menuItem);
        menuItem = new MenuItem(bundle.getString("common.contextMenu.redo"));
        menuItem.setOnAction(e -> this.getUndoManager().redo());
        contextMenu.getItems().add(menuItem);
        menuItem = new MenuItem(bundle.getString("common.contextMenu.cut"));
        menuItem.setOnAction(e -> this.cut());
        contextMenu.getItems().add(menuItem);
        menuItem = new MenuItem(bundle.getString("common.contextMenu.copy"));
        menuItem.setOnAction(e -> this.copy());
        contextMenu.getItems().add(menuItem);
        menuItem = new MenuItem(bundle.getString("common.contextMenu.paste"));
        menuItem.setOnAction(e -> this.paste());
        contextMenu.getItems().add(menuItem);
        menuItem = new MenuItem(bundle.getString("common.contextMenu.delete"));
        menuItem.setOnAction(e -> this.deleteText(this.getSelection()));
        contextMenu.getItems().add(menuItem);
        menuItem = new MenuItem(bundle.getString("common.contextMenu.selectAll"));
        menuItem.setOnAction(e -> this.selectAll());
        contextMenu.getItems().add(menuItem);
        this.setContextMenu(contextMenu);
    }
    
    private void initialize() {
        this.richChanges()
	        .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
	        .successionEnds(Duration.ofMillis(100))
	        .supplyTask(this::computeHighlightingAsync)
	        .awaitLatest(this.richChanges())
	        .filterMap(t -> {
	            if (t.isSuccess()) {
	                return Optional.of(t.get());
	            } else {
	                LOGGER.info("Highlighting failed", t.getFailure());
	                return Optional.empty();
	            }
        }).subscribe(this::applyHighlighting);

        fontSize.fontSizeProperty().addListener((observable, from, to) -> 
    		this.setStyle(String.format("-fx-font-size: %dpx;", fontSize.fontSizeProperty().get()))
        );
        Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.Z, KeyCombination.CONTROL_DOWN), e-> {
	        int oldLength = this.getText().length();
	        int caret = this.getCaretPosition();
	        this.undo();
	        int currentLength = this.getText().length();
	        int diff = currentLength - oldLength;
	        if(caret + diff >= 0 && caret + diff <= this.getText().length()) {
	        	this.moveTo(caret + diff);
	        }
        }));
        
        Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), e-> {
	        int oldLength = this.getText().length();
	        int caret = this.getCaretPosition();
	        this.redo();
	        int currentLength = this.getText().length();
	        int diff = currentLength - oldLength;
	        if(caret + diff >= 0 && caret + diff <= this.getText().length()) {
	        	this.moveTo(caret + diff);
	        }
        }));
    }

    public void startHighlighting() {
        if (this.executor == null) {
            this.executor = Executors.newSingleThreadExecutor();
        }
    }

    public void stopHighlighting() {
    	if(this.executor != null) {
	        this.executor.shutdown();
	        this.executor = null;   
    	}
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
        final String text = this.getText();
        if (executor == null) {
            // No executor - run and return a dummy task that does no highlighting
            final Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
                @Override
                protected StyleSpans<Collection<String>> call() {
                    return new StyleSpansBuilder<Collection<String>>().add(Collections.singleton("default"), text.length()).create();
                }
            };
            task.run();
            return task;
        } else {
            // Executor exists - do proper highlighting
            final Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
                @Override
                protected StyleSpans<Collection<String>> call() {
                    return computeHighlighting(text);
                }
            };
            executor.execute(task);
            return task;
        }
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
            LOGGER.info("Failed to lex", e);
        }
        return spansBuilder.create();
    }
}
