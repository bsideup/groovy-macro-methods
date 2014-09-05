package ru.trylogic.groovy.macro.transform;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;

import static org.codehaus.groovy.ast.tools.GeneralUtils.*;

public class TestMacroMethods {
    
    @Macro
    public static Expression safe(MacroContext macroContext, MethodCallExpression callExpression) {
        return ternaryX(
                notNullX(callExpression.getObjectExpression()),
                callExpression,
                constX(null)
        );
    }
}
