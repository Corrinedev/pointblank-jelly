package mod.pbj.feature.math;

import com.eliotlash.mclib.math.IValue;

public abstract class ExtendedFunction implements IValue {
    protected IValue[] args;
    protected String name;

    public ExtendedFunction(IValue[] values, String name) throws Exception {
        if (values.length < this.getRequiredArguments()) {
            String message = String.format("Function '%s' requires at least %s arguments. %s are given!", this.getName(), this.getRequiredArguments(), values.length);
            throw new Exception(message);
        } else {
            this.args = values;
            this.name = name;
        }
    }

    public double getArg(int index) {
        return index >= 0 && index < this.args.length ? this.args[index].get() : (double)0.0F;
    }

    public String toString() {
        String args = "";

        for(int i = 0; i < this.args.length; ++i) {
            args = args + this.args[i].toString();
            if (i < this.args.length - 1) {
                args = args + ", ";
            }
        }

        return this.getName() + "(" + args + ")";
    }

    public String getName() {
        return this.name;
    }

    public int getRequiredArguments() {
        return 0;
    }
}
