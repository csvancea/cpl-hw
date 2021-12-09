package cool.structures;

import java.util.*;

// O clasă este atât simbol, cât și domeniu de vizibilitate pentru atributele
// și metodele sale.

public class ClassSymbol extends Symbol implements Scope {

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

    public boolean setParent(Scope parent) {
        // Ne asigurăm că părintele pe care vrem să îl setăm pentru clasa curentă
        // nu are ca strămoș clasa curentă (inheritance cycle)
        Scope temp = parent;
        while (temp != null) {
            if (temp == this) {
                return false;
            }
            temp = temp.getParent();
        }

        this.parent = parent;
        return true;
    }
}
