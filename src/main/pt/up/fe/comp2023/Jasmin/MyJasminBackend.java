package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;


// nao e pra implementar ifs, loops e arrays
public class MyJasminBackend implements JasminBackend {

    ClassUnit classe;
    String code = "";
    MySymbolTable st;

    public MyJasminBackend(MySymbolTable st){
        this.st = st;
    }

    private String toJasminType(String type){
        switch (type) {
            case "VOID" -> {
                return "V";
            }
            case "INT32" -> {
                return "I";
            }
            case "BOOLEAN" -> {
                return "Z";
            }
            case "ARRAYREF" -> {
                return "[I";
            }
            case "OBJECTREF" -> {
                return "Ljava/lang/Object;";
            }
            default -> {
                return "TypeError("+type+")";
            }
        }
    }

    private void showClass(){
        System.out.println("\n<CLASS>");
        System.out.println("\tNAME: " + this.classe.getClassName());
        System.out.println("\tEXTENDS: " + this.classe.getSuperClass());
        System.out.println("\tFIELDS: " + this.classe.getFields());
        System.out.println("<END CLASS/>\n");

        this.classe.getMethods().forEach(method -> {
            System.out.println("\n<METHOD>");
            System.out.println("\tMETHOD: " + method.getMethodName());
            System.out.println("\tPARAMS: " + method.getParams());
            System.out.println("\tRETURN TYPE: " + method.getReturnType());
            System.out.println("<END METHOD/>");
        });
    }

    private String addHeaders() {
        code += ".class public " + this.classe.getClassName() + "\n";
        if (this.classe.getSuperClass() != null)
            code += ".super " + this.classe.getSuperClass() + "\n";
        else{
            code += ".super java/lang/Object\n";
        }
        return code;
    }

    public String addFields() {
        StringBuilder codeBuilder = new StringBuilder();
        this.classe.getFields().forEach(field -> {
            codeBuilder.append("\n\n.field private ").append(field.getFieldName()).append(" ").append(toJasminType(field.getFieldType().toString())).append("\n");
        });
        return codeBuilder.toString();
    }

    public String addConstructor() {
        StringBuilder codeBuilder = new StringBuilder();
        codeBuilder.append("\n\n.method public <init>()V\n");
        codeBuilder.append("\taload_0\n");
        codeBuilder.append("\tinvokenonvirtual java/lang/Object/<init>()V\n");
        codeBuilder.append("\treturn\n");
        codeBuilder.append(".end method\n\n");
        return codeBuilder.toString();
    }

    private String addMethods() {
        StringBuilder codeBuilder = new StringBuilder();
        this.classe.getMethods().forEach(method -> {

            if (method.getMethodName().equals("main"))
                codeBuilder.append("\n\n.method public static main([Ljava/lang/String;)V\n");
            else
                codeBuilder.append("\n\n.method public ").append(method.getMethodName()).append("(");


            method.getParams().forEach(param -> {
                codeBuilder.append(toJasminType(param.getType().toString()));
            });


            String returnType = method.getReturnType().toString();
            codeBuilder.append(")").append(toJasminType(returnType)).append("\n");



            // in this phase we don't need to worry about locals and stack limits
            codeBuilder.append("\t.limit locals 99;\n");
            codeBuilder.append("\t.limit stack 99;\n");



            codeBuilder.append("\t...CODE...\n");



            if (returnType.equals("VOID")) {
                // void
                codeBuilder.append("\treturn\n");
            } else if (returnType.equals("INT32") || returnType.equals("BOOLEAN")) {
                // int
                codeBuilder.append("\tireturn\n");
            } else {
                // int array
                codeBuilder.append("\tareturn\n");
            }



            codeBuilder.append(".end method\n\n");
        });
        return codeBuilder.toString();
    }

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        this.classe = ollirResult.getOllirClass();
        //this.showClass(); // debug print class


        code+= this.addHeaders();
        code += this.addFields();
        code += this.addConstructor();
        code += this.addMethods();

        System.out.println("\n\n======================JASMIN CODE======================\n\n");
        System.out.println(code);
        System.out.println("======================================================");



        return new JasminResult("""
                    .class public HelloWorld
                    .super java/lang/Object

                    ;
                    ; standard initializer (calls java.lang.Object's initializer)
                    ;
                    .method public <init>()V
                       aload_0
                       invokenonvirtual java/lang/Object/<init>()V
                       return
                    .end method

                    ;
                    ; main() - prints out Hello World
                    ;
                    .method public static main([Ljava/lang/String;)V
                       .limit stack 2   ; up to two items can be pushed

                       ; push System.out onto the stack
                       getstatic java/lang/System/out Ljava/io/PrintStream;

                       ; push a string onto the stack
                       ldc "120"

                       ; call the PrintStream.println() method.
                       invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

                       ; push another string onto the stack
                       ; ldc "Hello World Again!"

                       ; call the io.println() method.
                       ; invokestatic io/println(Ljava/lang/String;)V

                       ; done
                       return
                    .end method
                   \s
                     .method public foo(II)V
                     \t.limit locals 3 ; This is the minimum value, one for "this" and two for the arguments
                     \treturn
                     .end method\
                """);
    }
}
