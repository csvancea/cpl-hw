lexer grammar CoolLexer;

tokens { ERROR } 

@header{
    package cool.lexer;	
}

@members{
    private static final int MAX_STRING_LENGTH = 1024;

    private void raiseError(String msg) {
        setText(msg);
        setType(ERROR);
    }
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
 */
STRING  :   '"'
            (   '\\"'
            |   '\\' NEW_LINE
            |   '\u0000'    { raiseError("String contains null character"); }
            |   .
            )*?
            (   '"'         {
                                var text = getText();

                                text = text
                                        .replace("\\n", "\n")
                                        .replace("\\t", "\t")
                                        .replace("\\b", "\b")
                                        .replace("\\f", "\f")
                                        .replaceAll("\\\\(?!\\\\)", "");

                                text = text
                                        .substring(1, text.length() - 1);

                                if (text.length() > MAX_STRING_LENGTH) {
                                    raiseError("String constant too long");
                                } else if (getType() != ERROR) {
                                    setText(text);
                                }
                            }
            |   NEW_LINE    { raiseError("Unterminated string constant"); }
            |   EOF         { raiseError("EOF in string constant"); }
            );

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
      ('*)' { skip(); } | EOF { raiseError("EOF in comment"); })
    ;

BLOCK_COMMENT_UNMATCHED_START
    : '*)' { raiseError("Unmatched *)"); }
    ;

/* Spații albe.
 *
 * skip spune că nu este creat niciun token pentru lexemul depistat.
 */
WS
    :   [ \n\f\r\t]+ -> skip
    ;