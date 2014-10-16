package ru.trylogic.groovy.macro.transform;

import groovy.transform.CompilationUnitAware;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 *
 * @author Sergei Egorov <bsideup@gmail.com>
 */

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class MacroTransformation extends MethodCallTransformation implements CompilationUnitAware {

    protected CompilationUnit unit;

    @Override
    public void setCompilationUnit(CompilationUnit unit) {
        this.unit = unit;
    }

    @Override
    protected GroovyCodeVisitor getTransformer(ASTNode[] nodes, final SourceUnit sourceUnit) {
        return new MacroExtensionsTransformer(unit, sourceUnit);
    }
}

