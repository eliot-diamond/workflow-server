package uk.ac.diamond.daq.mx.astra.service;

import java.io.Serializable;
import java.util.UUID;

public class WorkflowInitiationResponse implements Serializable {
	private static final long serialVersionUID = 333444556L;

	private UUID requestId;
	private UUID enactmentId;
	private boolean started;
	
	public UUID getRequestId() {
		return requestId;
	}
	
	public void setRequestId(UUID requestId) {
		this.requestId = requestId;
	}

	public UUID getEnactmentId() {
		return enactmentId;
	}
	
	public void setEnactmentId(UUID enactmentId) {
		this.enactmentId = enactmentId;
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public void setStarted(boolean started) {
		this.started = started;
	}
}
