package ru.trylogic.groovy.macro.transform;

import groovy.lang.GroovyShell;
import groovy.transform.CompilationUnitAware;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.asm.InvocationWriter;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.codehaus.groovy.transform.stc.ExtensionMethodNode;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO validation
 * TODO multiple phases support
 *
 *
 * @author Sergei Egorov <bsideup@gmail.com>
 */

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class MacroTransformation extends MethodCallTransformation implements CompilationUnitAware {

    public static final ClassNode MACRO_ANNOTATION_CLASS_NODE = ClassHelper.make(Macro.class);

    public static final ClassNode MACRO_CONTEXT_CLASS_NODE = ClassHelper.make(MacroContext.class);

    CompilationUnit unit;

    @Override
    public void setCompilationUnit(CompilationUnit unit) {
        this.unit = unit;
    }

    @Override
    protected GroovyCodeVisitor getTransformer(ASTNode[] nodes, SourceUnit sourceUnit) {
        return new MacroCallsTransformer(sourceUnit);
    }

    protected class MacroCallsTransformer extends ClassCodeExpressionTransformer {

        private SourceUnit sourceUnit;

        @Override
        protected SourceUnit getSourceUnit() {
            return sourceUnit;
        }

        public MacroCallsTransformer(SourceUnit sourceUnit) {
            this.sourceUnit = sourceUnit;
        }

        @Override
        public Expression transform(final Expression exp) {
            if(!(exp instanceof MethodCallExpression)) {
                return super.transform(exp);
            }

            MethodCallExpression call = (MethodCallExpression) exp;

            List<Expression> callArguments = InvocationWriter.makeArgumentList(call.getArguments()).getExpressions();

            ClassNode[] argumentsList = new ClassNode[callArguments.size()];

            for(int i = 0; i < callArguments.size(); i++) {
                argumentsList[i] = ClassHelper.make(callArguments.get(i).getClass());
            }

            List<MethodNode> dgmMethods = StaticTypeCheckingSupport.findDGMMethodsByNameAndArguments(
                    unit.getTransformLoader(), MACRO_CONTEXT_CLASS_NODE, call.getMethodAsString(),
                    argumentsList);

            if(dgmMethods.size() != 1) {
                return super.transform(exp);
            }

            ExtensionMethodNode extensionMethodNode = (ExtensionMethodNode) dgmMethods.get(0);

            if(extensionMethodNode.getExtensionMethodNode().getAnnotations(MACRO_ANNOTATION_CLASS_NODE).isEmpty()) {
                return super.transform(exp);
            }

            visitMethodCallExpression(call);

            GroovyShell shell = new GroovyShell(unit.getTransformLoader());

            MacroContext macroContext = new MacroContext(unit, sourceUnit, call);

            List<Object> macroArguments = new ArrayList<Object>();
            macroArguments.add(macroContext);
            macroArguments.addAll(callArguments);

            Object clazz = shell.evaluate(extensionMethodNode.getExtensionMethodNode().getDeclaringClass().getName());
            return (Expression) InvokerHelper.invokeMethod(clazz, call.getMethodAsString(), macroArguments.toArray());
        }
    }
}

