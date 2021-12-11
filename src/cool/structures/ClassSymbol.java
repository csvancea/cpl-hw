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

    // Adâncimea la care se află clasa față de clasa Object
    private int depth = -1;

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

    public boolean isSubclassOf(ClassSymbol other) {
        Scope temp = this;

        do {
            if (temp == other) {
                return true;
            }
            temp = temp.getParent();
        } while (temp != null);

        return false;
    }

    private int getDepth()
    {
        // Cache result
        if (depth != -1)
            return depth;

        var parentClass = (ClassSymbol)getParent();
        if (parentClass == null) {
            depth = 0;
        }
        else {
            depth = parentClass.getDepth() + 1;
        }

        return depth;
    }

    public ClassSymbol getLeastUpperBound(ClassSymbol other)
    {
        // Find depths of two nodes and differences
        ClassSymbol c1 = this, c2 = other;
        int d1 = c1.getDepth(), d2 = c2.getDepth();
        int diff = d1 - d2;

        // If n2 is deeper, swap n1 and n2
        if (diff < 0)
        {
            ClassSymbol temp = c1;
            c1 = c2;
            c2 = temp;
            diff = -diff;
        }

        // Move n1 up until it reaches the same level as n2
        while (diff-- != 0)
            c1 = (ClassSymbol)c1.getParent();

        // Now n1 and n2 are at same levels
        while (c1 != null && c2 != null)
        {
            if (c1 == c2)
                return c1;

            c1 = (ClassSymbol)c1.getParent();
            c2 = (ClassSymbol)c2.getParent();
        }

        // Shouldn't get here because all classes inherit from Object
        return null;
    }
}
