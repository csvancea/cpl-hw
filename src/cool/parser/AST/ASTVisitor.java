package cool.parser.AST;

public interface ASTVisitor<T> {
    T visit(Id id);
    T visit(Int int_);
    T visit(String string);
    T visit(Bool bool_);
    T visit(If if_);
    T visit(While while_);
    T visit(Block block);
    T visit(LocalDef localDef);
    T visit(Let let);
    T visit(CaseTest caseTest);
    T visit(Case case_);
    T visit(New new_);
    T visit(IsVoid isVoid);
    T visit(Assign assign);
    T visit(Relational rel);
    T visit(Not not);
    T visit(Plus plus);
    T visit(Minus minus);
    T visit(Mult mult);
    T visit(Div div);
    T visit(Negate negate);
    T visit(Dispatch dispatch);
    T visit(Type type);
    T visit(Formal formal);
    T visit(AttributeDef attributeDef);
    T visit(MethodDef methodDef);
    T visit(ClassDef class_);
    T visit(Program program);
}
