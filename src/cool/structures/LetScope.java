package cool.structures;

public class LetScope extends DefaultScope {
    public LetScope(Scope parent) {
        super(parent);
    }

    @Override
    public boolean add(Symbol sym) {
        // Replace old symbol with the new one in case of collision

        symbols.put(sym.getName(), sym);
        return true;
    }
}
