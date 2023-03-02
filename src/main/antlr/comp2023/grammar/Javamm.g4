grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

type
   : 'boolean'
   | 'int'
   | 'int' '[' ']'
   | ID
   ;

fragment
BOOL
    : 'true'
    | 'false'
    ;

LITERAL
    : BOOL
    | INTEGER
    | 'null'
    ;


INTEGER: ('-')? [0-9]+;

ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;
COMMENT: '/*' .*? '*/'    -> skip;
LINE_COMMENT:  '//' ~[\r\n]*  -> skip;


program
    : importDeclaration* classDeclaration EOF #ProgramRoot
    ;

importDeclaration
    : 'import' classID=ID ('.' methodID=ID)* ';' #ImportDecl // doesnt support *
    ;

classDeclaration
    : 'class' name=ID ('extends' superName=ID)? '{' (varDeclaration | methodDeclaration )* '}' #ClassDecl
    ;

varDeclaration
    : type ('[' ']')? var=ID ('=' expression)? ';' // TODO ... ver este com mais cuidado, o input.txt n dá mas está a reconhecer no teste que criei
    ;

methodDeclaration
    : ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' ID ')' '{' methodBody '}' #MainMethodDecl
    | ('public')?  ( type | 'void') name=ID '(' methodParams? ')' '{' methodBody '}' #MethodDecl
    ;

methodParams
    : argType=type arg=ID (',' argType=type arg=ID)* #MethodParamsDecl
    ;

methodArgs
    : expression (',' expression)* #MethodArgsDecl
    ;

methodBody
    : varDeclaration* statement*
    ;
// O NOME DE CLASS é sempre com maiuscula ???
statement
    : '{' (statement)* '}' #BlockStmt
    | 'System.out.println' '(' expression ')' ';' #PrintStmt
    | 'if' '(' expression ')' statement elseIfStmt* elseStmt? #IfStmt
    | 'while' '(' expression ')' statement #WhileStmt
    | expression ';' #ExprStmt

    // VER DPS
    | type arrayCall var=ID '=' array_struct ';' #ArrayDeclAssign
    | type arrayCall? var=ID ';' #VarDecl // TODO: ESTE PEDAÇO ESTA REPETIDO ... o [] estava no sitio errado ... embora dei nos dois em java (os testes falhavam) ... perguntar ao professor
    | type var=ID '=' expression ';' #VarDeclAssign
    | var=ID arrayCall? '=' expression ';' #VarAssign


    // | var=ID '.' methodCall=ID '(' methodArgs* ')' ';' #MethodCall
    // | functionCall=ID '(' methodArgs* ')' ';' #FunctionCall

    // | 'break' ';' #BreakStmt
    // | 'continue' ';' #ContinueStmt
      | 'return' expression? ';' #ReturnStmt
    // | expression op=('++' | '--') ';' #UnaryOpStmt
    ;

arrayCall:('['expression?']')+;

array_struct
    : '{' (expression (',' expression)*)? '}' #ArrayStruct
    ;


elseIfStmt:('else' 'if' '(' expression ')' statement);
elseStmt:('else' statement);


expression
    : var=ID ('.' methodCall=ID )* #ObjectVar
    | '(' expression ')' #ParenExpr
    | '!' expression #UnaryNegation
    | expression op=('*'| '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression '&&' expression #BinaryBool
    | expression '<' expression #BinaryComp
    | value=LITERAL #LiteralExpr
    | var=ID '[' expression ']' #ArrayAccess
    | 'new' 'int' arrayCall #NewArrayExpr
    | 'new' ID '(' ')' #NewObjectExpr
    | 'this' #ThisExpr
    | var=ID #VarExpr
    // | array_struct #ArrayStructExpr // // adding this makes h = {1,5,6}; possible but allows for other weird stuff
    ;


// known problems
// - doesnt support a.b.c.d() calls --> only single calls method chain can be performed due to stuff (review later if needed)
// -- multiple layer imports are also busted like the above bulletpoint
