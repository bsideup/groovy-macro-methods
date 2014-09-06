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
}
