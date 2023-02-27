grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

TYPE
   : 'bool'
   | 'int'
   | 'char'
   | 'string'
   | 'float'
   ;

MethodVisibility
    : 'public'
    | 'private'
    | 'protected'
    ;

BOOL
    : 'true'
    | 'false'
    ;

LITERAL
    : BOOL
    | INTEGER
    | FLOAT
    | STRING
    | CHAR
    | 'null'
    ;


INTEGER: ('-')? [0-9]+;
FLOAT: INTEGER '.' [0-9]+;

ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;
CHAR : '\'' [a-zA-Z] '\'' ;
STRING : '"' ('\\' ["\\] | ~["\\\r\n])* '"' ;


WS : [ \t\n\r\f]+ -> skip ;
COMMENT: '/*' .*? '*/'    -> skip;
LINE_COMMENT:  '//' ~[\r\n]*  -> skip;


program
    : importDeclaration* classDeclaration EOF #ProgramRoot
    ;

importDeclaration
    : 'import' classID=ID ('.' methodID=ID)+ ';' #ImportDecl // doesnt support *
    ;

classDeclaration
    : 'class' name=ID ('extends' superName=ID)? '{' varDeclaration*  methodDeclaration* '}' #ClassDecl
    ;

varDeclaration
    : type=TYPE var=ID ('=' expression)? ';'
    ;

methodDeclaration
    : 'public' 'static' 'void' 'main' '(' 'String' '[' ']' ID ')' '{' methodBody '}' #MainMethodDecl
    | visibility=MethodVisibility (isStatic='static')? returnType=( TYPE | 'void') name=ID '(' methodParams? ')' '{' methodBody '}' #MethodDecl
    ;

methodParams
    : argType=TYPE arg=ID (',' argType=TYPE arg=ID)* #MethodParamsDecl
    ;

methodArgs
    : expression (',' expression)* #MethodArgsDecl
    ;

methodBody
    : varDeclaration* statement*
    ;

statement
    : '{' (statement)* '}' #BlockStmt
    | 'System.out.println' '(' expression ')' ';' #PrintStmt
    | 'if' '(' expression ')' statement elseIfStmt* elseStmt? #IfStmt
    | 'while' '(' expression ')' statement #WhileStmt
    | 'for' '(' ( (var=ID '=' expression) | (type=TYPE var=ID '=' expression) )  ';' expression ';' expression ')' statement #ForStmt

    | type=TYPE var=ID arrayCall '=' array_struct ';' #ArrayDeclAssign
    | type=TYPE var=ID arrayCall? ';' #VarDecl
    | type=TYPE var=ID '=' expression ';' #VarDeclAssign
    | var=ID arrayCall? '=' expression ';' #VarAssign



    | var=ID '.' methodCall=ID '(' methodArgs* ')' ';' #MethodCall
    | functionCall=ID '(' methodArgs* ')' ';' #FunctionCall

    | 'break' ';' #BreakStmt
    | 'continue' ';' #ContinueStmt
    | 'return' expression? ';' #ReturnStmt
    | expression op=('++' | '--') ';' #UnaryOpStmt
    ;

arrayCall:('['expression?']')+;

array_struct
    : '{' (expression (',' expression)*)? '}' #ArrayStruct
    ;


elseIfStmt:('else' 'if' '(' expression ')' statement);
elseStmt:('else' statement);


expression
    : '(' expression ')' #ParenExpr
    | '!' expression #UnaryNegation
    | expression op=('++' | '--') #UnaryOp
    | expression op=('*'| '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('&&' | '||') expression #BinaryBool
    | expression op=('==' | '!=' | '<' | '<=' | '>' | '>=') expression #BinaryComp
    | var=ID '[' expression ']' #ArrayAccess
    | value=LITERAL #LiteralExpr
    | var=ID #VarExpr
    // | array_struct #ArrayStructExpr // // adding this makes h = {1,5,6}; possible but allows for other weird stuff
    ;


// known problems
// - doesnt support a.b.c.d() calls --> only single calls method chain can be performed due to stuff (review later if needed)
// -- multiple layer imports are also busted like the above bulletpoint
