package ru.trylogic.groovy.macro.transform;

import groovy.lang.GroovyShell;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.asm.InvocationWriter;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.transform.stc.ExtensionMethodNode;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport;
import ru.trylogic.groovy.macro.runtime.Macro;
import ru.trylogic.groovy.macro.runtime.MacroContext;
import ru.trylogic.groovy.macro.runtime.MacroStub;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO validation
 * TODO multiple phases support
 *
 * @author Sergei Egorov <bsideup@gmail.com>
 */
public class MacroExtensionsTransformer extends ClassCodeVisitorSupport {

    public static final ClassNode MACRO_ANNOTATION_CLASS_NODE = ClassHelper.make(Macro.class);

    public static final ClassNode MACRO_CONTEXT_CLASS_NODE = ClassHelper.make(MacroContext.class);

    public static final ClassNode MACRO_STUB_CLASS_NODE = ClassHelper.make(MacroStub.class);

    public static final PropertyExpression MACRO_STUB_INSTANCE = new PropertyExpression(new ClassExpression(MACRO_STUB_CLASS_NODE), "INSTANCE");

    protected final CompilationUnit unit;

    protected final SourceUnit sourceUnit;

    protected final GroovyShell shell;

    public MacroExtensionsTransformer(CompilationUnit unit, SourceUnit sourceUnit) {
        this.unit = unit;
        this.sourceUnit = sourceUnit;
        shell = new GroovyShell(unit.getTransformLoader());
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return sourceUnit;
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression call) {
        super.visitMethodCallExpression(call);

        List<Expression> callArguments = InvocationWriter.makeArgumentList(call.getArguments()).getExpressions();

        ClassNode[] argumentsList = new ClassNode[callArguments.size()];

        for (int i = 0; i < callArguments.size(); i++) {
            argumentsList[i] = ClassHelper.make(callArguments.get(i).getClass());
        }

        String methodName = call.getMethodAsString();
        List<MethodNode> dgmMethods = StaticTypeCheckingSupport.findDGMMethodsByNameAndArguments(
                unit.getTransformLoader(), MACRO_CONTEXT_CLASS_NODE, methodName,
                argumentsList);

        for (MethodNode dgmMethod : dgmMethods) {
            ExtensionMethodNode extensionMethodNode = (ExtensionMethodNode) dgmMethod;

            MethodNode macroMethodNode = extensionMethodNode.getExtensionMethodNode();

            if (macroMethodNode.getAnnotations(MACRO_ANNOTATION_CLASS_NODE).isEmpty()) {
                continue;
            }

            MacroContext macroContext = new MacroContext(unit, sourceUnit, call);

            List<Object> macroArguments = new ArrayList<Object>();
            macroArguments.add(macroContext);
            macroArguments.addAll(callArguments);

            Object clazz = shell.evaluate(macroMethodNode.getDeclaringClass().getName());
            Expression result = (Expression) InvokerHelper.invokeMethod(clazz, methodName, macroArguments.toArray());

            call.setObjectExpression(MACRO_STUB_INSTANCE);
            call.setMethod(new ConstantExpression("macroMethod"));

            call.setSpreadSafe(false);
            call.setSafe(false);
            call.setImplicitThis(false);

            call.setArguments(result);

            break;
        }
    }
}
