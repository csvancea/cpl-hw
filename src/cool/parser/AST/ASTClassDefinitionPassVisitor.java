package cool.parser.AST;

import cool.structures.ClassSymbol;
import cool.structures.SymbolTable;

public class ASTClassDefinitionPassVisitor extends ASTDefaultVisitor<Void> {
    @Override
    public Void visit(ClassDef class_) {
        var classType = class_.type;
        var classTypeName = classType.getToken().getText();
        if (classTypeName.equals("SELF_TYPE")) {
            SymbolTable.error(classType, "Class has illegal name SELF_TYPE");
        }

        var classSymbol = new ClassSymbol(SymbolTable.globals, classTypeName);
        if (!SymbolTable.globals.add(classSymbol)) {
            SymbolTable.error(classType, "Class " + classTypeName + " is redefined");
        }

        return null;
    }

    @Override
    public Void visit(Program program) {
        program.classes.forEach(x -> x.accept(this));
        return null;
    }
}
