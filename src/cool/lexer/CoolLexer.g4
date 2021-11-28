lexer grammar CoolLexer;

tokens { ERROR } 

@header{
    package cool.lexer;	
}

@members{    
    private void raiseError(String msg) {
        setText(msg);
        setType(ERROR);
    }

    private final StringBuilder buf = new StringBuilder();
}

fragment UPPERCASE : [A-Z];
fragment LOWERCASE : [a-z];
fragment LETTER : [a-zA-Z];
fragment DIGIT : [0-9];
fragment NEW_LINE : '\r'? '\n';

/* Reguli de funcționare:
 *
 * * Se ia în considerare cel mai lung lexem recunoscut, indiferent de ordinea
 *   regulilor din specificație (maximal munch).
 *
 * * Dacă există mai multe cele mai lungi lexeme, se ia în considerare prima
 *   regulă din specificație.
 */

/* Cuvânt cheie.
 */
IF : 'if';
THEN : 'then';
ELSE : 'else';
FI: 'fi';

BOOL : 'true' | 'false';
NOT : 'not';

CLASS : 'class';
INHERITS : 'inherits';

NEW : 'new';
ISVOID : 'isvoid';

LET : 'let';
IN : 'in';

WHILE : 'while';
LOOP : 'loop';
POOL : 'pool';

CASE : 'case';
ESAC : 'esac';
OF : 'of';

/* Identificator de tip.
 */
TYPE : UPPERCASE (LETTER | '_' | DIGIT)*;

/* Identificator de obiect.
 */
ID : LOWERCASE (LETTER | '_' | DIGIT)*;

/* Număr întreg.
 */
INT : DIGIT+;

/* Șir de caractere.
 *
 * Poate conține caracterul '"', doar precedat de backslash.
 * . reprezintă orice caracter în afară de EOF.
 * *? este operatorul non-greedy, care încarcă să consume caractere cât timp
 * nu a fost întâlnit caracterul ulterior, '"'.
 *
 * Acoladele de la final pot conține secvențe arbitrare de cod Java,
 * care vor fi executate la întâlnirea acestui token.
 *
 * Detalii: https://github.com/tunnelvisionlabs/antlr4/blob/master/doc/faq/lexical.md
 */
STRING  :   '"'
            (   '\\'
                (   'r'     { buf.append("\r"); }
                |   'n'     { buf.append("\n"); }
                |   't'     { buf.append("\t"); }
                |   'b'     { buf.append("\b"); }
                |   'f'     { buf.append("\f"); }
                |   '"'     { buf.append("\""); }
                |   '\\'    { buf.append("\\"); }
                |   .       { buf.append((char)getInputStream().LA(-1)); }
                )
            |   ~('\\' | '"') { buf.append((char)getInputStream().LA(-1)); }
            )*
            '"' { setText(buf.toString()); buf.setLength(0); }
        ;

/* Diferiți tokeni.
 */
SEMI : ';';
COLON : ':';
COMMA : ',';
DOT : '.';
AT : '@';

ASSIGN : '<-';
CAST : '=>';

/* Paranteze.
 */
LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
RBRACE : '}';

/* Operatori aritmetici.
 */
PLUS : '+';
MINUS : '-';
MULT : '*';
DIV : '/';
NEG : '~';

/* Operatori conditionali.
 */
EQUAL : '=';
LT : '<';
LE : '<=';

/* Comentarii.
 *
 * skip spune că nu este creat niciun token pentru lexemul depistat.
 */
LINE_COMMENT
    : '--' .*? (NEW_LINE | EOF) -> skip
    ;

BLOCK_COMMENT
    : '(*'
      (BLOCK_COMMENT | .)*?
      ('*)' | EOF { System.err.println("EOF in comment"); }) -> skip
    ;

/* Spații albe.
 *
 * skip spune că nu este creat niciun token pentru lexemul depistat.
 */
WS
    :   [ \n\f\r\t]+ -> skip
    ;