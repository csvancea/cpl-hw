package cool.structures;

import java.util.List;

public class SelfClassSymbol implements ClassSymbol {
    private final ActualClassSymbol actualClass;

    public SelfClassSymbol(ActualClassSymbol currentClass) {
        this.actualClass = currentClass;
    }

    @Override
    public String getName() {
        return "SELF_TYPE";
    }

    @Override
    public String toString() {
        return "SELF_TYPE";
    }

    @Override
    public boolean add(Symbol sym) {
        return actualClass.add(sym);
    }

    @Override
    public Symbol lookup(String s) {
        return actualClass.lookup(s);
    }

    @Override
    public Scope getParent() {
        return actualClass.getParent();
    }

    @Override
    public void setParent(ClassSymbol parent) {
        actualClass.setParent(parent);
    }

    @Override
    public List<ClassSymbol> getChildren() {
        return actualClass.getChildren();
    }

    @Override
    public boolean isPrimitive() {
        return actualClass.isPrimitive();
    }

    @Override
    public boolean isSubclassOf(ClassSymbol other) {
        if (other.isSelfType()) {
            return this == other;
        }

        return actualClass.isSubclassOf(other);
    }

    @Override
    public int getDepth() {
        return actualClass.getDepth();
    }

    @Override
    public ClassSymbol getLeastUpperBound(ClassSymbol other) {
        return actualClass.getLeastUpperBound(other);
    }

    @Override
    public boolean isSelfType() {
        return true;
    }

    @Override
    public ClassSymbol getActualType() {
        return actualClass;
    }

    @Override
    public ClassSymbol getSelfType() {
        return this;
    }

    @Override
    public void setTag(int tag) {
        actualClass.setTag(tag);
    }

    @Override
    public int getTag() {
        return actualClass.getTag();
    }

    @Override
    public void setMaxSubTreeTag(int tag) {
        actualClass.setMaxSubTreeTag(tag);
    }

    @Override
    public int getMaxSubTreeTag() {
        return actualClass.getMaxSubTreeTag();
    }
}
