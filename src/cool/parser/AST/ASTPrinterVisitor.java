package cool.parser.AST;

public class ASTPrinterVisitor implements ASTVisitor<Void> {
    private static final int SPACES_PER_INDENT_LEVEL = 2;
    private int indentLevel = 0;

    private void print(java.lang.String line) {
        System.out.println(
                " ".repeat(indentLevel * SPACES_PER_INDENT_LEVEL) + line
        );
    }

    @Override
    public Void visit(Id id) {
        print(id.getToken().getText());
        return null;
    }

    @Override
    public Void visit(Int int_) {
        print(int_.getToken().getText());
        return null;
    }

    @Override
    public Void visit(String string) {
        print(string.getToken().getText());
        return null;
    }

    @Override
    public Void visit(Bool bool_) {
        print(bool_.getToken().getText());
        return null;
    }

    @Override
    public Void visit(If if_) {
        print("if");

        indentLevel++;

        if_.cond.accept(this);
        if_.thenBranch.accept(this);
        if_.elseBranch.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(While while_) {
        print("while");

        indentLevel++;

        while_.cond.accept(this);
        while_.body.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Block block) {
        print("block");

        indentLevel++;

        block.exprs.forEach(x -> x.accept(this));

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(LocalDef localDef) {
        print("local");

        indentLevel++;

        localDef.id.accept(this);
        localDef.type.accept(this);
        if (localDef.initValue != null) {
            localDef.initValue.accept(this);
        }

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Let let) {
        print("let");

        indentLevel++;

        let.vars.forEach(x -> x.accept(this));
        let.body.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(CaseTest caseTest) {
        print("case branch");

        indentLevel++;

        caseTest.id.accept(this);
        caseTest.type.accept(this);
        caseTest.body.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Case case_) {
        print("case");

        indentLevel++;

        case_.instance.accept(this);
        case_.caseTests.forEach(x -> x.accept(this));

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(New new_) {
        print("new");

        indentLevel++;

        new_.type.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(IsVoid isVoid) {
        print("isvoid");

        indentLevel++;

        isVoid.instance.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Assign assign) {
        print("<-");

        indentLevel++;

        assign.id.accept(this);
        assign.expr.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Relational rel) {
        print(rel.getToken().getText());

        indentLevel++;

        rel.left.accept(this);
        rel.right.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Not not) {
        print("not");

        indentLevel++;

        not.expr.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Plus plus) {
        print("+");

        indentLevel++;

        plus.left.accept(this);
        plus.right.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Minus minus) {
        print("-");

        indentLevel++;

        minus.left.accept(this);
        minus.right.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Mult mult) {
        print("*");

        indentLevel++;

        mult.left.accept(this);
        mult.right.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Div div) {
        print("/");

        indentLevel++;

        div.left.accept(this);
        div.right.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Negate negate) {
        print("~");

        indentLevel++;

        negate.expr.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Dispatch dispatch) {
        if (dispatch.instance == null) {
            print("implicit dispatch");
        } else {
            print(".");
        }

        indentLevel++;

        if (dispatch.instance != null) {
            dispatch.instance.accept(this);
        }
        if (dispatch.type != null) {
            dispatch.type.accept(this);
        }
        dispatch.id.accept(this);
        dispatch.args.forEach(x -> x.accept(this));

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Type type) {
        print(type.getToken().getText());
        return null;
    }

    @Override
    public Void visit(Formal formal) {
        print("formal");

        indentLevel++;

        formal.id.accept(this);
        formal.type.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(AttributeDef attributeDef) {
        print("attribute");

        indentLevel++;

        attributeDef.id.accept(this);
        attributeDef.type.accept(this);
        if (attributeDef.initValue != null) {
            attributeDef.initValue.accept(this);
        }

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(MethodDef methodDef) {
        print("method");

        indentLevel++;

        methodDef.id.accept(this);
        methodDef.formals.forEach(x -> x.accept(this));
        methodDef.type.accept(this);
        methodDef.body.accept(this);

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(ClassDef class_) {
        print("class");

        indentLevel++;

        class_.type.accept(this);
        if (class_.superType != null) {
            class_.superType.accept(this);
        }
        class_.features.forEach(x -> x.accept(this));

        indentLevel--;
        return null;
    }

    @Override
    public Void visit(Program program) {
        print("program");

        indentLevel++;

        program.classes.forEach(x -> x.accept(this));

        indentLevel--;
        return null;
    }
}
