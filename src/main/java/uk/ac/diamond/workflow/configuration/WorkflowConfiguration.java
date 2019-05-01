package uk.ac.diamond.workflow.configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import uk.ac.diamond.workflow.Workflow;
import uk.ac.diamond.workflow.receiver.WorkflowProcessController;
import uk.ac.diamond.workflow.wrapper.ScriptWrapper;

@Configuration
public class WorkflowConfiguration {
	@Bean
	boolean useEmulator (@Value("${use.emulator:false}") String value) {
		return Boolean.parseBoolean(value);
	}
	
	@Bean
	File scriptWorkingDirectory (@Value("${directory.script.working}") String scriptWorkingDirectoryPath) throws IOException {
		File scriptWorkingDirectory = new File (scriptWorkingDirectoryPath);
		if (!scriptWorkingDirectory.exists() || !scriptWorkingDirectory.isDirectory()) {
			throw new IOException(scriptWorkingDirectoryPath + " is an invalid directory");
		}
		return scriptWorkingDirectory;
	}
	
	@Bean
	File scriptsDirectory (@Value("${directory.workflow.scripts}") String scriptsDirectoryPath) throws IOException {
		File scriptsDirectory = new File (scriptsDirectoryPath);
		if (!scriptsDirectory.exists() || !scriptsDirectory.isDirectory()) {
			throw new IOException(scriptsDirectoryPath + " is an invalid directory");
		}
		return scriptsDirectory;
	}
	
	@Bean
	File modelDirectory (@Value("${workflow.model}") String workflowModelPath) throws IOException {
		File modelFile = new File (workflowModelPath);
		if (!modelFile.exists()) {
			File modelDirectory = modelFile.getParentFile();
			if (!modelDirectory.exists() || !modelDirectory.isDirectory()) {
				throw new IOException(modelDirectory.getAbsolutePath() + " is an invalid directory");
			}
		}
		return modelFile;
	}
	
	@Bean
	ScriptWrapper emulatorScriptWrapper (File scriptsDirectory, File scriptWorkingDirectory) {
		return new ScriptWrapper(new File(scriptsDirectory, "emulator.sh"), scriptWorkingDirectory, "EMULATOR");
	}
	
	@Bean
	Workflow transcalWorkflow (File scriptsDirectory, File scriptWorkingDirectory, 
			ScriptWrapper emulatorScriptWrapper, File modelDirectory, boolean useEmulator) {

		List<ScriptWrapper> scriptWrappers = new ArrayList<>();
		if (useEmulator) {
			scriptWrappers.add(emulatorScriptWrapper);
		}
		scriptWrappers.add(new ScriptWrapper(new File(scriptsDirectory, "transcal.sh"), scriptWorkingDirectory, "TRANSCAL"));
		
		return new Workflow(scriptWrappers, modelDirectory);
	}
	
	@Bean
	Workflow diffractcalWorkflow (File scriptsDirectory, File scriptWorkingDirectory, 
			ScriptWrapper emulatorScriptWrapper, File modelDirectory, boolean useEmulator) {

		List<ScriptWrapper> scriptWrappers = new ArrayList<>();
		
		scriptWrappers.add(new ScriptWrapper(new File(scriptsDirectory, "diffractcal.sh"), scriptWorkingDirectory, "DIFFRACTCAL"));
		
		if (useEmulator) {
			scriptWrappers.add(emulatorScriptWrapper);
		}
		
		return new Workflow(scriptWrappers, modelDirectory);
	}
	
	@Bean
	Workflow stratcalWorkflow (File scriptsDirectory, File scriptWorkingDirectory, 
			ScriptWrapper emulatorScriptWrapper, File modelDirectory, boolean useEmulator) {

		List<ScriptWrapper> scriptWrappers = new ArrayList<>();
		
		scriptWrappers.add(new ScriptWrapper(new File(scriptsDirectory, "stratcal.sh"), scriptWorkingDirectory, "STRATCAL"));
		
		if (useEmulator) {
			scriptWrappers.add(emulatorScriptWrapper);
		}
		
		return new Workflow(scriptWrappers, modelDirectory);
	}
	
	@Bean
	Map<String, Workflow> workflows (Workflow transcalWorkflow, Workflow diffractcalWorkflow, 
			Workflow stratcalWorkflow) {
		Map<String, Workflow> workflows = new HashMap<>();
		
		workflows.put("transcal", transcalWorkflow);
		workflows.put("diffractcal", diffractcalWorkflow);
		workflows.put("stratcal", stratcalWorkflow);
		
		return workflows;
	}
	
	@Autowired
	JmsTemplate jmsTemplate;
	
	@Bean
	WorkflowProcessController workflowProcessController (@Qualifier("workflows") Map<String, Workflow> workflows,
			JmsTemplate jmsTemplate) {
		return new WorkflowProcessController(workflows, jmsTemplate, "workflow-initiation.client.queue");
	}
}
