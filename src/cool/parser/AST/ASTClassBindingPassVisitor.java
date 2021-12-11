package cool.parser.AST;

import cool.structures.*;

import java.util.Arrays;

public class ASTClassBindingPassVisitor extends ASTDefaultVisitor<Void> {
    @Override
    public Void visit(CaseTest caseTest) {
        var id = caseTest.id;
        var idSymbol = id.getSymbol();
        if (idSymbol == null)
            return null;

        var type = caseTest.type;
        var typeName = type.getToken().getText();
        var typeSymbol = (ClassSymbol)SymbolTable.globals.lookup(typeName);

        if (typeName.equals("SELF_TYPE")) {
            SymbolTable.error(type, "Case variable " + idSymbol + " has illegal type SELF_TYPE");
            return null;
        }

        if (typeSymbol == null) {
            SymbolTable.error(type, "Case variable " + idSymbol + " has undefined type " + typeName);
            return null;
        }

        idSymbol.setType(typeSymbol);
        type.setSymbol(typeSymbol);
        return null;
    }

    @Override
    public Void visit(Case case_) {
        case_.instance.accept(this);
        case_.caseTests.forEach(x -> x.accept(this));
        return null;
    }

    @Override
    public Void visit(LocalDef localDef) {
        var id = localDef.id;
        var idSymbol = id.getSymbol();
        if (idSymbol == null)
            return null;

        var type = localDef.type;
        var typeName = type.getToken().getText();
        var typeSymbol = (ClassSymbol)SymbolTable.globals.lookup(typeName);

        if (typeSymbol == null) {
            SymbolTable.error(type, "Let variable " + idSymbol + " has undefined type " + typeName);
            return null;
        }

        idSymbol.setType(typeSymbol);
        type.setSymbol(typeSymbol);

        return null;
    }

    @Override
    public Void visit(Let let) {
        let.vars.forEach(x -> x.accept(this));
        let.body.accept(this);

        return null;
    }

    @Override
    public Void visit(Formal formal) {
        var id = formal.id;
        var methodSymbol = (MethodSymbol)id.getScope();
        if (methodSymbol == null)
            return null;

        var classSymbol = (ClassSymbol)methodSymbol.getParent();
        var idSymbol = id.getSymbol();
        if (idSymbol == null)
            return null;

        var type = formal.type;
        var typeName = type.getToken().getText();
        var typeSymbol = (ClassSymbol)SymbolTable.globals.lookup(typeName);

        if (typeName.equals("SELF_TYPE")) {
            SymbolTable.error(type, "Method " + methodSymbol + " of class " + classSymbol + " has formal parameter " + idSymbol + " with illegal type SELF_TYPE");
            return null;
        }

        if (typeSymbol == null) {
            SymbolTable.error(type, "Method " + methodSymbol + " of class " + classSymbol + " has formal parameter " + idSymbol + " with undefined type " + typeName);
            return null;
        }

        idSymbol.setType(typeSymbol);
        type.setSymbol(typeSymbol);
        return null;
    }

    @Override
    public Void visit(MethodDef methodDef) {
        var id = methodDef.id;
        var classSymbol = (ClassSymbol)id.getScope();
        if (classSymbol == null)
            return null;

        var methodSymbol = (MethodSymbol)id.getSymbol();
        if (methodSymbol == null)
            return null;

        var type = methodDef.type;
        var typeName = type.getToken().getText();
        var typeSymbol = (ClassSymbol)SymbolTable.globals.lookup(typeName);

        if (typeSymbol == null) {
            SymbolTable.error(type, "Class " + classSymbol + " has method " + methodSymbol + " with undefined return type " + typeName);
            return null;
        }

        methodSymbol.setType(typeSymbol);
        type.setSymbol(typeSymbol);

        methodDef.formals.forEach(x -> x.accept(this));
        methodDef.body.accept(this);
        return null;
    }

    @Override
    public Void visit(AttributeDef attributeDef) {
        var id = attributeDef.id;
        var classSymbol = (ClassSymbol)id.getScope();
        if (classSymbol == null)
            return null;

        var idSymbol = id.getSymbol();
        if (idSymbol == null)
            return null;

        var type = attributeDef.type;
        var typeName = type.getToken().getText();
        var typeSymbol = (ClassSymbol)SymbolTable.globals.lookup(typeName);

        if (typeSymbol == null) {
            SymbolTable.error(type, "Class " + classSymbol + " has attribute " + idSymbol + " with undefined type " + typeName);
            return null;
        }

        idSymbol.setType(typeSymbol);
        type.setSymbol(typeSymbol);

        if (attributeDef.initValue != null) {
            attributeDef.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(ClassDef class_) {
        var classSymbol = class_.type.getSymbol();
        if (classSymbol == null)
            return null;

        var parentClassType = class_.superType;
        if (parentClassType == null) {
            classSymbol.setParent(ClassSymbol.OBJECT);
        }
        else {
            var parentClassTypeName = parentClassType.getToken().getText();
            if (Arrays.asList("Int", "String", "Bool", "SELF_TYPE").contains(parentClassTypeName)) {
                SymbolTable.error(parentClassType, "Class " + classSymbol + " has illegal parent " + parentClassTypeName);
                return null;
            }

            var parentClassSymbol = (ClassSymbol)SymbolTable.globals.lookup(parentClassTypeName);
            if (parentClassSymbol == null) {
                SymbolTable.error(parentClassType, "Class " + classSymbol + " has undefined parent " + parentClassTypeName);
                return null;
            }

            classSymbol.setParent(parentClassSymbol);
            parentClassType.setSymbol(parentClassSymbol);
        }

        class_.features.forEach(x -> x.accept(this));
        return null;
    }

    @Override
    public Void visit(Program program) {
        program.classes.forEach(x -> x.accept(this));
        return null;
    }
}
