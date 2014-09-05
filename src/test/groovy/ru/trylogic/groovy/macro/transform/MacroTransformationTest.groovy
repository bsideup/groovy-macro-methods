package ru.trylogic.groovy.macro.transform

class MacroTransformationTest extends GroovyTestCase {
    
    public void testSimple() {
        assertScript """

        def nullObject = null;
        
        assert null == safe(nullObject.hashcode())

"""
    }
}
