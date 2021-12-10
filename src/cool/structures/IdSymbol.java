package cool.structures;

public class IdSymbol extends Symbol {
    // Fiecare identificator posedă un tip.
    private ClassSymbol type;
    
    public IdSymbol(String name) {
        super(name);
    }
    
    public IdSymbol setType(ClassSymbol type) {
        this.type = type;
        return this;
    }
    
    public ClassSymbol getType() {
        return type;
    }
}