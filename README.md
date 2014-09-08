groovy-macro-methods [ ![Download](https://api.bintray.com/packages/bsideup/groovy/groovy-macro-methods/images/download.png) ](https://bintray.com/bsideup/groovy/groovy-macro-methods/_latestVersion)
====================
Simple macro system for Groovy. Allows you to mark extension method with @Macro, and every call to this method will be translated to expression returned from it.

Usage
====================
Just mark your extension method with @Macro annotation:
```groovy
import static org.codehaus.groovy.ast.tools.GeneralUtils.*;

public class TestMacroMethods {
    
    @Macro
    public static Expression safe(MacroContext macroContext,
                                  MethodCallExpression callExpression) {
        return ternaryX(
                notNullX(callExpression.getObjectExpression()),
                callExpression,
                constX(null)
        );
    }
}
```

and then, use it as normal method:
```groovy
def nullObject = null;
        
assert null == safe(nullObject.hashcode())
```


License
====================
Project is licensed under the terms of the Apache License, Version 2.0
