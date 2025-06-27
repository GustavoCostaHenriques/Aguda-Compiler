/* ----- PROJECT GRAMMAR PARSER ----- */

grammar Aguda;

program 
    : (declaration)+ EOF
    ;

declaration
    : functionTypeDeclaration
    | variableDeclaration
    ;

functionTypeDeclaration
    : LET ID LEFTPAREN idList RIGHTPAREN COLONTOKEN functionType EQUALTOKEN exprs
    ; 

variableDeclaration
    : LET ID COLONTOKEN typeElem EQUALTOKEN exprs
    ;

/* ----------- VARIABLES ------------ */

idList
    : ID (COMMA ID)*
    ;

/* ------------- TYPES -------------- */

basicType 
    : INTTYPE
    | BOOLTYPE
    | STRINGTYPE
    | UNITTYPE
    ;

arrayType
    : basicType (LEFTBRACKETS RIGHTBRACKETS)+
    ;

functionType
    : typeElem ARROW typeElem
    | LEFTPAREN typeList RIGHTPAREN ARROW typeElem
    ;


typeElem
    : basicType
    | arrayType
    ;

typeList
    : typeElem (COMMA typeElem)* 
    ;

/* ---------- EXPRESSIONS ----------- */

exprs
    : (expr SEMICOLON)* expr
    ;

exprsList
    : (blockexpression COMMA)*  blockexpression
    ;

expr
    : setexpression
    | ifexpression
    | whileexpression
    | arraycreationexpression
    | logicalexpression
    | letexpressions
    ;

// Logical and Arithmetic expressions
logicalexpression
    : logicalexpression op=(AND|OR) logical            
    | logical                                           
    ;

logical
    : logical op=(EQ|NEQ|LT|LE|GT|GE) unaryLogicalExpr
    | unaryLogicalExpr
    ;

unaryLogicalExpr
    : NOT unaryLogicalExpr
    | arithmeticExpr
    ;

arithmeticExpr
    : arithmeticExpr op=(PLUS|MINUS) term                  
    | term                                              
    ;

term
    : term op=(MUL|DIV|MOD) factor                      
    | factor                                            
    ;

factor
    : atom (POW factor)?
    ;

atom
    : primary (LEFTBRACKETS blockexpression RIGHTBRACKETS)*
    ;

primary
    : INT 
    | MINUS primary 
    | ID 
    | ID LEFTPAREN exprsList RIGHTPAREN                                         
    | NULL                                              
    | BOOLEAN                                           
    | STRING 
    | LEFTPAREN blockexpression RIGHTPAREN
    ;

// Set expressions
setexpression
    : SET lefthandside EQUALTOKEN expr
    ;

lefthandside
    : ID (LEFTBRACKETS blockexpression RIGHTBRACKETS)*
    | primary (LEFTBRACKETS blockexpression RIGHTBRACKETS)+
    ;

// If expressions
ifexpression
    : IF expr THEN expr (ELSE expr)?
    ;

// While expressions
whileexpression
    : WHILE expr DO expr
    ;

// Array creation expressions
arraycreationexpression
    : NEW basicType (LEFTBRACKETS (blockexpression PIPE blockexpression)?  RIGHTBRACKETS)+
    ;

// Let expressions
letexpressions
    : LET ID COLONTOKEN typeElem EQUALTOKEN expr
    ;

// Block expressions
blockexpression
    : (expr SEMICOLON)* expr
    ;

/* ------------- LEXER -------------- */

// KeyWords
LET        : 'let' ;
INTTYPE    : 'Int' ;
BOOLTYPE   : 'Bool' ;
STRINGTYPE : 'String' ;
UNITTYPE   : 'Unit' ;
SET        : 'set' ;
IF         : 'if' ;
THEN       : 'then' ;
ELSE       : 'else' ;
WHILE      : 'while' ;
DO         : 'do' ;
NEW        : 'new' ;

// Symbols
COLONTOKEN     : ':' ;
EQUALTOKEN     : '=' ;
LEFTBRACKETS   : '[' ;
RIGHTBRACKETS  : ']' ;
ARROW          : '->' ;
LEFTPAREN      : '(' ;
RIGHTPAREN     : ')' ;
COMMA          : ',' ;
SEMICOLON      : ';' ;
PIPE           : '|' ;

// Operators
NOT : '!' ;
AND : '&&' ;
OR  : '||' ;
EQ  : '==' ;
NEQ : '!=' ;
LT  : '<' ;
LE  : '<=' ; 
GT  : '>' ;
GE  : '>=' ;
PLUS  : '+' ;
MINUS : '-' ;
MUL   : '*' ;
DIV   : '/' ;
MOD   : '%' ;
POW   : '^' ;

// Identifiers, integers, string, etc.
NULL : 'unit' ;
BOOLEAN : 'true' | 'false' ;
ID : [a-zA-Z_][a-zA-Z0-9'_]* ;
INT : [1-9][0-9]*|[0] ;
STRING : '"' (~["\r\n])* '"' ;

// Comments and things to ignore
WS : [ \t\r\n]+ -> skip ;
COMMENT : '--' ~[\r\n]* -> skip ;