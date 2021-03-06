package ErrorChecker;

import Builder.BuildChecker;
import Builder.SemanticErrorDictionary;
import Execution.ExecutionManager;
import GeneratedAntlrClasses.CorgiParser;
import Representations.CorgiFunction;
import Representations.CorgiValue;
import Searcher.VariableSearcher;
import Semantics.LocalScopeHandler;
import Semantics.CorgiScope;
import Semantics.SymbolTableManager;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

public class MultipleVarDecChecker implements IErrorChecker, ParseTreeListener {

    private CorgiParser.VariableDeclaratorIdContext varDecIdCtx;
    private int lineNumber;

    public MultipleVarDecChecker(CorgiParser.VariableDeclaratorIdContext varDecIdCtx) {
        this.varDecIdCtx = varDecIdCtx;

        Token firstToken = this.varDecIdCtx.getStart();
        this.lineNumber = firstToken.getLine();
    }


    @Override
    public void verify() {
        ParseTreeWalker treeWalker = new ParseTreeWalker();
        treeWalker.walk(this, this.varDecIdCtx);
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        if(ctx instanceof CorgiParser.VariableDeclaratorIdContext) {
            CorgiParser.VariableDeclaratorIdContext varDecCtx = (CorgiParser.VariableDeclaratorIdContext) ctx;
            this.verifyVariableOrConst(varDecCtx.getText());
        }
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        // TODO Auto-generated method stub

    }

    private void verifyVariableOrConst(String identifierString) {
        CorgiValue corgiValue = null;

        if(ExecutionManager.getInstance().isInFunctionExecution()) {
            CorgiFunction corgiFunction = ExecutionManager.getInstance().getCurrentFunction();
            corgiValue = VariableSearcher.searchVariableInFunction(corgiFunction, identifierString);
        }

        //if after function finding, Corgi value is still null, search local scope
        if(corgiValue == null) {
            corgiValue = LocalScopeHandler.searchVariableInLocalIterative(identifierString, LocalScopeHandler.getInstance().getActiveLocalScope());
        }

        //if corgi value is still null, search class
        if(corgiValue == null) {
            CorgiScope corgiScope = SymbolTableManager.getInstance().getCorgiScope();
            corgiValue = VariableSearcher.searchVariableInClass(corgiScope, identifierString);
        }


        if(corgiValue != null) {
            BuildChecker.reportCustomError(SemanticErrorDictionary.MULTIPLE_VARIABLE, "", identifierString, this.lineNumber);
        }
    }


}