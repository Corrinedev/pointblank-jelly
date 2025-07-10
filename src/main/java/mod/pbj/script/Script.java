package mod.pbj.script;

public record Script(String id) {
	public Object invokeMethod(String methodName, Object... finalArgs) {
		return null;
	}
	public <T> T invokeMethod(String methodName, Class<T> returnType, Object... args) {
		return null;
		// Object obj = scope.get(methodName, scope);
		// if(obj instanceof Function func) {
		//     Object result = func.call(shell(), scope, scope, args);
		//     if(returnType.isInstance(result)) {
		//         return returnType.cast(result);
		//     } else {
		//         throw new ClassCastException("Expected return type " + returnType.getName() + " but got " +
		//         result.getClass().getName());
		//     }
		//}
		// throw new IllegalStateException("No function found with name: " + methodName);
	}

	public boolean hasFunction(String functionName) {
		// shell(); // Ensure the shell is initialized on thread
		// Object obj = scope.get(functionName, scope);
		// return obj instanceof Function;
		return false;
	}

	public void run() {
		// script.exec(shell(), scope);
	}
}
