package uk.ac.diamond.workflow;

@SuppressWarnings("serial")
public class WorkflowException extends Exception {
	public WorkflowException (String message, Throwable throwable) {
		super(message, throwable);
	}
}
