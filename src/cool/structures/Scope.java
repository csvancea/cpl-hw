package cool.structures;

public interface Scope {
    boolean add(Symbol sym);
    
    Symbol lookup(String str);
    
    Scope getParent();
}
