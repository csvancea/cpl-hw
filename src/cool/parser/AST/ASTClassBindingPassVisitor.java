package cool.parser.AST;

import cool.structures.*;

import java.util.Arrays;

public class ASTClassBindingPassVisitor extends ASTDefaultVisitor<Void> {
    @Override
    public Void visit(New new_) {
        var scope = new_.getScope();
        if (scope == null)
            return null;

        var type = new_.type;
        var typeName = type.getToken().getText();
        var typeSymbol = (ClassSymbol)scope.lookup(typeName);

        if (typeSymbol == null) {
            SymbolTable.error(type, "new is used with undefined type " + typeName);
            return null;
        }

        new_.type.setSymbol(typeSymbol);
        return null;
    }

    @Override
    public Void visit(Id id) {
        var scope = id.getScope();
        if (scope == null)
            return null;

        var idName = id.getToken().getText();
        var idSymbol = (IdSymbol)scope.lookup(idName);

        if (idSymbol == null) {
            SymbolTable.error(id, "Undefined identifier " + idName);
            return null;
        }

        id.setSymbol(idSymbol);
        return null;
    }

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

        caseTest.body.accept(this);
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

        var scope = id.getScope();
        if (scope == null)
            return null;

        var type = localDef.type;
        var typeName = type.getToken().getText();
        var typeSymbol = (ClassSymbol)scope.lookup(typeName);

        if (typeSymbol == null) {
            SymbolTable.error(type, "Let variable " + idSymbol + " has undefined type " + typeName);
            return null;
        }

        idSymbol.setType(typeSymbol);
        type.setSymbol(typeSymbol);

        if (localDef.initValue != null) {
            localDef.initValue.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(Let let) {
        let.vars.forEach(x -> x.accept(this));
        let.body.accept(this);

        return null;
    }

    @Override
    public Void visit(Dispatch dispatch) {
        // În această etapă nu se poate concluziona nimic despre apelurile dinamice
        // deoarece nu avem informații despre tipurile expresiilor.

        var type = dispatch.type;
        if (type != null) {
            // Static dispatch!

            var typeName = type.getToken().getText();
            var typeSymbol = (ClassSymbol) SymbolTable.globals.lookup(typeName);

            if (typeName.equals("SELF_TYPE")) {
                SymbolTable.error(type, "Type of static dispatch cannot be SELF_TYPE");
                return null;
            }

            if (typeSymbol == null) {
                SymbolTable.error(type, "Type " + typeName + " of static dispatch is undefined");
                return null;
            }

            dispatch.type.setSymbol(typeSymbol);
        }

        if (dispatch.instance != null)
            dispatch.instance.accept(this);

        dispatch.args.forEach(x -> x.accept(this));
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
        var typeSymbol = (ClassSymbol)classSymbol.lookup(typeName);

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
        var typeSymbol = (ClassSymbol)classSymbol.lookup(typeName);

        if (typeSymbol == null) {
            SymbolTable.error(type, "Class " + classSymbol + " has attribute " + idSymbol + " with undefined type " + typeName);
            return null;
        }

        idSymbol.setType(typeSymbol);
        type.setSymbol(typeSymbol);

        if (attributeDef.initValue != null) {
            attributeDef.initValue.accept(this);
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
            classSymbol.setParent(ActualClassSymbol.OBJECT);
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
