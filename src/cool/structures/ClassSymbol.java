package cool.structures;

import java.util.*;

// O clasă este atât simbol, cât și domeniu de vizibilitate pentru atributele
// și metodele sale.

public class ClassSymbol extends Symbol implements Scope {
    public static final ClassSymbol OBJECT = new ClassSymbol(null, "Object");
    public static final ClassSymbol IO = new ClassSymbol(OBJECT, "IO");
    public static final ClassSymbol INT = new ClassSymbol(OBJECT, "Int");
    public static final ClassSymbol STRING = new ClassSymbol(OBJECT, "String");
    public static final ClassSymbol BOOL = new ClassSymbol(OBJECT, "Bool");
    public static final ClassSymbol SELF_TYPE = new ClassSymbol(OBJECT, "SELF_TYPE");

    // LinkedHashMap reține ordinea adăugării.
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();

    private Scope parent;

    public ClassSymbol(Scope parent, String name) {
        super(name);
        this.parent = parent;
    }

    @Override
    public boolean add(Symbol sym) {
        // Ne asigurăm că simbolul nu există deja în domeniul de vizibilitate
        // curent.
        if (symbols.containsKey(sym.getName()))
            return false;

        symbols.put(sym.getName(), sym);

        return true;
    }

    @Override
    public Symbol lookup(String s) {
        var sym = symbols.get(s);

        if (sym != null)
            return sym;

        // Dacă nu găsim simbolul în domeniul de vizibilitate curent, îl căutăm
        // în domeniul de deasupra.
        if (parent != null)
            return parent.lookup(s);

        return null;
    }

    @Override
    public Scope getParent() {
        return parent;
    }

    public void setParent(Scope parent) {
        this.parent = parent;
    }

    public boolean isPrimitive()
    {
        return List.of(INT, STRING, BOOL).contains(this);
    }
}
