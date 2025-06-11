
package br.ufscar.dc.compiladores.t5;

import java.util.HashMap;
import java.util.Map;
import static br.ufscar.dc.compiladores.t5.SemanticUtils.reduceName;

public class SymbolTable {

    private final Map<String, SymbolTableEntry> table;

    public SymbolTable() {
        this.table = new HashMap<>();
    }

    public enum TypeT5 {
        INTEGER,
        REAL,
        LITERAL,
        LOGIC,
        VOID,
        REGISTER,
        EXTENDEDTYPE,
        INVALID
    }

    public enum EntryType {
        VARIABLE,
        PROCEEDING,
        FUNCTION
    }

    static class SymbolTableEntry {
        String name;
        TypeT5 tipo;
        EntryType tipoE;

        private SymbolTableEntry(String name, TypeT5 tipo, EntryType tipoE) {
            this.name = name;
            this.tipo = tipo;
            this.tipoE = tipoE;
        }
    }

    public TypeT5 verify(String name) {
        name = reduceName(name, "[");

        return table.get(name).tipo;
    }

    public void add(String name, TypeT5 tipo, EntryType tipoE) {
        name = reduceName(name, "[");

        table.put(name, new SymbolTableEntry(name, tipo, tipoE));
    }

    public boolean exists(String name) {
        name = reduceName(name, "[");

        return table.containsKey(name);
    }
}