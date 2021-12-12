package cool.parser.AST;

import cool.structures.*;
import cool.util.stream;

import java.util.Objects;

public class ASTClassHierarchyValidationPassVisitor extends ASTDefaultVisitor<Void> {
    @Override
    public Void visit(MethodDef methodDef) {
        var id = methodDef.id;
        var classSymbol = (ClassSymbol)id.getScope();
        if (classSymbol == null)
            return null;

        var methodSymbol = (MethodSymbol)id.getSymbol();
        if (methodSymbol == null)
            return null;

        var overriddenMethodSymbol = (MethodSymbol)classSymbol.getParent().lookup(methodSymbol.getName());
        if (overriddenMethodSymbol == null)
            return null;

        var overriddenMethodFormals = overriddenMethodSymbol.getFormals();
        if (overriddenMethodFormals.size() != methodSymbol.getFormals().size()) {
            SymbolTable.error(id, "Class " + classSymbol + " overrides method " + methodSymbol + " with different number of formal parameters");
            return null;
        }

        var type = methodDef.type;
        var methodTypeSymbol = methodSymbol.getType();
        var overriddenMethodTypeSymbol = overriddenMethodSymbol.getType();
        if (overriddenMethodTypeSymbol != methodTypeSymbol && !overriddenMethodTypeSymbol.isSelfType() && !methodTypeSymbol.isSelfType()) {
            SymbolTable.error(type, "Class " + classSymbol + " overrides method " + methodSymbol + " but changes return type from " + overriddenMethodTypeSymbol + " to " + methodTypeSymbol);
            return null;
        }

        class TypeMismatch {
            final ClassSymbol oldType;
            final ClassSymbol newType;
            final Formal formal;

            TypeMismatch(ClassSymbol oldType, ClassSymbol newType, Formal formal) {
                this.oldType = oldType;
                this.newType = newType;
                this.formal = formal;
            }
        }

        var typeMismatch = stream.zip(
                overriddenMethodFormals.values().stream(),
                methodDef.formals.stream(),
                (overriddenFormalSymbol, newFormal) -> {
                    var oldType = overriddenFormalSymbol.getType();
                    var newType = newFormal.type.getSymbol();

                    if (oldType != newType) {
                        return new TypeMismatch(oldType, newType, newFormal);
                    }
                    return null;
                }
            )
            .filter(Objects::nonNull)
            .findFirst();

        if (typeMismatch.isPresent()) {
            var mismatch = typeMismatch.get();
            var oldType = mismatch.oldType;
            var newType = mismatch.newType;
            var mismatchSymbol = mismatch.formal.id.getSymbol();
            var astNode = mismatch.formal.type;

            SymbolTable.error(astNode, "Class " + classSymbol + " overrides method " + methodSymbol + " but changes type of formal parameter " + mismatchSymbol + " from " + oldType + " to " + newType);
            return null;
        }

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

        if (classSymbol.getParent().lookup(idSymbol.getName()) != null) {
            SymbolTable.error(id, "Class " + classSymbol + " redefines inherited attribute " + idSymbol);
            return null;
        }

        return null;
    }

    @Override
    public Void visit(ClassDef class_) {
        var type = class_.type;
        var classSymbol = type.getSymbol();
        if (classSymbol == null)
            return null;

        Scope temp = classSymbol.getParent();
        while (temp != null) {
            if (temp == classSymbol) {
                SymbolTable.error(type, "Inheritance cycle for class " + classSymbol);
                break;
            }
            temp = temp.getParent();
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
