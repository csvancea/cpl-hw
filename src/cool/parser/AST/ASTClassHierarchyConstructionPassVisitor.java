package cool.parser.AST;

import cool.structures.ClassSymbol;
import cool.structures.SymbolTable;

import java.util.Arrays;

public class ASTClassHierarchyConstructionPassVisitor extends ASTDefaultVisitor<Void> {
    @Override
    public Void visit(ClassDef class_) {
        var classType = class_.type;
        var classTypeName = classType.getToken().getText();
        var classSymbol = (ClassSymbol) SymbolTable.globals.lookup(classTypeName);

        var parentClassType = class_.superType;
        java.lang.String parentClassTypeName;
        if (parentClassType == null) {
            parentClassType = class_.type;
            parentClassTypeName = "Object";
        }
        else {
            parentClassTypeName = parentClassType.getToken().getText();
            if (Arrays.asList("Int", "String", "Bool", "SELF_TYPE").contains(parentClassTypeName)) {
                SymbolTable.error(parentClassType, "Class " + classTypeName + " has illegal parent " + parentClassTypeName);
                return null;
            }
        }

        var parentClassSymbol = (ClassSymbol)SymbolTable.globals.lookup(parentClassTypeName);
        if (parentClassSymbol == null) {
            SymbolTable.error(parentClassType, "Class " + classTypeName + " has undefined parent " + parentClassTypeName);
            return null;
        }

        classSymbol.setParent(parentClassSymbol);
        return null;
    }

    @Override
    public Void visit(Program program) {
        program.classes.forEach(x -> x.accept(this));
        return null;
    }
}
