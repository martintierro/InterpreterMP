package Evaluation;

import Commands.ICommand;
import GeneratedAntlrClasses.CorgiParser;
import Representations.CorgiFunction;
import Representations.CorgiValue;
import Semantics.MainScope;
import Semantics.SymbolTableManager;
import Utlities.KeywordRecognizer;
import com.udojava.evalex.Expression;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.math.BigDecimal;
import java.util.List;

public class EvaluationCommand implements ICommand, ParseTreeListener {

    private CorgiParser.ExpressionContext parentExprCtx;
    private String modifiedExp;
    private BigDecimal resultValue;

    public EvaluationCommand(CorgiParser.ExpressionContext exprCtx) {
        this.parentExprCtx = exprCtx;
    }

    /* (non-Javadoc)
     * @see com.neildg.mobiprog.execution.commands.ICommand#execute()
     */
    @Override
    public void execute() {
        this.modifiedExp = this.parentExprCtx.getText();

        //catch rules if the value has direct boolean flags
        if(this.modifiedExp.contains(KeywordRecognizer.BOOLEAN_TRUE)) {
            this.resultValue = new BigDecimal(1);
        }
        else if(this.modifiedExp.contains(KeywordRecognizer.BOOLEAN_FALSE)) {
            this.resultValue = new BigDecimal(0);
        }
        else {
            ParseTreeWalker treeWalker = new ParseTreeWalker();
            treeWalker.walk(this, this.parentExprCtx);

            Expression evalEx = new Expression(this.modifiedExp);
            //Log.i(TAG,"Modified exp to eval: " +this.modifiedExp);
            this.resultValue = evalEx.eval();
        }

    }

    @Override
    public void visitTerminal(TerminalNode node) {

    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        if (ctx instanceof CorgiParser.ExpressionContext) {
            CorgiParser.ExpressionContext exprCtx = (CorgiParser.ExpressionContext) ctx;
            if (EvaluationCommand.isFunctionCall(exprCtx)) {
                this.evaluateFunctionCall(exprCtx);
            }

            else if (EvaluationCommand.isVariableOrConst(exprCtx)) {
                this.evaluateVariable(exprCtx);
            }
        }
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {

    }

    public static boolean isFunctionCall(CorgiParser.ExpressionContext exprCtx) {
        if (exprCtx.arguments() != null) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isVariableOrConst(CorgiParser.ExpressionContext exprCtx) {
        if (exprCtx.primary() != null && exprCtx.primary().Identifier() != null) {
            return true;
        } else {
            return false;
        }
    }

    private void evaluateFunctionCall(CorgiParser.ExpressionContext exprCtx) {
        String functionName = exprCtx.expression(0).Identifier().getText();

        MainScope mainScope = SymbolTableManager.getInstance().getMainScope(
                ParserHandler.getInstance().getCurrentClassName());
        CorgiFunction corgiFunction = mainScope.getFunction(functionName);

        if (exprCtx.arguments().expressionList() != null) {
            List<CorgiParser.ExpressionContext> exprCtxList = exprCtx.arguments()
                    .expressionList().expression();

            for (int i = 0; i < exprCtxList.size(); i++) {
                CorgiParser.ExpressionContext parameterExprCtx = exprCtxList.get(i);

                EvaluationCommand evaluationCommand = new EvaluationCommand(parameterExprCtx);
                evaluationCommand.execute();

                corgiFunction.mapParameterByValueAt(evaluationCommand.getResult().toEngineeringString(), i);
            }
        }

        corgiFunction.execute();

        this.modifiedExp = this.modifiedExp.replace(exprCtx.getText(),
                corgiFunction.getReturnValue().getValue().toString());
    }

    private void evaluateVariable(CorgiParser.ExpressionContext exprCtx) {
        CorgiValue mobiValue = VariableSearcher.searchVariable(exprCtx.getText());

        this.modifiedExp = this.modifiedExp.replaceFirst(exprCtx.getText(),
                mobiValue.getValue().toString());
    }

    /*
     * Returns the result
     */
    public BigDecimal getResult() {
        return this.resultValue;
    }
}