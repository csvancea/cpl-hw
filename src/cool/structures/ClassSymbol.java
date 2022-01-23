package cool.structures;

import java.util.List;

public interface ClassSymbol extends Symbol, Scope {
    void setParent(ClassSymbol parent);
    List<ClassSymbol> getChildren();

    boolean isPrimitive();
    boolean isSubclassOf(ClassSymbol other);
    int getDepth();
    ClassSymbol getLeastUpperBound(ClassSymbol other);

    boolean isSelfType();

    ClassSymbol getActualType();
    ClassSymbol getSelfType();

    void setTag(int tag);
    int getTag();

    void setMaxSubTreeTag(int tag);
    int getMaxSubTreeTag();
}
