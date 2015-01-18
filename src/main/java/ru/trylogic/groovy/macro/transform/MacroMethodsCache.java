package ru.trylogic.groovy.macro.transform;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.runtime.m12n.ExtensionModule;
import org.codehaus.groovy.runtime.m12n.ExtensionModuleScanner;
import org.codehaus.groovy.runtime.m12n.MetaInfExtensionModule;
import org.codehaus.groovy.transform.stc.ExtensionMethodNode;
import ru.trylogic.groovy.macro.runtime.Macro;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Sergei Egorov <bsideup@gmail.com>
 */
public class MacroMethodsCache {

    public static final ClassNode MACRO_ANNOTATION_CLASS_NODE = ClassHelper.make(Macro.class);
    
    protected static volatile Map<ClassLoader, Map<String, List<MethodNode>>> CACHE = new WeakHashMap<ClassLoader, Map<String, List<MethodNode>>>();
    
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public static Map<String, List<MethodNode>> getMacroMethodsByNameMap(ClassLoader classLoader) {
        try {
            lock.readLock().lock();
            if (!CACHE.containsKey(classLoader)) {
                lock.readLock().unlock();
                lock.writeLock().lock();

                try {
                    if (!CACHE.containsKey(classLoader)) {
                        WeakHashMap<ClassLoader, Map<String, List<MethodNode>>> newCache = new WeakHashMap<ClassLoader, Map<String, List<MethodNode>>>(CACHE);
    
                        Map<String, List<MethodNode>> methods = getMacroMethodsFromClassLoader(classLoader);
                        newCache.put(classLoader, methods);
    
                        CACHE = Collections.unmodifiableMap(newCache);
                    }
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }

            return CACHE.get(classLoader);
        } finally {
            lock.readLock().unlock();
        }
    }

    protected static Map<String, List<MethodNode>> getMacroMethodsFromClassLoader(ClassLoader classLoader) {
        final Map<String, List<MethodNode>> result = new HashMap<String, List<MethodNode>>();
        ExtensionModuleScanner.ExtensionModuleListener listener = new ExtensionModuleScanner.ExtensionModuleListener() {
            @Override
            public void onModule(ExtensionModule module) {
                if (!(module instanceof MetaInfExtensionModule)) {
                    return;
                }

                MetaInfExtensionModule extensionModule = (MetaInfExtensionModule) module;

                scanExtClasses(result, extensionModule.getInstanceMethodsExtensionClasses(), false);
                scanExtClasses(result, extensionModule.getStaticMethodsExtensionClasses(), true);
            }
        };

        ExtensionModuleScanner macroModuleScanner = new ExtensionModuleScanner(listener, classLoader);

        macroModuleScanner.scanClasspathModules();

        for (Map.Entry<String, List<MethodNode>> entry : result.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }

        return Collections.unmodifiableMap(result);
    }
    
    protected static void scanExtClasses(Map<String, List<MethodNode>> accumulator, List<Class> classes, boolean isStatic) {
        for (Class dgmLikeClass : classes) {
            ClassNode cn = ClassHelper.makeWithoutCaching(dgmLikeClass, true);
            for (MethodNode metaMethod : cn.getMethods()) {
                Parameter[] types = metaMethod.getParameters();
                if (!(metaMethod.isStatic() && metaMethod.isPublic())) {
                    continue;
                }

                if (types.length == 0) {
                    continue;
                }

                if (metaMethod.getAnnotations(MACRO_ANNOTATION_CLASS_NODE).isEmpty()) {
                    continue;
                }

                Parameter[] parameters = new Parameter[types.length - 1];
                System.arraycopy(types, 1, parameters, 0, parameters.length);
                ExtensionMethodNode node = new ExtensionMethodNode(
                        metaMethod,
                        metaMethod.getName(),
                        metaMethod.getModifiers(),
                        metaMethod.getReturnType(),
                        parameters,
                        ClassNode.EMPTY_ARRAY, null,
                        isStatic);
                node.setGenericsTypes(metaMethod.getGenericsTypes());
                ClassNode declaringClass = types[0].getType();
                node.setDeclaringClass(declaringClass);

                List<MethodNode> macroMethods = accumulator.get(metaMethod.getName());

                if (macroMethods == null) {
                    macroMethods = new ArrayList<MethodNode>();
                    accumulator.put(metaMethod.getName(), macroMethods);
                }

                macroMethods.add(node);
            }
        }
    }
}
