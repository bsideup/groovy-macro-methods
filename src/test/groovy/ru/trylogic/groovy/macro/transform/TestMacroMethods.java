package ru.trylogic.groovy.macro.transform;

import org.codehaus.groovy.ast.expr.*;
import ru.trylogic.groovy.macro.runtime.Macro;
import ru.trylogic.groovy.macro.runtime.MacroContext;

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
    
    @Macro
    public static ConstantExpression methodName(MacroContext macroContext, MethodCallExpression callExpression) {
        return constX(callExpression.getMethodAsString());
    }

    @Macro
    public static ConstantExpression methodName(MacroContext macroContext, MethodPointerExpression methodPointerExpression) {
        return constX(methodPointerExpression.getMethodName().getText());
    }
    
    @Macro
    public static ConstantExpression propertyName(MacroContext macroContext, PropertyExpression propertyExpression) {
        return constX(propertyExpression.getPropertyAsString());
    }
}
