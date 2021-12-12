package cool.structures;

import java.util.*;

// O clasă este atât simbol, cât și domeniu de vizibilitate pentru atributele
// și metodele sale.

public class ActualClassSymbol implements ClassSymbol {
    public static final ActualClassSymbol OBJECT = new ActualClassSymbol(null, "Object");
    public static final ActualClassSymbol IO = new ActualClassSymbol(OBJECT, "IO");
    public static final ActualClassSymbol INT = new ActualClassSymbol(OBJECT, "Int");
    public static final ActualClassSymbol STRING = new ActualClassSymbol(OBJECT, "String");
    public static final ActualClassSymbol BOOL = new ActualClassSymbol(OBJECT, "Bool");
    public final SelfClassSymbol SELF_TYPE = new SelfClassSymbol(this);

    // Numele simbolului
    private final String name;

    // LinkedHashMap reține ordinea adăugării.
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();

    // Clasa părinte
    private ClassSymbol parent;

    // Adâncimea la care se află clasa curentă față de rădăcină (clasa Object)
    private int depth;

    public ActualClassSymbol(Scope parent, String name) {
        this.name = name;
        this.setParent((ClassSymbol)parent);

        var selfSymbol = new IdSymbol("self");
        selfSymbol.setType(SELF_TYPE);
        this.add(selfSymbol);
        this.add(SELF_TYPE);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
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

        // Dacă simbolul nu se află pe lanțul de clase de mai sus, se caută în scope-ul top-level.
        return SymbolTable.globals.lookup(s);
    }

    @Override
    public Scope getParent() {
        return parent;
    }

    @Override
    public void setParent(ClassSymbol parent) {
        this.parent = parent;

        if (parent != null) {
            this.depth = parent.getDepth() + 1;
        }
    }

    @Override
    public boolean isPrimitive() {
        return List.of(INT, STRING, BOOL).contains(this);
    }

    @Override
    public boolean isSubclassOf(ClassSymbol other) {
        if (other.isSelfType()) {
            // Nu are sens T <= SELF_TYPE(c)
            return false;
        }

        Scope temp = this;

        do {
            if (temp == other) {
                return true;
            }
            temp = temp.getParent();
        } while (temp != null);

        return false;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public ClassSymbol getLeastUpperBound(ClassSymbol other) {
        // Find depths of two nodes and differences
        Scope c1 = this, c2 = other;
        int d1 = getDepth(), d2 = other.getDepth();
        int diff = d1 - d2;

        // If n2 is deeper, swap n1 and n2
        if (diff < 0)
        {
            Scope temp = c1;
            c1 = c2;
            c2 = temp;
            diff = -diff;
        }

        // Move n1 up until it reaches the same level as n2
        while (diff-- != 0)
            c1 = c1.getParent();

        // Now n1 and n2 are at same levels
        while (c1 != null && c2 != null)
        {
            if (c1 == c2)
                return (ClassSymbol)c1;

            c1 = c1.getParent();
            c2 = c2.getParent();
        }

        // Shouldn't get here because all classes inherit from Object
        return null;
    }

    @Override
    public boolean isSelfType() {
        return false;
    }

    @Override
    public ClassSymbol getActualType() {
        return this;
    }

    @Override
    public ClassSymbol getSelfType() {
        return SELF_TYPE;
    }
}
