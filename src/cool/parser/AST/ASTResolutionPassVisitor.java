package cool.parser.AST;

import cool.parser.CoolParser;
import cool.structures.ClassSymbol;
import cool.structures.SymbolTable;

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
    public ClassSymbol visit(Assign assign) {
        var id = assign.id;
        var idName = id.getToken().getText();
        var idSymbol = id.getSymbol();
        var idType = id.accept(this);

        if (idType == null)
            return null;

        var expr = assign.expr;
        var exprType = expr.accept(this);

        if (exprType == null)
            return null;

        if (idName.equals("self")) {
            SymbolTable.error(id, "Cannot assign to self");
            return null;
        }

        if (!exprType.isSubclassOf(idType)) {
            SymbolTable.error(expr, "Type " + exprType + " of assigned expression is incompatible with declared type " + idType + " of identifier " + idSymbol);
            return null;
        }

        // Operația de atribuire nu are tip în Cool.
        return null;
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