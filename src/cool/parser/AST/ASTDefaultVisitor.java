package cool.parser.AST;

public class ASTDefaultVisitor<T> implements ASTVisitor<T> {
    @Override
    public T visit(Id id) {
        return null;
    }

    @Override
    public T visit(Int int_) {
        return null;
    }

    @Override
    public T visit(String string) {
        return null;
    }

    @Override
    public T visit(Bool bool_) {
        return null;
    }

    @Override
    public T visit(If if_) {
        return null;
    }

    @Override
    public T visit(While while_) {
        return null;
    }

    @Override
    public T visit(Block block) {
        return null;
    }

    @Override
    public T visit(LocalDef localDef) {
        return null;
    }

    @Override
    public T visit(Let let) {
        return null;
    }

    @Override
    public T visit(CaseTest caseTest) {
        return null;
    }

    @Override
    public T visit(Case case_) {
        return null;
    }

    @Override
    public T visit(New new_) {
        return null;
    }

    @Override
    public T visit(IsVoid isVoid) {
        return null;
    }

    @Override
    public T visit(Assign assign) {
        return null;
    }

    @Override
    public T visit(Relational rel) {
        return null;
    }

    @Override
    public T visit(Not not) {
        return null;
    }

    @Override
    public T visit(Plus plus) {
        return null;
    }

    @Override
    public T visit(Minus minus) {
        return null;
    }

    @Override
    public T visit(Mult mult) {
        return null;
    }

    @Override
    public T visit(Div div) {
        return null;
    }

    @Override
    public T visit(Negate negate) {
        return null;
    }

    @Override
    public T visit(Dispatch dispatch) {
        return null;
    }

    @Override
    public T visit(Type type) {
        return null;
    }

    @Override
    public T visit(Formal formal) {
        return null;
    }

    @Override
    public T visit(AttributeDef attributeDef) {
        return null;
    }

    @Override
    public T visit(MethodDef methodDef) {
        return null;
    }

    @Override
    public T visit(ClassDef class_) {
        return null;
    }

    @Override
    public T visit(Program program) {
        return null;
    }
}
