package cool.parser.AST;

import cool.parser.CoolParser;
import cool.parser.CoolParserBaseVisitor;

import java.util.stream.Collectors;

public class ASTConstructionVisitor extends CoolParserBaseVisitor<ASTNode> {
    @Override
    public ASTNode visitId(CoolParser.IdContext ctx) {
        return new Id(ctx.ID().getSymbol());
    }

    @Override
    public ASTNode visitInt(CoolParser.IntContext ctx) {
        return new Int(ctx.INT().getSymbol());
    }

    @Override
    public ASTNode visitString(CoolParser.StringContext ctx) {
        return new String(ctx.STRING().getSymbol());
    }

    @Override
    public ASTNode visitBool(CoolParser.BoolContext ctx) {
        return new Bool(ctx.BOOL().getSymbol());
    }

    @Override
    public ASTNode visitIf(CoolParser.IfContext ctx) {
        return new If((Expression)visit(ctx.cond),
                      (Expression)visit(ctx.thenBranch),
                      (Expression)visit(ctx.elseBranch),
                      ctx.start);
    }

    @Override
    public ASTNode visitWhile(CoolParser.WhileContext ctx) {
        return new While((Expression)visit(ctx.cond),
                         (Expression)visit(ctx.body),
                         ctx.start);
    }

    @Override
    public ASTNode visitBlock(CoolParser.BlockContext ctx) {
        var exprs = ctx.exprs.stream().map(x -> (Expression)visit(x)).collect(Collectors.toList());
        return new Block(exprs,
                         ctx.start);
    }

    @Override
    public ASTNode visitLet(CoolParser.LetContext ctx) {
        var vars = ctx.vars.stream().map(x -> (AttributeDef)visit(x)).collect(Collectors.toList());
        return new Let(vars,
                       (Expression)visit(ctx.body),
                       ctx.start);
    }

    @Override
    public ASTNode visitCaseTest(CoolParser.CaseTestContext ctx) {
        return new CaseTest(new Id(ctx.name),
                            new Type(ctx.type),
                            (Expression)visit(ctx.body),
                            ctx.start);
    }

    @Override
    public ASTNode visitCase(CoolParser.CaseContext ctx) {
        var cases = ctx.cases.stream().map(x -> (CaseTest)visit(x)).collect(Collectors.toList());
        return new Case((Expression)visit(ctx.instance),
                        cases,
                        ctx.start);
    }

    @Override
    public ASTNode visitNew(CoolParser.NewContext ctx) {
        return new New(new Type(ctx.type),
                       ctx.start);
    }

    @Override
    public ASTNode visitIsVoid(CoolParser.IsVoidContext ctx) {
        return new IsVoid((Expression)visit(ctx.instance),
                          ctx.start);
    }

    @Override
    public ASTNode visitAssign(CoolParser.AssignContext ctx) {
        return new Assign(new Id(ctx.name),
                          (Expression)visit(ctx.e),
                          ctx.ASSIGN().getSymbol());
    }

    @Override
    public ASTNode visitRelational(CoolParser.RelationalContext ctx) {
        return new Relational((Expression)visit(ctx.left),
                              (Expression)visit(ctx.right),
                              ctx.op);
    }

    @Override
    public ASTNode visitNot(CoolParser.NotContext ctx) {
        return new Not((Expression)visit(ctx.e),
                       ctx.NOT().getSymbol());
    }

    @Override
    public ASTNode visitPlusMinus(CoolParser.PlusMinusContext ctx) {
        if (ctx.op.getText().equals("+")) {
            return new Plus((Expression)visit(ctx.left),
                            (Expression)visit(ctx.right),
                            ctx.op);
        }
        else if (ctx.op.getText().equals("-")) {
            return new Minus((Expression)visit(ctx.left),
                             (Expression)visit(ctx.right),
                             ctx.op);
        } else {
            return null;
        }
    }

    @Override
    public ASTNode visitMultDiv(CoolParser.MultDivContext ctx) {
        if (ctx.op.getText().equals("*")) {
            return new Mult((Expression)visit(ctx.left),
                            (Expression)visit(ctx.right),
                            ctx.op);
        }
        else if (ctx.op.getText().equals("/")) {
            return new Div((Expression)visit(ctx.left),
                           (Expression)visit(ctx.right),
                           ctx.op);
        } else {
            return null;
        }
    }

    @Override
    public ASTNode visitNegate(CoolParser.NegateContext ctx) {
        return new Negate((Expression)visit(ctx.e),
                          ctx.NEG().getSymbol());
    }

    @Override
    public ASTNode visitParen(CoolParser.ParenContext ctx) {
        return visit(ctx.e);
    }

    @Override
    public ASTNode visitExplicitDispatch(CoolParser.ExplicitDispatchContext ctx) {
        var args = ctx.args.stream().map(x -> (Expression)visit(x)).collect(Collectors.toList());
        return new Dispatch((Expression)visit(ctx.instance),
                            ctx.type == null ? null : new Type(ctx.type),
                            new Id(ctx.name),
                            args,
                            ctx.start);
    }

    @Override
    public ASTNode visitImplicitDispatch(CoolParser.ImplicitDispatchContext ctx) {
        var args = ctx.args.stream().map(x -> (Expression)visit(x)).collect(Collectors.toList());
        return new Dispatch(null,
                            null,
                            new Id(ctx.name),
                            args,
                            ctx.start);
    }

    @Override
    public ASTNode visitFormal(CoolParser.FormalContext ctx) {
        return new Formal(new Type(ctx.type), new Id(ctx.name), ctx.start);
    }

    @Override
    public ASTNode visitVariable(CoolParser.VariableContext ctx) {
        return new AttributeDef(new Type(ctx.type),
                                new Id(ctx.name),
                                ctx.init == null ? null : (Expression)visit(ctx.init),
                                ctx.start
        );
    }

    @Override
    public ASTNode visitAttributeDef(CoolParser.AttributeDefContext ctx) {
        return visit(ctx.variable());
    }

    @Override
    public ASTNode visitMethodDef(CoolParser.MethodDefContext ctx) {
        var formals = ctx.formals.stream().map(x -> (Formal)visit(x)).collect(Collectors.toList());

        return new MethodDef(new Type(ctx.type),
                             new Id(ctx.name),
                             formals,
                             (Expression)visit(ctx.body),
                             ctx.start
        );
    }

    @Override
    public ASTNode visitClass_(CoolParser.Class_Context ctx) {
        var features = ctx.features.stream().map(x -> (Feature)visit(x)).collect(Collectors.toList());
        return new ClassDef(new Type(ctx.type),
                            ctx.super_ == null ? null : new Type(ctx.super_),
                            features,
                            ctx.start
        );
    }

    @Override
    public ASTNode visitProgram(CoolParser.ProgramContext ctx) {
        var classes = ctx.classes.stream().map(x -> (ClassDef)visit(x)).collect(Collectors.toList());
        return new Program(classes, ctx.start);
    }
}
