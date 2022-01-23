package cool.structures;

public class IdSymbol implements Symbol {
    public enum DefinitionType {
        ATTRIBUTE,
        METHOD,
        LOCAL,
        FORMAL
    }

    private DefinitionType definitionType;

    // Fiecare identificator posedă un tip.
    private ClassSymbol type;

    // Numele simbolului
    private final String name;

    // Sensul depinde de tipul simbolului:
    //   - metoda: indexul în vmtable
    //   - atribut: indexul în cadrul listei de atribute
    //   - formal: indexul în cadrul listei de parametri (maybe?)
    private int index = -1;
    
    public IdSymbol(String name, DefinitionType definitionType) {
        this.name = name;
        this.definitionType = definitionType;
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

    public DefinitionType getDefinitionType() {
        return definitionType;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}