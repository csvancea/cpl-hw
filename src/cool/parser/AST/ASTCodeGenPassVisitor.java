package cool.parser.AST;

import cool.structures.ActualClassSymbol;
import cool.structures.ClassSymbol;
import org.stringtemplate.v4.ST;

public class ASTCodeGenPassVisitor extends ASTDefaultVisitor<ST> {
    private void assignClassTagIds(ClassSymbol sym) {
        var parentClass = (ClassSymbol) sym.getParent();

        // Determin eticheta clasei
        int parentMaxSubTreeTag = (parentClass == null) ? 0 : parentClass.getMaxSubTreeTag();
        sym.setTag(parentMaxSubTreeTag);
        sym.setMaxSubTreeTag(parentMaxSubTreeTag + 1);

        // Parcurgere recursivă a subclaselor (în ordine DFS -- important pentru asignarea corectă a tagurilor)
        for (var c : sym.getChildren()) {
            assignClassTagIds(c);
        }

        if (parentClass != null) {
            parentClass.setMaxSubTreeTag(sym.getMaxSubTreeTag());
        }
    }

    @Override
    public ST visit(Program program) {
        assignClassTagIds(ActualClassSymbol.OBJECT);
        return null;
    }
}
