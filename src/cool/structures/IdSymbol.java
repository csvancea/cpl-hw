package cool.structures;

public class IdSymbol implements Symbol {
    // Fiecare identificator posedă un tip.
    private ClassSymbol type;

    // Numele simbolului
    private final String name;
    
    public IdSymbol(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public IdSymbol setType(ClassSymbol type) {
        this.type = type;
        return this;
    }
    
    public ClassSymbol getType() {
        return type;
    }
}