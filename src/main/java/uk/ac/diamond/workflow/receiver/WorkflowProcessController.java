package uk.ac.diamond.workflow.receiver;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import uk.ac.diamond.daq.mx.astra.service.WorkflowInitiationRequest;
import uk.ac.diamond.daq.mx.astra.service.WorkflowInitiationResponse;
import uk.ac.diamond.workflow.Workflow;
import uk.ac.diamond.workflow.WorkflowException;

@Component
public class WorkflowProcessController {
	private static final Logger log = LoggerFactory.getLogger(WorkflowProcessController.class);
	
	private final Map<String, Workflow> workflows;
	private final JmsTemplate jmsTemplate;
	private final String outboundQueue;
	
	public WorkflowProcessController (Map<String, Workflow> workflows, JmsTemplate jmsTemplate, String outboundQueue) {
		this.workflows = workflows;
		this.jmsTemplate = jmsTemplate;
		this.outboundQueue = outboundQueue;
		
		log.info("Starting workflow controller");
		for (Entry<String, Workflow> entry : workflows.entrySet()) {
			log.info("Loaded workflow => Name: {}, Workflow Object: {}", entry.getKey(), entry.getValue());
		}
	}
	
	@JmsListener (destination = "workflow-initiation.server.queue", containerFactory = "myFactory")
	public void initiateWorkflow (WorkflowInitiationRequest workflowInitiationRequest) {
		log.info("{} workflow requested with ID of {}", 
				workflowInitiationRequest.getWorkflowName(), 
				workflowInitiationRequest.getRequestId());
		
		Workflow workflow = workflows.get(workflowInitiationRequest.getWorkflowName());
		if (workflow == null) {
			log.error("No workflow found with name {}", workflowInitiationRequest.getWorkflowName());
			return;
		}
		
		UUID enactmentId = UUID.randomUUID();

		WorkflowInitiationResponse workflowInitiationResponse = new WorkflowInitiationResponse();
		workflowInitiationResponse.setRequestId(workflowInitiationRequest.getRequestId());
		workflowInitiationResponse.setEnactmentId(enactmentId);
		
		try {
			workflow.restart(enactmentId, workflowInitiationRequest.isDeleteExistingModel());
			workflowInitiationResponse.setStarted(true);
		} catch (WorkflowException e) {
			log.error ("Failed to restart workflow", e);
			workflowInitiationResponse.setStarted(false);
		}
		
		jmsTemplate.convertAndSend(outboundQueue, workflowInitiationResponse);
	}
}
