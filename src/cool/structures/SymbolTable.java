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
        ClassSymbol.OBJECT.setParent(globals);
        globals.add(ClassSymbol.OBJECT);
        globals.add(ClassSymbol.IO);
        globals.add(ClassSymbol.INT);
        globals.add(ClassSymbol.STRING);
        globals.add(ClassSymbol.BOOL);
        globals.add(ClassSymbol.SELF_TYPE); // TODO: check if this makes sense

        // Define methods of the built-in types
        defineMethod(ClassSymbol.OBJECT, "abort", ClassSymbol.OBJECT);
        defineMethod(ClassSymbol.OBJECT, "type_name", ClassSymbol.STRING);
        defineMethod(ClassSymbol.OBJECT, "copy", ClassSymbol.SELF_TYPE);


        defineMethod(ClassSymbol.IO, "out_string", ClassSymbol.SELF_TYPE, new LinkedHashMap<>(
                Map.of("x", ClassSymbol.STRING)
        ));
        defineMethod(ClassSymbol.IO, "out_int", ClassSymbol.SELF_TYPE, new LinkedHashMap<>(
                Map.of("x", ClassSymbol.INT)
        ));
        defineMethod(ClassSymbol.IO, "in_string", ClassSymbol.STRING);
        defineMethod(ClassSymbol.IO, "in_int", ClassSymbol.INT);


        defineMethod(ClassSymbol.STRING, "length", ClassSymbol.INT);
        defineMethod(ClassSymbol.STRING, "concat", ClassSymbol.STRING, new LinkedHashMap<>(
                Map.of("s", ClassSymbol.STRING)
        ));
        defineMethod(ClassSymbol.STRING, "substr", ClassSymbol.STRING, new LinkedHashMap<>(
                Map.of("i", ClassSymbol.INT, "l", ClassSymbol.INT)
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
