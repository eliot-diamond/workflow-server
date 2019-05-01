package uk.ac.diamond.workflow;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.workflow.wrapper.ScriptWrapper;

public class Workflow {
	private static final Logger log = LoggerFactory.getLogger(Workflow.class);

	private final List<ScriptWrapper> scriptWrappers;
	private final File modelFile;

	public Workflow (List<ScriptWrapper> scriptWrappers, File modelFile) {
		this.scriptWrappers = scriptWrappers;
		this.modelFile = modelFile;
	}

	private void purgeModelData() {
		if (modelFile.exists()) {
			if (!modelFile.delete()) {
				log.error("Error deleting model data file: {}", modelFile.getAbsolutePath());
			}
		} else {
			log.info("No model data file to purge: {}", modelFile.getAbsolutePath());
		}
	}

	public void restart(UUID enactmentId, boolean deleteModelOnStart) throws WorkflowException {
		for (ScriptWrapper scriptWrapper : scriptWrappers) {
			scriptWrapper.stop();
		}

		try {
			log.info("starting new enactment");
			if (deleteModelOnStart) {
				purgeModelData();
			}
			for (ScriptWrapper scriptWrapper : scriptWrappers) {
				scriptWrapper.start(enactmentId);
			}
		} catch (IOException e) {
			throw new WorkflowException("Problem starting workflow process", e);
		}
	}
}
