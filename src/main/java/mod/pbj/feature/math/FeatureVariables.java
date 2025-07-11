package mod.pbj.feature.math;

import com.eliotlash.mclib.math.*;
import com.eliotlash.mclib.math.functions.Function;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import mod.pbj.PointBlankJelly;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureVariables extends MathBuilder {
    public static FeatureVariables GLOBAL;
    public Map<String, Class<? extends ExtendedFunction>> extendedFunctions = new HashMap();

    public FeatureVariables(Variable... variables) {
        for (Variable variable : variables)
            register(variable);
    }

    public static void createGlobal() {
        GLOBAL = new FeatureVariables();
    }

    public static Variable[] createDefaults(String... names) {
        Variable[] returns = new Variable[names.length];
        int i = 0;
        for(String str : names) {
            returns[i] = new Variable(str, 0.0);
            i++;
        }
        return returns;
    }

    public double getParsedElement(JsonElement element) {
        if(element.isJsonPrimitive()) {
            JsonPrimitive primitive = (JsonPrimitive) element;
            if(primitive.isNumber()) {
                return element.getAsJsonPrimitive().getAsDouble();
            }
            if(primitive.isString()) {
                try {
                    return parse(primitive.getAsString()).get();
                } catch (Exception e) {
                    PointBlankJelly.LOGGER.warn("Parsing of math expression failed! Reason = {}", e.getMessage());
                }
            }
        }
        return 0.0;
    }

    public void set(String name, double value) {
        this.getVariable(name).set(value);
    }

    @Override
    public String[] breakdown(String expression) throws Exception {
        if (!expression.matches("^[\\w\\d\\s_+-/*%^&|<>=!?:.,()\"\"]+$")) {
            throw new Exception("Given expression '" + expression + "' contains illegal characters!");
        } else {
            expression = expression.replaceAll("\\s+", "");
            String[] chars = expression.split("(?!^)");
            int left = 0;
            int right = 0;

            for(String s : chars) {
                if (s.equals("(")) {
                    ++left;
                } else if (s.equals(")")) {
                    ++right;
                }
            }

            if (left != right) {
                throw new Exception("Given expression '" + expression + "' has more uneven amount of parenthesis, there are " + left + " open and " + right + " closed!");
            } else {
                return chars;
            }
        }
    }

    @Override
    protected IValue createFunction(String first, List<Object> args) throws Exception {
        if (first.equals("!")) {
            return new Negate(this.parseSymbols(args));
        } else if (first.startsWith("!") && first.length() > 1) {
            return new Negate(this.createFunction(first.substring(1), args));
        } else if (first.equals("-")) {
            return new Negative(new Group(this.parseSymbols(args)));
        } else if (first.startsWith("-") && first.length() > 1) {
            return new Negative(this.createFunction(first.substring(1), args));
        } else if (!this.functions.containsKey(first) && !this.extendedFunctions.containsKey(first)) {
            throw new Exception("Function '" + first + "' couldn't be found!");
        } else {
            List<IValue> values = new ArrayList();
            List<Object> buffer = new ArrayList();

            for(Object o : args) {
                if (o.equals(",")) {
                    values.add(this.parseSymbols(buffer));
                    buffer.clear();
                } else {
                    buffer.add(o);
                }
            }

            if (!buffer.isEmpty()) {
                values.add(this.parseSymbols(buffer));
            }

            if(functions.get(first) != null) {
                Class<? extends Function> function = this.functions.get(first);
                Constructor<? extends Function> ctor = function.getConstructor(IValue[].class, String.class);
                return ctor.newInstance(values.toArray(new IValue[values.size()]), first);
            }

            Class<? extends ExtendedFunction> function = this.extendedFunctions.get(first);
            Constructor<? extends ExtendedFunction> ctor = function.getConstructor(IValue[].class, String.class);

            return ctor.newInstance(values.toArray(new IValue[values.size()]), first);
        }
    }
}
