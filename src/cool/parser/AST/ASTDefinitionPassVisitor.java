package cool.parser.AST;

import cool.structures.*;

public class ASTDefinitionPassVisitor extends ASTDefaultVisitor<Void> {
    private Scope currentScope = null;

    @Override
    public Void visit(Formal formal) {
        var methodSymbol = (MethodSymbol)currentScope;
        var classSymbol = (ClassSymbol)currentScope.getParent();

        var id = formal.id;
        var idName = id.getToken().getText();
        var idSymbol = new IdSymbol(idName);

        if (idName.equals("self")) {
            SymbolTable.error(id, "Method " + methodSymbol + " of class " + classSymbol + " has formal parameter with illegal name self");
            return null;
        }

        if (!currentScope.add(idSymbol)) {
            SymbolTable.error(id, "Method " + methodSymbol + " of class " + classSymbol + " redefines formal parameter " + idName);
            return null;
        }

        id.setSymbol(idSymbol);
        id.setScope(currentScope);

        return null;
    }

    @Override
    public Void visit(MethodDef methodDef) {
        var classSymbol = (ClassSymbol)currentScope;

        var id = methodDef.id;
        var idName = id.getToken().getText();
        var idSymbol = new MethodSymbol(currentScope, idName);

        if (!currentScope.add(idSymbol)) {
            SymbolTable.error(id, "Class " + classSymbol + " redefines method " + idName);
            return null;
        }

        id.setSymbol(idSymbol);
        id.setScope(currentScope);

        currentScope = idSymbol;
        methodDef.formals.forEach(x -> x.accept(this));
        methodDef.body.accept(this);
        currentScope = currentScope.getParent();

        return null;
    }

    @Override
    public Void visit(AttributeDef attributeDef) {
        var classSymbol = (ClassSymbol)currentScope;

        var id = attributeDef.id;
        var idName = id.getToken().getText();
        var idSymbol = new IdSymbol(idName);

        if (idName.equals("self")) {
            SymbolTable.error(id, "Class " + classSymbol + " has attribute with illegal name self");
            return null;
        }

        if (!currentScope.add(idSymbol)) {
            SymbolTable.error(id, "Class " + classSymbol + " redefines attribute " + idName);
            return null;
        }

        id.setSymbol(idSymbol);
        id.setScope(currentScope);

        if (attributeDef.initValue != null) {
            attributeDef.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(ClassDef class_) {
        var classType = class_.type;
        var classTypeName = classType.getToken().getText();
        if (classTypeName.equals("SELF_TYPE")) {
            SymbolTable.error(classType, "Class has illegal name SELF_TYPE");
            return null;
        }

        var classSymbol = new ClassSymbol(SymbolTable.globals, classTypeName);
        if (!SymbolTable.globals.add(classSymbol)) {
            SymbolTable.error(classType, "Class " + classTypeName + " is redefined");
            return null;
        }

        classType.setSymbol(classSymbol);

        currentScope = classSymbol;
        class_.features.forEach(x -> x.accept(this));
        currentScope = null;

        return null;
    }

    @Override
    public Void visit(Program program) {
        program.classes.forEach(x -> x.accept(this));
        return null;
    }
}
