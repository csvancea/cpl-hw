package cool.parser.AST;

import cool.structures.ClassSymbol;
import cool.structures.Scope;
import cool.structures.SymbolTable;

public class ASTClassHierarchyValidationPassVisitor extends ASTDefaultVisitor<Void> {
    @Override
    public Void visit(ClassDef class_) {
        var classType = class_.type;
        var classTypeName = classType.getToken().getText();
        var classSymbol = (ClassSymbol) SymbolTable.globals.lookup(classTypeName);

        Scope temp = classSymbol.getParent();
        while (temp != null) {
            if (temp == classSymbol) {
                SymbolTable.error(classType, "Inheritance cycle for class " + classTypeName);
                break;
            }
            temp = temp.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Program program) {
        program.classes.forEach(x -> x.accept(this));
        return null;
    }
}
