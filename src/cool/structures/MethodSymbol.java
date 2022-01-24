package cool.structures;

import java.util.*;

// O metodă este atât simbol, cât și domeniu de vizibilitate pentru parametrii
// săi formali.

public class MethodSymbol extends IdSymbol implements Scope {
 
    // LinkedHashMap reține ordinea adăugării.
    private final Map<String, IdSymbol> symbols = new LinkedHashMap<>();

    // Reține numărul de variabile locale metodei.
    private int localDefs = 0;

    private final Scope parent;
    
    public MethodSymbol(Scope parent, String name) {
        super(decorate(name), DefinitionType.METHOD);
        this.parent = parent;
    }

    @Override
    public boolean add(Symbol sym) {
        // Ne asigurăm că simbolul nu există deja în domeniul de vizibilitate
        // curent.
        if (symbols.containsKey(sym.getName()))
            return false;
        
        symbols.put(sym.getName(), (IdSymbol)sym);
        
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

    @Override
    public String toString() {
        return parent.toString() + "." + undecorate(getName());
    }
    
    public Map<String, IdSymbol> getFormals() {
        return symbols;
    }

    public int registerLocalDef() {
        return localDefs++;
    }

    public int getTotalLocalDefs() {
        return localDefs;
    }

    // În specificația Cool, metodele și atributele unei clase fac parte din scope-uri lexicale diferite,
    // fiind permis astfel să existe metode și atribute cu același nume.
    // Ca workaround pentru a nu fi nevoit sa am scope-uri diferite în cadrul unei clase,
    // am ales să decorez funcțiile adăugând un underscore (caracter invalid în Cool)
    // în fața numelor metodelor.
    public static String decorate(String name) {
        return "_" + name;
    }

    public static String undecorate(String name) {
        return name.substring(1);
    }
}
