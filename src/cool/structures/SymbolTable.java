package cool.structures;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import cool.parser.AST.ASTNode;
import org.antlr.v4.runtime.*;

import cool.compiler.Compiler;
import cool.parser.CoolParser;

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


        defineMethod(ActualClassSymbol.IO, "out_string", ActualClassSymbol.IO.SELF_TYPE, new LinkedHashMap<>(
                Map.of("x", ActualClassSymbol.STRING)
        ));
        defineMethod(ActualClassSymbol.IO, "out_int", ActualClassSymbol.IO.SELF_TYPE, new LinkedHashMap<>(
                Map.of("x", ActualClassSymbol.INT)
        ));
        defineMethod(ActualClassSymbol.IO, "in_string", ActualClassSymbol.STRING);
        defineMethod(ActualClassSymbol.IO, "in_int", ActualClassSymbol.INT);


        defineMethod(ActualClassSymbol.STRING, "length", ActualClassSymbol.INT);
        defineMethod(ActualClassSymbol.STRING, "concat", ActualClassSymbol.STRING, new LinkedHashMap<>(
                Map.of("s", ActualClassSymbol.STRING)
        ));
        defineMethod(ActualClassSymbol.STRING, "substr", ActualClassSymbol.STRING, new LinkedHashMap<>(
                Map.of("i", ActualClassSymbol.INT, "l", ActualClassSymbol.INT)
        ));
    }

    private static void defineMethod(ClassSymbol classSymbol, String methodName, ClassSymbol returnSymbol) {
        defineMethod(classSymbol, methodName, returnSymbol, new LinkedHashMap<>());
    }

    private static void defineMethod(ClassSymbol classSymbol, String methodName, ClassSymbol returnSymbol, LinkedHashMap<String, ClassSymbol> formals) {
        var methodSymbol = new MethodSymbol(classSymbol, methodName);
        methodSymbol.setType(returnSymbol);

        for (Map.Entry<String, ClassSymbol> entry : formals.entrySet()) {
            methodSymbol.add(
                    new IdSymbol(entry.getKey()).setType(entry.getValue())
            );
        }

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
