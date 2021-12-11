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
        if_.cond.accept(this);
        if_.thenBranch.accept(this);
        if_.elseBranch.accept(this);
        return null;
    }

    @Override
    public T visit(While while_) {
        while_.cond.accept(this);
        while_.body.accept(this);
        return null;
    }

    @Override
    public T visit(Block block) {
        block.exprs.forEach(x -> x.accept(this));
        return null;
    }

    @Override
    public T visit(LocalDef localDef) {
        localDef.id.accept(this);
        localDef.type.accept(this);
        if (localDef.initValue != null)
            localDef.initValue.accept(this);
        return null;
    }

    @Override
    public T visit(Let let) {
        let.vars.forEach(x -> x.accept(this));
        let.body.accept(this);
        return null;
    }

    @Override
    public T visit(CaseTest caseTest) {
        caseTest.id.accept(this);
        caseTest.type.accept(this);
        caseTest.body.accept(this);
        return null;
    }

    @Override
    public T visit(Case case_) {
        case_.instance.accept(this);
        case_.caseTests.forEach(x -> x.accept(this));
        return null;
    }

    @Override
    public T visit(New new_) {
        new_.type.accept(this);
        return null;
    }

    @Override
    public T visit(IsVoid isVoid) {
        isVoid.instance.accept(this);
        return null;
    }

    @Override
    public T visit(Assign assign) {
        assign.id.accept(this);
        assign.expr.accept(this);
        return null;
    }

    @Override
    public T visit(Relational rel) {
        rel.left.accept(this);
        rel.right.accept(this);
        return null;
    }

    @Override
    public T visit(Not not) {
        not.expr.accept(this);
        return null;
    }

    @Override
    public T visit(Plus plus) {
        plus.left.accept(this);
        plus.right.accept(this);
        return null;
    }

    @Override
    public T visit(Minus minus) {
        minus.left.accept(this);
        minus.right.accept(this);
        return null;
    }

    @Override
    public T visit(Mult mult) {
        mult.left.accept(this);
        mult.right.accept(this);
        return null;
    }

    @Override
    public T visit(Div div) {
        div.left.accept(this);
        div.right.accept(this);
        return null;
    }

    @Override
    public T visit(Negate negate) {
        negate.expr.accept(this);
        return null;
    }

    @Override
    public T visit(Dispatch dispatch) {
        if (dispatch.instance != null)
            dispatch.instance.accept(this);

        if (dispatch.type != null)
            dispatch.type.accept(this);

        dispatch.id.accept(this);
        dispatch.args.forEach(x -> x.accept(this));
        return null;
    }

    @Override
    public T visit(Type type) {
        return null;
    }

    @Override
    public T visit(Formal formal) {
        formal.id.accept(this);
        formal.type.accept(this);
        return null;
    }

    @Override
    public T visit(AttributeDef attributeDef) {
        attributeDef.id.accept(this);
        attributeDef.type.accept(this);
        if (attributeDef.initValue != null)
            attributeDef.initValue.accept(this);
        return null;
    }

    @Override
    public T visit(MethodDef methodDef) {
        methodDef.id.accept(this);
        methodDef.formals.forEach(x -> x.accept(this));
        methodDef.type.accept(this);
        methodDef.body.accept(this);
        return null;
    }

    @Override
    public T visit(ClassDef class_) {
        class_.type.accept(this);
        if (class_.superType != null)
            class_.superType.accept(this);
        class_.features.forEach(x -> x.accept(this));
        return null;
    }

    @Override
    public T visit(Program program) {
        program.classes.forEach(x -> x.accept(this));
        return null;
    }
}
