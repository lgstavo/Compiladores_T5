package br.ufscar.dc.compiladores.t5;

import java.util.LinkedList;
import java.util.List;

public final class Scopes {

    private final LinkedList<SymbolTable> tablesStack;

    public Scopes() {
        tablesStack = new LinkedList<>();
        createNewScope();
    }

    public void createNewScope() {
        tablesStack.push(new SymbolTable());
    }

    public SymbolTable getCurrentScope() {
        return tablesStack.peek();
    }

    public List<SymbolTable> traverseNestedScopes() {
        return tablesStack;
    }
    
    public void leaveScope(){
        tablesStack.pop();
    }
}
