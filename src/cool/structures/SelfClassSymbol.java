package cool.structures;

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
}
