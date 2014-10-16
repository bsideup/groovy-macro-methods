package ru.trylogic.groovy.macro.runtime;

/**
 * Stub for macro calls. Used to replace "someMacroMethod(123)" to "MacroStub.INSTANCE.macroMethod(123)"
 * 
 * @author Sergei Egorov <bsideup@gmail.com>
 */
public enum MacroStub {
    INSTANCE;
    
    public <T> T macroMethod(T obj) {
        return obj;
    }
}
