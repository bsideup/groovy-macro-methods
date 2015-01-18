package ru.trylogic.groovy.macro.transform

import org.codehaus.groovy.control.CompilerConfiguration

class MacroTransformationPerformanceTest extends GroovyTestCase {
    
    void testPerformance() {
        def configuration = new CompilerConfiguration();
        configuration.disabledGlobalASTTransformations = [MacroTransformation.name];

        def normalClassLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader, configuration);
        def macroClassLoader = new GroovyClassLoader();

        def code = 'assert "bytes" == propertyName("".bytes)'
        
        def r = benchmark {
            'normal' {
                normalClassLoader.parseClass code
            }
            
            'macro' {
                macroClassLoader.parseClass code
            }
        }
        
        r.prettyPrint();
    }
}
