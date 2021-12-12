package cool.structures;

public interface ClassSymbol extends Symbol, Scope {
    void setParent(ClassSymbol parent);
    boolean isPrimitive();
    boolean isSubclassOf(ClassSymbol other);
    int getDepth();
    ClassSymbol getLeastUpperBound(ClassSymbol other);

    boolean isSelfType();

    ClassSymbol getActualType();
    ClassSymbol getSelfType();
}
