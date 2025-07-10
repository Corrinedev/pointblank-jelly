package mod.pbj;

public class InvalidExtensionComponentException extends Exception {
	private static final long serialVersionUID = 1L;
	private String componentName;

	public InvalidExtensionComponentException(String componentName, Throwable cause) {
		super(cause);
		this.componentName = componentName;
	}

	public String getMessage() {
		return this.toString();
	}

	public String toString() {
		return String.format("Invalid extension component %s, error: %s", this.componentName, this.getCause());
	}
}
