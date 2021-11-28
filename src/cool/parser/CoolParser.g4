parser grammar CoolParser;

options {
    tokenVocab = CoolLexer;
}

@header{
    package cool.parser;
}

program
    : (classes+=class_ SEMI)+ EOF
    ; 

class_
    : CLASS type=TYPE (INHERITS super_=TYPE)? LBRACE (features+=feature SEMI)* RBRACE
    ;

feature
    : name=ID LPAREN (formals+=formal (COMMA formals+=formal)*)? RPAREN COLON type=TYPE LBRACE body=expr RBRACE         # methodDef
    | variable                                                                                                          # attributeDef
    ;

formal
    : name=ID COLON type=TYPE
    ;

variable
    : name=ID COLON type=TYPE (ASSIGN init=expr)?
    ;

caseTest
    : name=ID COLON type=TYPE CAST body=expr SEMI
    ;

expr
    : instance=expr (AT type=TYPE)? DOT name=ID LPAREN (args+=expr (COMMA args+=expr)*)? RPAREN                         # explicitDispatch
    | name=ID LPAREN (args+=expr (COMMA args+=expr)*)? RPAREN                                                           # implicitDispatch
    | IF cond=expr THEN thenBranch=expr ELSE elseBranch=expr FI                                                         # if
    | WHILE cond=expr LOOP body=expr POOL                                                                               # while
    | LBRACE (exprs+=expr SEMI)+ RBRACE                                                                                 # block
    | LET vars+=variable (vars+=variable)* IN body=expr                                                                 # let
    | CASE instance=expr OF (cases+=caseTest)+ ESAC                                                                     # case
    | NEW type=TYPE                                                                                                     # new
    | ISVOID instance=expr                                                                                              # isVoid
    | left=expr op=(MULT | DIV) right=expr                                                                              # multDiv
    | left=expr op=(PLUS | MINUS) right=expr                                                                            # plusMinus
    | NEG e=expr                                                                                                        # negate
    | left=expr op=(LT | LE | EQUAL) right=expr                                                                         # relational
    | NOT e=expr                                                                                                        # not
    | LPAREN e=expr RPAREN                                                                                              # paren
    | name=ID ASSIGN e=expr                                                                                             # assign
    | ID                                                                                                                # id
    | INT                                                                                                               # int
    | STRING                                                                                                            # string
    | BOOL                                                                                                              # bool
    ;
