package cool.parser.AST;

import cool.util.stream;
import cool.structures.*;

import java.util.Objects;

public class ASTDefinitionPassVisitor extends ASTDefaultVisitor<Void> {
    private Scope currentScope = null;

    @Override
    public Void visit(Formal formal) {
        var methodSymbol = (MethodSymbol)currentScope;
        var classSymbol = (ClassSymbol)currentScope.getParent();

        var type = formal.type;
        var typeName = type.getToken().getText();
        var typeSymbol = (ClassSymbol)SymbolTable.globals.lookup(typeName);

        var id = formal.id;
        var idName = id.getToken().getText();
        var idSymbol = new IdSymbol(idName);

        if (typeName.equals("SELF_TYPE")) {
            SymbolTable.error(type, "Method " + methodSymbol + " of class " + classSymbol + " has formal parameter " + idName + " with illegal type SELF_TYPE");
            return null;
        }

        if (typeSymbol == null) {
            SymbolTable.error(type, "Method " + methodSymbol + " of class " + classSymbol + " has formal parameter " + idName + " with undefined type " + typeName);
            return null;
        }

        if (idName.equals("self")) {
            SymbolTable.error(id, "Method " + methodSymbol + " of class " + classSymbol + " has formal parameter with illegal name self");
            return null;
        }

        if (!currentScope.add(idSymbol)) {
            SymbolTable.error(id, "Method " + methodSymbol + " of class " + classSymbol + " redefines formal parameter " + idName);
            return null;
        }

        return null;
    }

    @Override
    public Void visit(AttributeDef attributeDef) {
        var classSymbol = (ClassSymbol)currentScope;

        var type = attributeDef.type;
        var typeName = type.getToken().getText();
        var typeSymbol = (ClassSymbol)SymbolTable.globals.lookup(typeName);

        var id = attributeDef.id;
        var idName = id.getToken().getText();
        var idSymbol = new IdSymbol(idName);

        if (typeSymbol == null) {
            SymbolTable.error(type, "Class " + classSymbol + " has attribute " + idName + " with undefined type " + typeName);
            return null;
        }

        if (idName.equals("self")) {
            SymbolTable.error(id, "Class " + classSymbol + " has attribute with illegal name self");
            return null;
        }

        if (currentScope.getParent().lookup(idName) != null) {
            SymbolTable.error(id, "Class " + classSymbol + " redefines inherited attribute " + idName);
            return null;
        }

        if (!currentScope.add(idSymbol)) {
            SymbolTable.error(id, "Class " + classSymbol + " redefines attribute " + idName);
            return null;
        }

        if (attributeDef.initValue != null) {
            attributeDef.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(MethodDef methodDef) {
        var classSymbol = (ClassSymbol)currentScope;

        var type = methodDef.type;
        var typeName = type.getToken().getText();
        var typeSymbol = (ClassSymbol)SymbolTable.globals.lookup(typeName);

        var id = methodDef.id;
        var idName = id.getToken().getText();
        var idDecoratedName = MethodSymbol.decorate(idName);
        var idSymbol = new MethodSymbol(currentScope, idName);

        if (typeSymbol == null) {
            SymbolTable.error(type, "Class " + classSymbol + " has method " + idName + " with undefined return type " + typeName);
            return null;
        }

        var overriddenMethodSymbol = (MethodSymbol)currentScope.getParent().lookup(idDecoratedName);
        if (overriddenMethodSymbol != null) {
            var overriddenMethodFormals = overriddenMethodSymbol.getFormals();
            if (overriddenMethodFormals.size() != methodDef.formals.size()) {
                SymbolTable.error(id, "Class " + classSymbol + " overrides method " + idName + " with different number of formal parameters");
                return null;
            }

            var overriddenMethodTypeSymbol = overriddenMethodSymbol.getType();
            if (overriddenMethodTypeSymbol != typeSymbol) {
                SymbolTable.error(type, "Class " + classSymbol + " overrides method " + idName + " but changes return type from " + overriddenMethodTypeSymbol + " to " + typeSymbol);
                return null;
            }

            var typeMismatch = stream.zip(
                    overriddenMethodFormals.values().stream(),
                    methodDef.formals.stream(),
                    (overriddenFormalSymbol, newFormal) -> {
                        var oldType = overriddenFormalSymbol.getType().toString();
                        var newType = newFormal.type.getToken().toString();
                        var newName = newFormal.id.getToken().toString();

                        if (!oldType.equals(newType)) {
                            return "Class " + classSymbol + " overrides method " + idName + " but changes return type of formal parameter " + newName + " from " + oldType + " to " + newType;
                        }
                        return null;
                    }
                    )
                    .filter(Objects::nonNull)
                    .findFirst();

            if (typeMismatch.isPresent()) {
                SymbolTable.error(methodDef, typeMismatch.get());
                return null;
            }
        }

        if (!currentScope.add(idSymbol)) {
            SymbolTable.error(id, "Class " + classSymbol + " redefines method " + idName);
            return null;
        }

        currentScope = idSymbol;
        methodDef.formals.forEach(x -> x.accept(this));
        methodDef.body.accept(this);
        currentScope = currentScope.getParent();

        return null;
    }

    @Override
    public Void visit(ClassDef class_) {
        var classType = class_.type;
        var classTypeName = classType.getToken().getText();
        var classSymbol = (ClassSymbol)SymbolTable.globals.lookup(classTypeName);

        // All classes must have been defined previously in Class Definition Pass
        assert(classSymbol != null);

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
