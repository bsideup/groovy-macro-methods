package ru.trylogic.groovy.macro.transform

class MacroTransformationTest extends GroovyTestCase {
    
    void testSimple() {
        assertScript """

        def nullObject = null;
        
        assert null == safe(nullObject.hashcode())

"""
    }

    void testMacroInClosure() {
        assertScript """

        def cl = {
            return safe(it.hashcode())
        }
        
        assert null == cl(null)

"""
    }

    void testCascade() {
        assertScript """
        def nullObject = null;

        assert null == safe(safe(nullObject.hashcode()).toString())

"""
    }
    
    void testMethodName() {
        assertScript """
            
        assert "toString" == methodName(123.toString())
        
        assert "getInteger" == methodName(Integer.getInteger())
        
        assert "call" == methodName({}())
"""
    }
    
    void testPropertyName() {
        assertScript """
        
        assert "bytes" == propertyName("".bytes)
        
        assert "class" == propertyName(123.class)
"""
    }
}
