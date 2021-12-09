package cool.structures;

public class IdSymbol extends Symbol {
    // Fiecare identificator posedă un tip.
    private ClassSymbol type;
    
    public IdSymbol(String name) {
        super(name);
    }
    
    public void setType(ClassSymbol type) {
        this.type = type;
    }
    
    public ClassSymbol getType() {
        return type;
    }
}