package uk.ac.diamond.workflow.util;

import java.io.IOException;
import java.io.Writer;

import org.slf4j.Logger;

public class LoggerWriter extends Writer {

	private final Logger logger;

	/**
	 * Creates a {@link LoggerWriter} that will pass messages to the specified {@link Logger}.
	 */
	public LoggerWriter(Logger logger) {
		this.logger = logger;
	}

	private StringBuilder buffer = new StringBuilder();

	private State state = State.IN_MESSAGE;

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {

		// This is a really simple state machine that toggles between two states depending on whether or not the next
		// character is a newline. Repeated newline characters are treated as a single line break, so empty lines will
		// be discarded; but in practice they don't matter, and it saves having to work out what newline representation
		// is being used.

		synchronized (buffer) {
			for (int i=off; i<off+len; i++) {
				final char c = cbuf[i];

				switch (state) {

					case IN_MESSAGE:
						if (isNewlineCharacter(c)) {
							logLine(buffer.toString());
							buffer.setLength(0);
							state = State.IN_LINEBREAK;
						} else {
							buffer.append(c);
						}
						break;

					case IN_LINEBREAK:
						if (!isNewlineCharacter(c)) {
							buffer.append(c);
							state = State.IN_MESSAGE;
						}
				}
			}
		}
	}

	/**
	 * Subclass and overwrite to log at a level other than INFO.
	 */
	protected void logLine(String line) {
		logger.info(line);
	}

	private static boolean isNewlineCharacter(char c) {
		return (c == '\r') || (c == '\n');
	}

	@Override
	public void close() throws IOException {
		// ignore
	}

	@Override
	public void flush() throws IOException {
		// ignore
	}

	enum State {
		IN_MESSAGE,
		IN_LINEBREAK
	}

}