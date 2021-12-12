package cool.parser.AST;

import cool.parser.CoolParser;
import cool.structures.*;
import cool.util.stream;
import org.antlr.v4.runtime.misc.Pair;

import java.util.Objects;

public class ASTResolutionPassVisitor extends ASTDefaultVisitor<ClassSymbol> {
    @Override
    public ClassSymbol visit(Id id) {
        var idSymbol = id.getSymbol();
        if (idSymbol == null)
            return null;

        return idSymbol.getType();
    }

    @Override
    public ClassSymbol visit(Type type) {
        return type.getSymbol();
    }

    @Override
    public ClassSymbol visit(Int int_) {
        return ActualClassSymbol.INT;
    }

    @Override
    public ClassSymbol visit(String string) {
        return ActualClassSymbol.STRING;
    }

    @Override
    public ClassSymbol visit(Bool bool_) {
        return ActualClassSymbol.BOOL;
    }

    @Override
    public ClassSymbol visit(IsVoid isVoid) {
        super.visit(isVoid);
        return ActualClassSymbol.BOOL;
    }

    @Override
    public ClassSymbol visit(New new_) {
        return new_.type.accept(this);
    }

    private static ClassSymbol resolveReturnTypeToActualType(ClassSymbol instanceType, ClassSymbol returnType) {
        if (!returnType.isSelfType())
            return returnType;

        return instanceType;
    }

    @Override
    public ClassSymbol visit(Dispatch dispatch) {
        var id = dispatch.id;
        var idName = id.getToken().getText();
        var decoratedIdName = MethodSymbol.decorate(idName);
        var scope = id.getScope();

        // Tipul instanței pe care se aplică metoda.
        ClassSymbol instanceType;

        // Clasa în care se va căuta metoda apelată.
        ClassSymbol lookupType;

        // În prima parte se determină structura apelului (static sau dinamic, instanță implicită sau explicită)
        if (dispatch.instance == null) {
            // Nu se specifică o instanță anume. Se consideră că apelul se aplică implicit
            // pe atributul self al clasei curente (clasa din care e apelată metoda)
            // Apelul simplificat arată astfel: f(e')

            var selfSymbol = (IdSymbol)scope.lookup("self");
            if (selfSymbol == null) {
                SymbolTable.error(dispatch.id, "Method " + idName + " cant find implicit self");
                return null;
            }

            instanceType = selfSymbol.getType().getActualType();
            lookupType = instanceType;
        }
        else {
            instanceType = dispatch.instance.accept(this);
            if (instanceType == null) {
                return null;
            }

            instanceType = instanceType.getActualType();

            if (dispatch.type == null) {
                // Nu se specifică o superclasă anume. Metoda va fi căutată în clasa typeof(e)
                // Apelul simplificat arată astfel: e.f(e')
                lookupType = instanceType;
            }
            else {
                // Se specifică o superclasă. Metoda va fi căutată în clasa C.
                // Apelul arată astfel: e@C.f(e')
                lookupType = dispatch.type.getSymbol();
            }
        }

        if (!instanceType.isSubclassOf(lookupType)) {
            SymbolTable.error(dispatch.type, "Type " + lookupType + " of static dispatch is not a superclass of type " + instanceType);
            return null;
        }

        var methodSymbol = (MethodSymbol) lookupType.lookup(decoratedIdName);
        if (methodSymbol == null) {
            SymbolTable.error(dispatch.id, "Undefined method " + idName + " in class " + lookupType);
            return null;
        }

        var methodFormals = methodSymbol.getFormals();
        if (methodFormals.size() != dispatch.args.size()) {
            SymbolTable.error(dispatch.id, "Method " + idName + " of class " + lookupType + " is applied to wrong number of arguments");
            return resolveReturnTypeToActualType(instanceType, methodSymbol.getType());
        }

        var actualInvokeTypes = dispatch.args
                .stream()
                .map(x -> new Pair<>(x, x.accept(this)))
                .filter(x -> x.b != null);

        if (actualInvokeTypes.count() != methodFormals.size()) {
            SymbolTable.error(dispatch.id, "Method " + idName + " of class " + lookupType + "  something bad happened");
            // Una sau mai multe expresii ale parametrilor actuali a produs o eroare de tip. Stop!
            return resolveReturnTypeToActualType(instanceType, methodSymbol.getType());
        }

        actualInvokeTypes = dispatch.args
                .stream()
                .map(x -> new Pair<>(x, x.accept(this)))
                .filter(x -> x.b != null);

        class TypeMismatch {
            final IdSymbol formalSymbol;
            final ClassSymbol actualType;
            final Expression actualExpr;

            TypeMismatch(IdSymbol formalSymbol, ClassSymbol actualType, Expression actualExpr) {
                this.formalSymbol = formalSymbol;
                this.actualType = actualType;
                this.actualExpr = actualExpr;
            }
        }

        var typeMismatch = stream.zip(
                        methodSymbol.getFormals().entrySet().stream(),
                        actualInvokeTypes,
                        (formalEntry, actualType) -> {
                            var formalName = formalEntry.getKey();
                            var formalSymbol = formalEntry.getValue();
                            var formalType = formalSymbol.getType();

                            if (!actualType.b.isSubclassOf(formalType)) {
                                return new TypeMismatch(formalSymbol, actualType.b, actualType.a);
                            }

                            return null;
                        }
                )
                .filter(Objects::nonNull)
                .findFirst();

        if (typeMismatch.isPresent()) {
            var mismatch = typeMismatch.get();

            SymbolTable.error(mismatch.actualExpr, "In call to method " + methodSymbol + " of class " + lookupType + ", actual type " + mismatch.actualType + " of formal parameter " + mismatch.formalSymbol.getName() + " is incompatible with declared type " + mismatch.formalSymbol.getType());
            return resolveReturnTypeToActualType(instanceType, methodSymbol.getType());
        }

        id.setSymbol(methodSymbol);
        return resolveReturnTypeToActualType(instanceType, methodSymbol.getType());
    }

    @Override
    public ClassSymbol visit(Block block) {
        return block.exprs
                .stream()
                .map(x -> x.accept(this))
                .filter(Objects::nonNull)
                .reduce((a, b) -> b)
                .orElse(ActualClassSymbol.OBJECT);
    }

    private interface AssignmentErrorMessageFormatter {
        java.lang.String format(ClassSymbol exprType, IdSymbol idSymbol, ClassSymbol idType);
    }

    private ClassSymbol validateAssignment(Id destNode, Expression exprNode, AssignmentErrorMessageFormatter errorFormatter) {
        var idSymbol = destNode.getSymbol();
        if (idSymbol == null)
            return null;

        var idType = idSymbol.getType();
        if (idType == null)
            return null;

        ClassSymbol actualIdType;
        if (idType.isSelfType())
            actualIdType = ((IdSymbol)destNode.getScope().lookup("self")).getType().getActualType();
        else
            actualIdType = idType;

        // Verificare dacă există expresie de inițializare (eg: pentru let)
        if (exprNode == null)
            return idType;

        // TODO: Poate ar trebui ca în caz de eroare (exprType == null)
        //       să întorc de asemenea null
        var exprType = exprNode.accept(this);
        if (exprType != null && !exprType.isSubclassOf(actualIdType)) {
            SymbolTable.error(exprNode, errorFormatter.format(exprType, idSymbol, idType));
            return null;
        }

        return idType;
    }

    @Override
    public ClassSymbol visit(Assign assign) {
        return validateAssignment(
                assign.id,
                assign.expr,
                (exprType, idSymbol, idType) -> "Type " + exprType + " of assigned expression is incompatible with declared type " + idType + " of identifier " + idSymbol
        );
    }

    @Override
    public ClassSymbol visit(MethodDef methodDef) {
        return validateAssignment(
                methodDef.id,
                methodDef.body,
                (exprType, idSymbol, idType) -> "Type " + exprType + " of the body of method " + idSymbol + " is incompatible with declared return type " + idType
        );
    }

    @Override
    public ClassSymbol visit(AttributeDef attributeDef) {
        return validateAssignment(
                attributeDef.id,
                attributeDef.initValue,
                (exprType, idSymbol, idType) -> "Type " + exprType + " of initialization expression of attribute " + idSymbol + " is incompatible with declared type " + idType
        );
    }

    @Override
    public ClassSymbol visit(LocalDef localDef) {
        return validateAssignment(
                localDef.id,
                localDef.initValue,
                (exprType, idSymbol, idType) -> "Type " + exprType + " of initialization expression of identifier " + idSymbol + " is incompatible with declared type " + idType
        );
    }

    @Override
    public ClassSymbol visit(Let let) {
        let.vars.forEach(x -> x.accept(this));
        return let.body.accept(this);
    }

    @Override
    public ClassSymbol visit(CaseTest caseTest) {
        return caseTest.body.accept(this);
    }

    @Override
    public ClassSymbol visit(Case case_) {
        case_.instance.accept(this);

        var lub = case_.caseTests
                .stream()
                .map(x -> x.accept(this))
                .filter(Objects::nonNull)
                .reduce(ClassSymbol::getLeastUpperBound);

        return lub.orElse(ActualClassSymbol.OBJECT);
    }

    @Override
    public ClassSymbol visit(While while_) {
        var condType = while_.cond.accept(this);
        while_.body.accept(this);

        if (condType != null && condType != ActualClassSymbol.BOOL) {
            SymbolTable.error(while_.cond, "While condition has type " + condType + " instead of Bool");
        }

        return ActualClassSymbol.OBJECT;
    }

    @Override
    public ClassSymbol visit(If if_) {
        var condType = if_.cond.accept(this);
        var thenType = if_.thenBranch.accept(this);
        var elseType = if_.elseBranch.accept(this);

        // Dacă oricare subexpresie generează o eroare de tip, consider că întreaga expresie if are tipul Object.
        if (condType != null && condType != ActualClassSymbol.BOOL) {
            SymbolTable.error(if_.cond, "If condition has type " + condType + " instead of Bool");
            return ActualClassSymbol.OBJECT;
        }

        if (condType == null || thenType == null || elseType == null)
            return ActualClassSymbol.OBJECT;

        return thenType.getLeastUpperBound(elseType);
    }

    private boolean validateRelationalArithmeticOperand(ASTNode node, ClassSymbol type, ASTNode operator, ClassSymbol expectedType)
    {
        if (type != expectedType) {
            SymbolTable.error(node, "Operand of " + operator.getToken().getText() + " has type " + type + " instead of " + expectedType);
            return false;
        }

        return true;
    }

    private boolean validateRelationalArithmeticOperation(ASTNode leftNode, ASTNode rightNode, ASTNode operator, ClassSymbol expectedType)
    {
        var leftType = leftNode.accept(this);
        var rightType = rightNode == null ? null : rightNode.accept(this);

        if (leftType == null || rightNode != null && rightType == null)
            return false;

        if (!validateRelationalArithmeticOperand(leftNode, leftType, operator, expectedType))
            return false;

        if (rightNode != null && !validateRelationalArithmeticOperand(rightNode, rightType, operator, expectedType))
            return false;

        return true;
    }

    @Override
    public ClassSymbol visit(Relational rel) {
        var operator = rel.getToken();

        if (operator.getType() == CoolParser.EQUAL) {
            var leftType = rel.left.accept(this);
            var rightType = rel.right.accept(this);

            if (leftType == null || rightType == null)
                return null;

            if (leftType != rightType && (leftType.isPrimitive() || rightType.isPrimitive())) {
                SymbolTable.error(rel, "Cannot compare " + leftType + " with " + rightType);
                return null;
            }
        }
        else if (!validateRelationalArithmeticOperation(rel.left, rel.right, rel, ActualClassSymbol.INT)) {
            return null;
        }

        return ActualClassSymbol.BOOL;
    }

    @Override
    public ClassSymbol visit(Not not) {
        if (!validateRelationalArithmeticOperation(not.expr, null, not, ActualClassSymbol.BOOL))
            return null;

        return ActualClassSymbol.BOOL;
    }

    @Override
    public ClassSymbol visit(Plus plus) {
        if (!validateRelationalArithmeticOperation(plus.left, plus.right, plus, ActualClassSymbol.INT))
            return null;

        return ActualClassSymbol.INT;
    }

    @Override
    public ClassSymbol visit(Minus minus) {
        if (!validateRelationalArithmeticOperation(minus.left, minus.right, minus, ActualClassSymbol.INT))
            return null;

        return ActualClassSymbol.INT;
    }

    @Override
    public ClassSymbol visit(Mult mult) {
        if (!validateRelationalArithmeticOperation(mult.left, mult.right, mult, ActualClassSymbol.INT))
            return null;

        return ActualClassSymbol.INT;
    }

    @Override
    public ClassSymbol visit(Div div) {
        if (!validateRelationalArithmeticOperation(div.left, div.right, div, ActualClassSymbol.INT))
            return null;

        return ActualClassSymbol.INT;
    }

    @Override
    public ClassSymbol visit(Negate negate) {
        // Checker-ul nu vrea această eroare propagată mai sus
        validateRelationalArithmeticOperation(negate.expr, null, negate, ActualClassSymbol.INT);
        return ActualClassSymbol.INT;
    }
}
