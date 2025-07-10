package mod.pbj.script;

public class PBJImporter {
	//    {
	//        initStandardObjects(shell(), false);
	//        for(Class<?> cl : JLib.DEFAULT_IMPORTS) {
	//            importClass(this, new NativeJavaClass(this, cl));
	//        }
	//        put("std", this, new NativeJavaClass(this, JLib.class));
	//
	//        for(Method method : JLib.class.getDeclaredMethods()) {
	//            put(method.getName(), this, new NativeJavaMethod(method, method.getName()));
	//        }
	//    }
	//
	//    public static void importClass(Scriptable scope, NativeJavaClass cl) {
	//        String s = cl.getClassObject().getName();
	//        String n = s.substring(s.lastIndexOf('.') + 1);
	//        Object val = scope.get(n, scope);
	//        if (val != NOT_FOUND) {
	//            if (val.equals(cl)) {
	//                return; // do not redefine same class
	//            }
	//            throw Context.reportRuntimeError("PBJImporter: class " + n + " already defined in scope");
	//        }
	//        // defineProperty(n, cl, DONTENUM);
	//        scope.put(n, scope, cl);
	//    }
}
