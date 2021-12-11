package cool.parser.AST;

import cool.parser.CoolParser;
import cool.structures.ClassSymbol;
import cool.structures.IdSymbol;
import cool.structures.SymbolTable;

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
        return ClassSymbol.INT;
    }

    @Override
    public ClassSymbol visit(String string) {
        return ClassSymbol.STRING;
    }

    @Override
    public ClassSymbol visit(Bool bool_) {
        return ClassSymbol.BOOL;
    }

    @Override
    public ClassSymbol visit(IsVoid isVoid) {
        super.visit(isVoid);
        return ClassSymbol.BOOL;
    }

    @Override
    public ClassSymbol visit(New new_) {
        return new_.type.accept(this);
    }

    @Override
    public ClassSymbol visit(Block block) {
        return block.exprs
                .stream()
                .map(x -> x.accept(this))
                .filter(Objects::nonNull)
                .reduce((a, b) -> b)
                .orElse(ClassSymbol.OBJECT);
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

        // Verificare dacă există expresie de inițializare (eg: pentru let)
        if (exprNode == null)
            return idType;

        // TODO: Poate ar trebui ca în caz de eroare (exprType == null)
        //       să întorc de asemenea null
        var exprType = exprNode.accept(this);
        if (exprType != null && !exprType.isSubclassOf(idType)) {
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

        return lub.orElse(ClassSymbol.OBJECT);
    }

    @Override
    public ClassSymbol visit(While while_) {
        var condType = while_.cond.accept(this);
        while_.body.accept(this);

        if (condType != null && condType != ClassSymbol.BOOL) {
            SymbolTable.error(while_.cond, "While condition has type " + condType + " instead of Bool");
        }

        return ClassSymbol.OBJECT;
    }

    @Override
    public ClassSymbol visit(If if_) {
        var condType = if_.cond.accept(this);
        var thenType = if_.thenBranch.accept(this);
        var elseType = if_.elseBranch.accept(this);

        // Dacă oricare subexpresie generează o eroare de tip, consider că întreaga expresie if are tipul Object.
        if (condType != null && condType != ClassSymbol.BOOL) {
            SymbolTable.error(if_.cond, "If condition has type " + condType + " instead of Bool");
            return ClassSymbol.OBJECT;
        }

        if (condType == null || thenType == null || elseType == null)
            return ClassSymbol.OBJECT;

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
        else if (!validateRelationalArithmeticOperation(rel.left, rel.right, rel, ClassSymbol.INT)) {
            return null;
        }

        return ClassSymbol.BOOL;
    }

    @Override
    public ClassSymbol visit(Not not) {
        if (!validateRelationalArithmeticOperation(not.expr, null, not, ClassSymbol.BOOL))
            return null;

        return ClassSymbol.BOOL;
    }

    @Override
    public ClassSymbol visit(Plus plus) {
        if (!validateRelationalArithmeticOperation(plus.left, plus.right, plus, ClassSymbol.INT))
            return null;

        return ClassSymbol.INT;
    }

    @Override
    public ClassSymbol visit(Minus minus) {
        if (!validateRelationalArithmeticOperation(minus.left, minus.right, minus, ClassSymbol.INT))
            return null;

        return ClassSymbol.INT;
    }

    @Override
    public ClassSymbol visit(Mult mult) {
        if (!validateRelationalArithmeticOperation(mult.left, mult.right, mult, ClassSymbol.INT))
            return null;

        return ClassSymbol.INT;
    }

    @Override
    public ClassSymbol visit(Div div) {
        if (!validateRelationalArithmeticOperation(div.left, div.right, div, ClassSymbol.INT))
            return null;

        return ClassSymbol.INT;
    }

    @Override
    public ClassSymbol visit(Negate negate) {
        // Checker-ul nu vrea această eroare propagată mai sus
        validateRelationalArithmeticOperation(negate.expr, null, negate, ClassSymbol.INT);
        return ClassSymbol.INT;
    }
}
