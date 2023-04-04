grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

// PARSER

program
    : (importDeclaration)* classDeclaration EOF #ProgramRoot
    ;

importDeclaration
    : 'import' importPackage ('.' importPackage)* ';' #ImportDecl
    ;

importPackage
    : packageID=ID
    ;

classDeclaration
    : 'class' name=ID ('extends' superName=ID)? '{' (varDeclaration) *  ( methodDeclaration )* '}' #ClassDecl
    ;

varDeclaration
    : type var=ID';' #varDcl
    ;


methodDeclaration
    : ('public')? 'static' 'void' 'main' '(' type '[' ']' arg=ID ')' '{' (varDeclaration)* (statement)* '}' #MainMethod
    | ('public')? type name=ID '(' methodParams? ')' '{' (varDeclaration)* (statement)*  returnStatement '}' #MethodDecl
    ;

methodParams
    : paramDeclaration (',' paramDeclaration)* #MethodArgs
    ;

paramDeclaration
    : type var=ID #ParamDecl
    ;

returnStatement
    : 'return' expression ';' #ReturnStmt
    ;


type
    : 'int' '[' ']' #IntArrayType
    | 'boolean' #BooleanType
    | 'int' #IntType
    | name=ID #IdType
    ;

then_block : statement #ThenBlock
    ;

else_block : statement #ElseBlock
    ;

while_block : statement #WhileBlock
    ;

statement
    : '{' (statement)* '}' #Scope
    | 'if' '(' expression ')' then_block 'else' else_block #IfClause
    | 'while' '(' expression ')' while_block #While
    | expression ';' #ExpressionStmt
    | var=ID '=' expression ';' #Assign
    | var=ID '[' expression ']' '=' expression ';' #ArrayAssign
    ;

expression
    : '(' expression ')' #Paren
    | expression '[' expression ']' #ArrayLookup
    | var=ID '[' expression ']' #ArrayLookup
    | expression '.' atribute=ID #AtributeAccess
    | expression '.' method=ID '(' (expression (',' expression)*)? ')' #MethodCall
    | '!' expression #Not
    | 'new' 'int' '[' expression ']' #NewIntArray // TODO
    | 'new' objClass=ID '(' ')' #NewObject
    | expression op=('*'| '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op='<' expression #BinaryComp
    | expression op='&&' expression #BinaryBool
    | 'this' #This
    | var=ID #Var
    | val=BooleanLiteral #Boolean
    | val=INTEGER #Int
    ;

// LEXER


IMPORT: 'import' ;
CLASS: 'class' ;
PUBLIC: 'public' ;
STATIC: 'static' ;
VOID: 'void' ;
RETURN: 'return' ;
NEW: 'new' ;
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
INTEGER: [0] | [1-9][0-9]*;

WS : [ \t\n\r\f]+ -> skip ;
COMMENT: '/*' .*? '*/'    -> skip;
LINE_COMMENT:  '//' ~[\r\n]*  -> skip;

