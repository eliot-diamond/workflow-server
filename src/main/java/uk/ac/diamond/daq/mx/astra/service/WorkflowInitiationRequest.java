package uk.ac.diamond.daq.mx.astra.service;

import java.io.Serializable;
import java.util.UUID;

public class WorkflowInitiationRequest implements Serializable {
	private static final long serialVersionUID = 333444555L;

	private UUID requestId;
	private String workflowName;
	private boolean deleteExistingModel;
	
	public UUID getRequestId() {
		return requestId;
	}
	
	public void setRequestId(UUID requestId) {
		this.requestId = requestId;
	}

	public String getWorkflowName() {
		return workflowName;
	}

	public void setWorkflowName(String workflowName) {
		this.workflowName = workflowName;
	}

	public boolean isDeleteExistingModel() {
		return deleteExistingModel;
	}
	
	public void setDeleteExistingModel(boolean deleteExistingModel) {
		this.deleteExistingModel = deleteExistingModel;
	}
}
