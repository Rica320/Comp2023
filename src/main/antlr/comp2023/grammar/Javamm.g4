grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

// PARSER


program
    : (importDeclaration)* classDeclaration EOF #ProgramRoot
    ;

importDeclaration
    : 'import' packageID=ID ('.' packageID=ID)* ';' #ImportDecl
    ;

classDeclaration
    : 'class' name=ID ('extends' superName=ID)? '{' (varDeclaration) *  ( methodDeclaration )* '}' #ClassDecl
    ;

varDeclaration
    : type ('[' ']')? var=ID';'
    ;


methodDeclaration // aqui acontece a questão da 'String', isto resolve e acho que tinha sido o que o stor disse ao joão
    : ('public')? 'static' 'void' 'main' '(' type '[' ']' arg=ID ')' '{' (varDeclaration)* (statement)* '}' #MainMethodDecl
    | ('public')?  type name=ID '(' (type arg=ID (',' type arg=ID)*)?')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}' #MethodDecl // se não me engano o retorno n pode ser void
    ;

type
    : 'int' '[' ']' #IntArrayType
    | 'boolean' #BooleanType
    | 'int' #IntType
    | name=ID #IdType
    ;

statement
    : '{' (statement)* '}' #Block
    | 'if' '(' expression ')' statement ('else' statement)? #If
    | 'while' '(' expression ')' statement #While
    | expression ';' #ExpressionStmt
    | var=ID '=' expression ';' #Assign
    | var=ID '[' expression ']' '=' expression ';' #ArrayAssign
    ;

expression
    : '(' expression ')' #Paren
    | BooleanLiteral #Boolean
    | 'new' 'int' '[' expression ']' #NewIntArray
    | 'new' ID '(' ')' #NewObject
    | '!' expression #Not
    | expression '[' expression ']' #ArrayLookup
    | expression '.' 'length' #ArrayLength
    | expression '.' 'this' '(' (expression (',' expression)*)? ')' #MethodCall
    | expression '.' ID '(' (expression (',' expression)*)? ')' #MethodCall
    | expression op=('*'| '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression '&&' expression #BinaryBool
    | expression '<' expression #BinaryComp
    | 'this' #This
    | var=ID #Var
    | var=ID '[' expression ']' #ArrayLookup
    | INTEGER #Int
    ;

// LEXER


IMPORT: 'import' ;
CLASS: 'class' ;
PUBLIC: 'public' ;
STATIC: 'static' ;
VOID: 'void' ;
RETURN: 'return' ;
NEW: 'new' ;
LENGTH: 'length' ;
THIS: 'this' ;

BooleanLiteral
    : 'true'
    | 'false'
    ;

BOOLEAN: 'boolean' ;
INT: 'int' ;
IF: 'if' ;
ELSE: 'else' ;
WHILE: 'while' ;
ASSIGN: '=' ;


LPAREN: '(' ;
RPAREN: ')' ;
LBRACE: '{';
RBRACE: '}';
LBRACK: '[';
RBRACK: ']';
COMMA: ',';
DOT: '.' ;
SEMI: ';' ;

// Operadores
BANG: '!' ;
PLUS: '+' ;
MINUS: '-' ;
TIMES: '*' ;
DIV: '/' ;
AND: '&&' ;
LT: '<' ;


ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;
INTEGER:  ('-')? [0-9]+; // o '-' é opcional ??????

WS : [ \t\n\r\f]+ -> skip ;
COMMENT: '/*' .*? '*/'    -> skip;
LINE_COMMENT:  '//' ~[\r\n]*  -> skip;

// o Any pode ser usado para apanhar erros ... um dos pontos do checkpoint
// ANY: . ;
