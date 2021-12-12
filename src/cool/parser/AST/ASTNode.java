package cool.parser.AST;

import cool.structures.ClassSymbol;
import cool.structures.IdSymbol;
import cool.structures.Scope;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.List;

// Rădăcina ierarhiei de clase reprezentând nodurile arborelui de sintaxă
// abstractă (AST). Singura metodă permite primirea unui visitor.
public abstract class ASTNode {
    // Reținem un token descriptiv al nodului, pentru a putea afișa ulterior
    // informații legate de linia și coloana eventualelor erori semantice.
    private final ParserRuleContext parserRuleContext;
    private final Token token;

    ASTNode(ParserRuleContext parserRuleContext, Token token) {
        this.parserRuleContext = parserRuleContext;
        this.token = token;
    }

    public ParserRuleContext getParserRuleContext() {
        return parserRuleContext;
    }
    
    public Token getToken() {
        return token;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return null;
    }
}

// Orice expresie.
abstract class Expression extends ASTNode {
    Expression(ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
    }
}

// Identificatori
class Id extends Expression {
    private IdSymbol symbol;
    private Scope scope;

    Id(ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
    }
    
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    IdSymbol getSymbol() {
        return symbol;
    }

    void setSymbol(IdSymbol symbol) {
        this.symbol = symbol;
    }

    Scope getScope() {
        return scope;
    }

    void setScope(Scope scope) {
        this.scope = scope;
    }
}

// Literali întregi
class Int extends Expression {
    Int(ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Literali șiruri de caractere
class String extends Expression {
    String(ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Literali bool
class Bool extends Expression {
    Bool(ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Construcția if.
class If extends Expression {
    // Sunt necesare trei câmpuri pentru cele trei componente ale expresiei.
    Expression cond;
    Expression thenBranch;
    Expression elseBranch;

    If(Expression cond,
       Expression thenBranch,
       Expression elseBranch,
       ParserRuleContext parserRuleContext,
       Token start) {
        super(parserRuleContext, start);
        this.cond = cond;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Construcția while.
class While extends Expression {
    Expression cond;
    Expression body;

    While(Expression cond, Expression body, ParserRuleContext parserRuleContext, Token start) {
        super(parserRuleContext, start);
        this.cond = cond;
        this.body = body;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Construcția block.
class Block extends Expression {
    List<Expression> exprs;

    Block(List<Expression> exprs, ParserRuleContext parserRuleContext, Token start) {
        super(parserRuleContext, start);
        this.exprs = exprs;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Variabilă locală. Inițializarea poate să lipsească.
class LocalDef extends ASTNode {
    Type type;
    Id id;
    Expression initValue;

    LocalDef(Type type, Id id, Expression initValue, ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
        this.type = type;
        this.id = id;
        this.initValue = initValue;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Construcția let.
class Let extends Expression {
    List<LocalDef> vars;
    Expression body;

    Let(List<LocalDef> vars, Expression body, ParserRuleContext parserRuleContext, Token start) {
        super(parserRuleContext, start);
        this.vars = vars;
        this.body = body;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Fiecare CaseTest reprezintă câte o ramură a construcției case.
class CaseTest extends ASTNode {
    Id id;
    Type type;
    Expression body;

    CaseTest(Id id, Type type, Expression body, ParserRuleContext parserRuleContext, Token start) {
        super(parserRuleContext, start);
        this.id = id;
        this.type = type;
        this.body = body;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Construcția case.
class Case extends Expression {
    Expression instance;
    List<CaseTest> caseTests;

    Case(Expression instance, List<CaseTest> caseTests, ParserRuleContext parserRuleContext, Token start) {
        super(parserRuleContext, start);
        this.instance = instance;
        this.caseTests = caseTests;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Construcția new.
class New extends Expression {
    Type type;
    private Scope scope;

    New(Type type, ParserRuleContext parserRuleContext, Token start) {
        super(parserRuleContext, start);
        this.type = type;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    Scope getScope() {
        return scope;
    }

    void setScope(Scope scope) {
        this.scope = scope;
    }
}

// Construcția isvoid.
class IsVoid extends Expression {
    Expression instance;

    IsVoid(Expression instance, ParserRuleContext parserRuleContext, Token start) {
        super(parserRuleContext, start);
        this.instance = instance;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Token-ul pentru un Assign va fi '<-'
class Assign extends Expression {
    Id id;
    Expression expr;

    Assign(Id id, Expression expr, ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
        this.id = id;
        this.expr = expr;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Pentru un Relational avem 'op=(LT | LE | EQUAL)' ca reprezentare.
class Relational extends Expression {
    Expression left;
    Expression right;

    Relational(Expression left, Expression right, ParserRuleContext parserRuleContext, Token op) {
        super(parserRuleContext, op);
        this.left = left;
        this.right = right;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Pentru not, operatorul va fi 'not'.
class Not extends Expression {
    Expression expr;

    Not(Expression expr, ParserRuleContext parserRuleContext, Token op) {
        super(parserRuleContext, op);
        this.expr = expr;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Pentru un PlusMinus avem 'op=(PLUS | MINUS)' ca reprezentare.
class Plus extends Expression {
    Expression left;
    Expression right;

    Plus(Expression left, Expression right, ParserRuleContext parserRuleContext, Token op) {
        super(parserRuleContext, op);
        this.left = left;
        this.right = right;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Minus extends Expression {
    Expression left;
    Expression right;

    Minus(Expression left, Expression right, ParserRuleContext parserRuleContext, Token op) {
        super(parserRuleContext, op);
        this.left = left;
        this.right = right;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Pentru un MultDiv avem 'op=(MULT | DIV)' ca reprezentare.
class Mult extends Expression {
    Expression left;
    Expression right;

    Mult(Expression left, Expression right, ParserRuleContext parserRuleContext, Token op) {
        super(parserRuleContext, op);
        this.left = left;
        this.right = right;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Div extends Expression {
    Expression left;
    Expression right;

    Div(Expression left, Expression right, ParserRuleContext parserRuleContext, Token op) {
        super(parserRuleContext, op);
        this.left = left;
        this.right = right;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Pentru Negate, operatorul va fi '~'.
class Negate extends Expression {
    Expression expr;

    Negate(Expression expr, ParserRuleContext parserRuleContext, Token op) {
        super(parserRuleContext, op);
        this.expr = expr;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Apelul unei metode poate avea oricâte argumente. Le vom salva într-o listă.
// În plus, pentru dispatch explicit trebuie salvate instanța și (super-)tipul specificat.
class Dispatch extends Expression {
    Expression instance;
    Type type;
    Id id;
    List<Expression> args;

    Dispatch(Expression instance, Type type, Id id, List<Expression> args, ParserRuleContext parserRuleContext, Token start) {
        super(parserRuleContext, start);
        this.instance = instance;
        this.type = type;
        this.id = id;
        this.args = args;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Tipul unei expresii sau al unui feature.
class Type extends ASTNode {
    private ClassSymbol symbol;

    Type(ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    ClassSymbol getSymbol() {
        return symbol;
    }

    void setSymbol(ClassSymbol symbol) {
        this.symbol = symbol;
    }
}

// Argumentul din definiția unei metode.
class Formal extends ASTNode {
    Type type;
    Id id;

    Formal(Type type, Id id, ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
        this.type = type;
        this.id = id;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Clasă abstractă ce denotă un feature al unei clase.
// Prin feature se înțelege fie o definiție de metodă, fie un atribut.
abstract class Feature extends ASTNode {
    Feature(ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
    }
}

// Inițializarea poate să lipsească.
class AttributeDef extends Feature {
    Type type;
    Id id;
    Expression initValue;

    AttributeDef(Type type, Id id, Expression initValue, ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
        this.type = type;
        this.id = id;
        this.initValue = initValue;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Definirea unei metode.
class MethodDef extends Feature {
    Type type;
    Id id;
    List<Formal> formals;
    Expression body;

    MethodDef(Type type, Id id, List<Formal> formals, Expression body, ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
        this.type = type;
        this.id = id;
        this.formals = formals;
        this.body = body;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Definirea unei clase.
class ClassDef extends ASTNode {
    Type type;
    Type superType;
    List<Feature> features;

    ClassDef(Type type, Type superType, List<Feature> features, ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
        this.type = type;
        this.superType = superType;
        this.features = features;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

// Programul.
class Program extends ASTNode {
    List<ClassDef> classes;

    Program(List<ClassDef> classes, ParserRuleContext parserRuleContext, Token token) {
        super(parserRuleContext, token);
        this.classes = classes;
    }

    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
