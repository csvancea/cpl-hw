package cool.parser.AST;

import cool.structures.*;

public class ASTDefinitionPassVisitor extends ASTDefaultVisitor<Void> {
    private Scope currentScope = null;
    private ClassSymbol currentClassSymbol = null;
    private MethodSymbol currentMethodSymbol = null;

    @Override
    public Void visit(Id id) {
        id.setScope(currentScope);
        return null;
    }

    @Override
    public Void visit(New new_) {
        new_.setScope(currentScope);
        return null;
    }

    @Override
    public Void visit(CaseTest caseTest) {
        var id = caseTest.id;
        var idName = id.getToken().getText();
        var idSymbol = new IdSymbol(idName, IdSymbol.DefinitionType.LOCAL); // TODO: is this a special definition type?

        if (idName.equals("self")) {
            SymbolTable.error(id, "Case variable has illegal name self");
            return null;
        }

        currentScope = new DefaultScope(currentScope);

        currentScope.add(idSymbol);
        id.setSymbol(idSymbol);
        id.setScope(currentScope);
        caseTest.body.accept(this);

        currentScope = currentScope.getParent();
        return null;
    }

    @Override
    public Void visit(Case case_) {
        // Ugly hack: pentru case se rezervă spațiu pe stivă pentru o variabilă. Offset-ul corespunzător este salvat în
        // toate variabilele introduse de expresia case în ramurile sale. Offset-ul este același pentru fiecare ramură.
        int localDef = currentMethodSymbol.registerLocalDef();

        case_.instance.accept(this);
        case_.caseTests.forEach(x -> {
            x.accept(this);
            if (x.id.getSymbol() != null) {
                x.id.getSymbol().setIndex(localDef);
            }
        });
        return null;
    }

    @Override
    public Void visit(Assign assign) {
        var id = assign.id;
        var idName = id.getToken().getText();

        if (idName.equals("self")) {
            SymbolTable.error(id, "Cannot assign to self");
        }

        return super.visit(assign);
    }

    @Override
    public Void visit(LocalDef localDef) {
        var id = localDef.id;
        var idName = id.getToken().getText();
        var idSymbol = new IdSymbol(idName, IdSymbol.DefinitionType.LOCAL);

        if (idName.equals("self")) {
            SymbolTable.error(id, "Let variable has illegal name self");
            return null;
        }

        // Expresiile de inițializare sunt evaluate în cadrul scope-ului
        // părinte al variabilei nou definite, astfel variabila nu va fi
        // vizibilă în interiorul propriei expresii de inițializare.
        if (localDef.initValue != null) {
            var originalScope = currentScope;

            currentScope = currentScope.getParent();
            localDef.initValue.accept(this);
            currentScope = originalScope;
        }

        currentScope.add(idSymbol);
        id.setSymbol(idSymbol);
        id.setScope(currentScope);

        idSymbol.setIndex(currentMethodSymbol.registerLocalDef());
        return null;
    }

    @Override
    public Void visit(Let let) {
        var originalScope = currentScope;

        // Fiecare variabilă nou introdusă va avea propriul ei scope.
        // În cazul expresiilor let ce introduc mai multe variabile,
        // scope-urile sunt imbricate. Corpul va fi evaluat în contextul
        // ultimului scope (va cuprinde toate variabilele).
        let.vars.forEach(x -> {
            currentScope = new DefaultScope(currentScope);
            x.accept(this);
        });
        let.body.accept(this);

        currentScope = originalScope;
        return null;
    }

    @Override
    public Void visit(Dispatch dispatch) {
        dispatch.id.setScope(currentScope);
        return super.visit(dispatch);
    }

    @Override
    public Void visit(Formal formal) {
        var id = formal.id;
        var idName = id.getToken().getText();
        var idSymbol = new IdSymbol(idName, IdSymbol.DefinitionType.FORMAL);

        if (idName.equals("self")) {
            SymbolTable.error(id, "Method " + currentMethodSymbol + " of class " + currentClassSymbol + " has formal parameter with illegal name self");
            return null;
        }

        if (!currentScope.add(idSymbol)) {
            SymbolTable.error(id, "Method " + currentMethodSymbol + " of class " + currentClassSymbol + " redefines formal parameter " + idName);
            return null;
        }

        id.setSymbol(idSymbol);
        id.setScope(currentScope);

        return null;
    }

    @Override
    public Void visit(MethodDef methodDef) {
        var id = methodDef.id;
        var idName = id.getToken().getText();
        currentMethodSymbol = new MethodSymbol(currentScope, idName);

        if (!currentScope.add(currentMethodSymbol)) {
            SymbolTable.error(id, "Class " + currentClassSymbol + " redefines method " + idName);
            return null;
        }

        id.setSymbol(currentMethodSymbol);
        id.setScope(currentScope);

        currentScope = currentMethodSymbol;
        methodDef.formals.forEach(x -> x.accept(this));
        methodDef.body.accept(this);
        currentScope = currentScope.getParent();

        int formalIdx = 0;
        for (var formal : currentMethodSymbol.getFormals().values()) {
            formal.setIndex(formalIdx++);
        }
        currentMethodSymbol = null;

        return null;
    }

    @Override
    public Void visit(AttributeDef attributeDef) {
        var id = attributeDef.id;
        var idName = id.getToken().getText();
        var idSymbol = new IdSymbol(idName, IdSymbol.DefinitionType.ATTRIBUTE);

        if (idName.equals("self")) {
            SymbolTable.error(id, "Class " + currentClassSymbol + " has attribute with illegal name self");
            return null;
        }

        if (!currentScope.add(idSymbol)) {
            SymbolTable.error(id, "Class " + currentClassSymbol + " redefines attribute " + idName);
            return null;
        }

        id.setSymbol(idSymbol);
        id.setScope(currentScope);

        if (attributeDef.initValue != null) {
            attributeDef.initValue.accept(this);
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

        currentClassSymbol = new ActualClassSymbol(null, classTypeName);
        if (!SymbolTable.globals.add(currentClassSymbol)) {
            SymbolTable.error(classType, "Class " + classTypeName + " is redefined");
            return null;
        }
        classType.setSymbol(currentClassSymbol);

        currentScope = currentClassSymbol;
        class_.features.forEach(x -> x.accept(this));
        currentScope = null;
        currentClassSymbol = null;

        return null;
    }

    @Override
    public Void visit(Program program) {
        program.classes.forEach(x -> x.accept(this));
        return null;
    }
}
