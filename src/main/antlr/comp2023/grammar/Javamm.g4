grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

// PARSER

//
// Notas: queria dar um nome ao returno da função
//

// A melhorar:
// 1. Ver o tipo de retorno identificado .. [0]
// melhor nome aos objetos nos em method calls
// talvez o mesmo de cima mas para argumentos
// no if else n sei se valerá a pena


// ou deixar como está e perguntar ao professor o que acha

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
    : type var=ID';'
    ;


methodDeclaration returns [String returnType, String methodName]:
    ('public')? 'static' 'void' 'main' '(' type '[' ']' arg=ID ')' '{' (varDeclaration)* (statement)* '}'
    {
        $returnType = "void";
        $methodName = "main";
    }
    | ('public')? type name=ID '(' methodParams? ')' '{' (varDeclaration)* (statement)*  returnStatement '}'
    {
        $returnType = $type.text;
        $methodName = $name.text;
    }
    ; // TODO: ver se funcionou o tipo de retorno

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

statement
    : '{' (statement)* '}' #Scope
    | 'if' '(' expression ')' statement ('else' statement)? #If
    | 'while' '(' expression ')' statement #While
    | expression ';' #ExpressionStmt
    | var=ID '=' expression ';' #Assign
    | var=ID '[' expression ']' '=' expression ';' #ArrayAssign
    ;

expression
    : '(' expression ')' #Paren
    | value=BooleanLiteral #Boolean
    | 'new' 'int' '[' expression ']' #NewIntArray
    | 'new' objClass=ID '(' ')' #NewObject
    | '!' expression #Not
    | expression '[' expression ']' #ArrayLookup
    | expression '.' 'length' #ArrayLength
 //   | expression '.' 'this' '(' (expression (',' expression)*)? ')' #MethodCall // TODO... é para tirar ?
    | expression '.' method=ID '(' (expression (',' expression)*)? ')' #MethodCall // TODO ... é possivel dar um node ao objeto?
    | expression op=('*'| '/') expression #BinaryOp  // nota ... a assocividade é importante
    | expression op=('+' | '-') expression #BinaryOp
    | expression op='&&' expression #BinaryBool
    | expression op='<' expression #BinaryComp
    | 'this' #This
    | var=ID #Var
    | var=ID '[' expression ']' #ArrayLookup
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
