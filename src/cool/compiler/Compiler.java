package cool.compiler;

import cool.parser.AST.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import cool.lexer.*;
import cool.parser.*;
import cool.structures.SymbolTable;

import java.io.*;
import java.util.Arrays;


public class Compiler {
    // Annotates class nodes with the names of files where they are defined.
    public static ParseTreeProperty<String> fileNames = new ParseTreeProperty<>();

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("No file(s) given");
            return;
        }
        
        CoolLexer lexer = null;
        CommonTokenStream tokenStream = null;
        CoolParser parser = null;
        ParserRuleContext globalTree = null;
        
        // True if any lexical or syntax errors occur.
        boolean lexicalSyntaxErrors = false;
        
        // Parse each input file and build one big parse tree out of
        // individual parse trees.
        for (var fileName : args) {
            var input = CharStreams.fromFileName(fileName);
            
            // Lexer
            if (lexer == null)
                lexer = new CoolLexer(input);
            else
                lexer.setInputStream(input);

            // Token stream
            if (tokenStream == null)
                tokenStream = new CommonTokenStream(lexer);
            else
                tokenStream.setTokenSource(lexer);
                
            /*
            // Test lexer only.
            tokenStream.fill();
            List<Token> tokens = tokenStream.getTokens();
            tokens.stream().forEach(token -> {
                var text = token.getText();
                var name = CoolLexer.VOCABULARY.getSymbolicName(token.getType());
                
                System.out.println(text + " : " + name);
                //System.out.println(token);
            });
            */
            
            // Parser
            if (parser == null)
                parser = new CoolParser(tokenStream);
            else
                parser.setTokenStream(tokenStream);
            
            // Customized error listener, for including file names in error
            // messages.
            var errorListener = new BaseErrorListener() {
                public boolean errors = false;
                
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line, int charPositionInLine,
                                        String msg,
                                        RecognitionException e) {
                    String newMsg = "\"" + new File(fileName).getName() + "\", line " +
                                        line + ":" + (charPositionInLine + 1) + ", ";
                    
                    Token token = (Token)offendingSymbol;
                    if (token.getType() == CoolLexer.ERROR)
                        newMsg += "Lexical error: " + token.getText();
                    else
                        newMsg += "Syntax error: " + msg;
                    
                    System.err.println(newMsg);
                    errors = true;
                }
            };
            
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);
            
            // Actual parsing
            var tree = parser.program();
            if (globalTree == null)
                globalTree = tree;
            else
                // Add the current parse tree's children to the global tree.
                for (int i = 0; i < tree.getChildCount(); i++)
                    globalTree.addAnyChild(tree.getChild(i));
                    
            // Annotate class nodes with file names, to be used later
            // in semantic error messages.
            for (int i = 0; i < tree.getChildCount(); i++) {
                var child = tree.getChild(i);
                // The only ParserRuleContext children of the program node
                // are class nodes.
                if (child instanceof ParserRuleContext)
                    fileNames.put(child, fileName);
            }
            
            // Record any lexical or syntax errors.
            lexicalSyntaxErrors |= errorListener.errors;
        }

        // Stop before semantic analysis phase, in case errors occurred.
        if (lexicalSyntaxErrors) {
            System.err.println("Compilation halted");
            return;
        }

        // Visitor-ul de mai jos parcurge arborele de derivare ??i construie??te
        // un arbore de sintax?? abstract?? (AST).
        var astConstructionVisitor = new ASTConstructionVisitor();

        // ast este AST-ul proasp??t construit pe baza arborelui de derivare.
        var ast = astConstructionVisitor.visit(globalTree);

        // Visitor-ul de mai jos parcurge AST-ul ??i afi??eaz?? ??n consol?? structura programului.
        // var astPrinterVisitor = new ASTPrinterVisitor();

        // ast.accept(astPrinterVisitor);

        // Populate global scope.
        SymbolTable.defineBasicClasses();

        // ??n vederea gestiunii referirilor anticipate, utiliz??m mai multe treceri.
        Arrays.asList(
                // ??n prima trecere sunt definite toate simbolurile (clase, variabile, metode,
                // atribute, parametri etc.) ??i domeniile de vizibilitate.
                new ASTDefinitionPassVisitor(),

                // ??n a doua trecere se stabilesc tipurile simbolurilor conform tipurilor
                // specificate la declarare ??i se creeaz?? ierarhia de clase. Clasele derivate
                // vor avea ca scope p??rinte clasa de baz??. Clasele nederivate vor avea clasa
                // Object ca (scope) p??rinte.
                new ASTClassBindingPassVisitor(),

                // ??n a treia trecere se valideaz?? ierarhia claselor astfel ??nc??t s?? nu existe
                // dependen??e circulare ??i se valideaz?? suprascrierile.
                new ASTClassHierarchyValidationPassVisitor(),

                // ??n ultima trecere se verific?? tipurile expresiilor.
                new ASTResolutionPassVisitor()
        ).forEach(ast::accept);

        if (SymbolTable.hasSemanticErrors()) {
            System.err.println("Compilation halted");
            return;
        }

        var astCodeGenPassVisitor = new ASTCodeGenPassVisitor();
        var codeGen = ast.accept(astCodeGenPassVisitor);

        if (codeGen != null)
            System.out.println(codeGen.render());
    }
}
