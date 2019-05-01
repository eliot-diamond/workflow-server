package uk.ac.diamond.workflow.wrapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.workflow.util.LoggerWriter;
import uk.ac.diamond.workflow.util.StreamPrinter;

public class ScriptWrapper {
	private static final Logger log = LoggerFactory.getLogger(ScriptWrapper.class);

	private final File script;
	private final File scriptWorkingDirectory;
	private final String loggerName;
	private Process runningProcess;
	
	public ScriptWrapper(File script, File scriptWorkingDirectory, String loggerName) {
		super();
		this.script = script;
		this.scriptWorkingDirectory = scriptWorkingDirectory;
		this.loggerName = loggerName;
	}

	public void start(UUID enactmentId) throws IOException {
		ProcessBuilder pb = new ProcessBuilder()
				.command(script.getAbsolutePath())
				.redirectErrorStream(true);

		final Map<String, String> env = pb.environment();
		env.put("ASTRA_wdir", scriptWorkingDirectory.getAbsolutePath());
		env.put("ASTRA_enactmentId", enactmentId.toString());

		log.info("starting new process with command '{}' and env '{}'", script.getAbsolutePath(), env);
		
		runningProcess = pb.start(); // and it leave running
		//long pid = runningProcess.getPid(); // Java 9 only
		
		log.info("redirecting workflow logging");
		final Logger logger = LoggerFactory.getLogger(loggerName);
		final LoggerWriter loggerWriter = new LoggerWriter(logger) {
			@Override
			protected void logLine(String line) {
				logger.debug(line);
			}
		};
		StreamPrinter streamPrinter = new StreamPrinter(runningProcess.getInputStream(), loggerWriter, null, true, "");
		streamPrinter.start();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			//Do nothing
		}
	}
	
	public void stop() {
		log.info("stopping any running enactment");
		if (runningProcess != null && runningProcess.isAlive()) {
			log.info("Stopping process: {}", runningProcess);
			//runningProcess.destroy();
			runningProcess.destroyForcibly();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				//Do nothing
			}
			log.info("Is Alive? => {}", runningProcess.isAlive());
		} else {
			log.info("no process running");
		}
	}
}
