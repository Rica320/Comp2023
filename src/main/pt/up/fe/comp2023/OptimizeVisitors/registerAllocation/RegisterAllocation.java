package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.ClassUnit;

public class RegisterAllocation {

    // https://www.cs.purdue.edu/homes/hosking/502/notes/08-reg.pdf

    private ClassUnit classUnit;

    public RegisterAllocation(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public void liveliness() {

        classUnit.getMethods().forEach(method -> {

        });


    }
}
