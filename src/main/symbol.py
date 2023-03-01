
class Symbol():
    def __init__(self, name, symb_type):
        self.name = name
        self.symb_type = symb_type  # return type in case of funcs


class Function(Symbol):
    def __init__(self, name, symb_type, label, parameters=[]):
        super().__init__(name, symb_type)
        self.label = label
        self.parameters = parameters  # array of <Var>

    def addParam(self, var):
        self.parameters(var)
        
    def __repr__(self):
    	
    	args = ", ".join([f"{var.symb_type} {var.name}" for var in self.parameters])
    	return f"{self.symb_type} {self.name} ({args}) =  {self.label}"
    	


class Var(Symbol):
    def __init__(self, name, symb_type, label):
        super().__init__(name, symb_type)
        self.label = label

        self.addr = None
        # self.assigned_depth = None
    
    def __repr__(self):
    	return f"{self.symb_type} {self.name} =  {self.label}"

# =======================================================================


class Scope():

    def __init__(self, r_type, depth):
        self.r_type = r_type
        self.depth = depth
        self.frame_size = 0  # number of declared vars
        self.parentScope = None
        self.locals = dict()  # map <String, Symbol>

    def __repr__(self):
        for name in self.locals:
            symbol = self.locals[name]

            for i in range(0, self.depth):
            	print("    ", end="")
           	
            print(symbol)

        #print(name + ": ", str(var.addr))
       
       	return "" # useless

    def isIdentifierUnique(self, new_name):
        for name in self.locals:
            if (new_name == name):
                print("Error: Identifier '"+new_name + "' is already in use")
                return False
        return True

    def addSymbol(self, symbol):

        if not self.isIdentifierUnique(symbol.name):
            return

        self.locals[symbol.name] = symbol

        if (isinstance(symbol, Var)):
            self.frame_size += 1
            symbol.addr = self.frame_size

    def findSymbol(self, find_name):
        for name in self.locals:
            if (name == find_name):
                return self.locals[name]

        return None


# =======================================================================


class SymbolTable():
    def __init__(self):
        self.curr_scope = None
        self.curr_scope_depth = 0

    def __repr__(self):
        print(" ======= Symbol Table =======\n")
        scope = self.curr_scope

        while (scope != None):
            print(scope)
            scope = scope.parentScope
            
        return "" # useless

    def newScope(self, scope_type):
        self.curr_scope_depth += 1
        new_scope = Scope(scope_type, self.curr_scope_depth)
        new_scope.parentScope = self.curr_scope
        self.curr_scope = new_scope

    def delScope(self):
        self.curr_scope_depth -= 1
        self.curr_scope = curr_scope.parentScope

    def addFunc(self, function_obj):
        self.curr_scope.addSymbol(function_obj)

    def addVar(self, var_obj):
        self.curr_scope.addSymbol(var_obj)

    def findSymbol(name):
        scope = self.curr_scope

        # Bubble up scope trying to find identifier
        while (scope != None):
            symbol = scope.findSymbol(name)
            if (symbol != None):
                return symbol
            scope = scope.parentScope

        print("Error: Identifier '"+name+"' is not declared!")
        return None

# =======================================================================


table = SymbolTable()

v = Var('var1', 'String', '"Hello World!"')
f = Function('func1', 'array<int>', "func_label?", [v,v])


table.newScope("if_block")
table.addVar(v)
table.newScope("else_block")
table.addFunc(f)
table.addVar(v)
table.newScope("else_block")
table.addFunc(f)
table.newScope("else_block")
table.addVar(v)

print(table)


# table.addFunc("funcao1", "int", "funcao1(int a, char b)")
