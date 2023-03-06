package pt.up.fe.comp2023.Visitor;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.function.BiFunction;

public class MyNodeVisitor extends AJmmVisitor<String, String> {

    private String className;

    public MyNodeVisitor(String className) {
        this.className = className;
    }

    @Override
    public String visit(JmmNode jmmNode, String s) {
        return super.visit(jmmNode);
    }

    @Override
    protected void buildVisitor() {
        addVisit("Class", this::dealWithClass);
    }

    public String dealWithClass(JmmNode jmmNode, String s) {
        return s + " Class ";
    }

    @Override
    public void addVisit(String s, BiFunction<JmmNode, String, String> biFunction) {
        super.addVisit(s, biFunction);
    }


    public String setDefaultVisit(JmmNode jmmNode, String s) {
        return s + " Error ";
    }


}
