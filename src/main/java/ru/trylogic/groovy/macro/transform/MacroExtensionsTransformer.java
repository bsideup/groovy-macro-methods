package ru.trylogic.groovy.macro.transform;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.asm.InvocationWriter;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.transform.stc.ExtensionMethodNode;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport;
import ru.trylogic.groovy.macro.runtime.MacroContext;
import ru.trylogic.groovy.macro.runtime.MacroStub;

import java.util.*;

/**
 * TODO validation
 * TODO multiple phases support
 *
 * @author Sergei Egorov <bsideup@gmail.com>
 */
public class MacroExtensionsTransformer extends ClassCodeVisitorSupport {

    public static final ClassNode MACRO_CONTEXT_CLASS_NODE = ClassHelper.make(MacroContext.class);

    public static final ClassNode MACRO_STUB_CLASS_NODE = ClassHelper.make(MacroStub.class);

    public static final PropertyExpression MACRO_STUB_INSTANCE = new PropertyExpression(new ClassExpression(MACRO_STUB_CLASS_NODE), "INSTANCE");
    
    public static final String MACRO_STUB_METHOD_NAME = "macroMethod";

    protected final CompilationUnit unit;

    protected final SourceUnit sourceUnit;

    protected final Map<String, List<MethodNode>> macroMethodsByName;

    public MacroExtensionsTransformer(CompilationUnit unit, SourceUnit sourceUnit) {
        this.unit = unit;
        this.sourceUnit = sourceUnit;

        this.macroMethodsByName = MacroMethodsCache.getMacroMethodsByNameMap(unit.getTransformLoader());
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
        
        List<MethodNode> methods = macroMethodsByName.get(methodName);
        
        if(methods == null) {
            return;
        }

        methods = StaticTypeCheckingSupport.chooseBestMethod(MACRO_CONTEXT_CLASS_NODE, methods, argumentsList);
        
        for (MethodNode macroMethodNode : methods) {
            if(!(macroMethodNode instanceof ExtensionMethodNode)) {
                continue;
            }
            
            ExtensionMethodNode extensionMethodNode = (ExtensionMethodNode) macroMethodNode;

            MethodNode macroExtensionMethodNode = extensionMethodNode.getExtensionMethodNode();

            MacroContext macroContext = new MacroContext(unit, sourceUnit, call);

            List<Object> macroArguments = new ArrayList<Object>();
            macroArguments.add(macroContext);
            macroArguments.addAll(callArguments);

            final Class clazz;
            try {
                clazz = unit.getClassLoader().loadClass(macroExtensionMethodNode.getDeclaringClass().getName());
            } catch (ClassNotFoundException e) {
                //TODO different reaction?
                continue;
            }
            
            Expression result = (Expression) InvokerHelper.invokeStaticMethod(clazz, methodName, macroArguments.toArray());

            call.setObjectExpression(MACRO_STUB_INSTANCE);
            call.setMethod(new ConstantExpression(MACRO_STUB_METHOD_NAME));

            call.setSpreadSafe(false);
            call.setSafe(false);
            call.setImplicitThis(false);

            call.setArguments(result);

            break;
        }
    }
}
