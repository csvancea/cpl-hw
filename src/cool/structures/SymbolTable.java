package cool.structures;

import java.io.File;
import java.util.List;

import cool.parser.AST.ASTNode;
import org.antlr.v4.runtime.*;

import cool.compiler.Compiler;
import cool.parser.CoolParser;
import org.antlr.v4.runtime.misc.Pair;

public class SymbolTable {
    public static Scope globals;
    
    private static boolean semanticErrors;
    
    public static void defineBasicClasses() {
        globals = new DefaultScope(null);
        semanticErrors = false;
        
        // Populate global scope.
        globals.add(ActualClassSymbol.OBJECT);
        globals.add(ActualClassSymbol.IO);
        globals.add(ActualClassSymbol.INT);
        globals.add(ActualClassSymbol.STRING);
        globals.add(ActualClassSymbol.BOOL);

        // Define methods of the built-in types
        defineMethod(ActualClassSymbol.OBJECT, "abort", ActualClassSymbol.OBJECT);
        defineMethod(ActualClassSymbol.OBJECT, "type_name", ActualClassSymbol.STRING);
        defineMethod(ActualClassSymbol.OBJECT, "copy", ActualClassSymbol.OBJECT.SELF_TYPE);


        defineMethod(ActualClassSymbol.IO, "out_string", ActualClassSymbol.IO.SELF_TYPE, List.of(
                new Pair<>("x", ActualClassSymbol.STRING)
        ));
        defineMethod(ActualClassSymbol.IO, "out_int", ActualClassSymbol.IO.SELF_TYPE, List.of(
                new Pair<>("x", ActualClassSymbol.INT)
        ));
        defineMethod(ActualClassSymbol.IO, "in_string", ActualClassSymbol.STRING);
        defineMethod(ActualClassSymbol.IO, "in_int", ActualClassSymbol.INT);


        defineMethod(ActualClassSymbol.STRING, "length", ActualClassSymbol.INT);
        defineMethod(ActualClassSymbol.STRING, "concat", ActualClassSymbol.STRING, List.of(
                new Pair<>("s", ActualClassSymbol.STRING)
        ));
        defineMethod(ActualClassSymbol.STRING, "substr", ActualClassSymbol.STRING, List.of(
                new Pair<>("i", ActualClassSymbol.INT),
                new Pair<>("l", ActualClassSymbol.INT)
        ));

        // Fake attributes. Necesare pentru calcularea corectă a dimensiunii obiectului prototip pentru generarea de cod
        ActualClassSymbol.INT.add(new IdSymbol("$int", IdSymbol.DefinitionType.ATTRIBUTE).setType(ActualClassSymbol.OBJECT));
        ActualClassSymbol.STRING.add(new IdSymbol("$len", IdSymbol.DefinitionType.ATTRIBUTE).setType(ActualClassSymbol.OBJECT));
        ActualClassSymbol.STRING.add(new IdSymbol("$string", IdSymbol.DefinitionType.ATTRIBUTE).setType(ActualClassSymbol.OBJECT));
        ActualClassSymbol.BOOL.add(new IdSymbol("$bool", IdSymbol.DefinitionType.ATTRIBUTE).setType(ActualClassSymbol.OBJECT));
    }

    private static void defineMethod(ClassSymbol classSymbol, String methodName, ClassSymbol returnSymbol) {
        defineMethod(classSymbol, methodName, returnSymbol, List.of());
    }

    private static void defineMethod(ClassSymbol classSymbol, String methodName, ClassSymbol returnSymbol, List<Pair<String, ClassSymbol>> formals) {
        var methodSymbol = new MethodSymbol(classSymbol, methodName);

        formals.forEach((p) -> methodSymbol.add(new IdSymbol(p.a, IdSymbol.DefinitionType.FORMAL).setType(p.b)));

        methodSymbol.setType(returnSymbol);
        classSymbol.add(methodSymbol);
    }
    
    /**
     * Displays a semantic error message.
     * 
     * @param ctx Used to determine the enclosing class context of this error,
     *            which knows the file name in which the class was defined.
     * @param info Used for line and column information.
     * @param str The error message.
     */
    public static void error(ParserRuleContext ctx, Token info, String str) {
        while (! (ctx.getParent() instanceof CoolParser.ProgramContext))
            ctx = ctx.getParent();
        
        String message = "\"" + new File(Compiler.fileNames.get(ctx)).getName()
                + "\", line " + info.getLine()
                + ":" + (info.getCharPositionInLine() + 1)
                + ", Semantic error: " + str;
        
        System.err.println(message);
        
        semanticErrors = true;
    }

    public static void error(ASTNode info, String str) {
        error(info.getParserRuleContext(), info.getToken(), str);
    }
    
    public static void error(String str) {
        String message = "Semantic error: " + str;
        
        System.err.println(message);
        
        semanticErrors = true;
    }
    
    public static boolean hasSemanticErrors() {
        return semanticErrors;
    }
}
