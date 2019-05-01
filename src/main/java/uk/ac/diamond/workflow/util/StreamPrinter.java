package uk.ac.diamond.workflow.util;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.Arrays;

/**
 * Class to use a {@link PrintStream} to output the data being received
 * by an {@link InputStream}. A typical usage pattern (with the thread being
 * created internally to the instance) is:
 * 
 * <pre>
 *  Process process = processBuilder.start();
 *  StreamPrinter errPrinter = new StreamPrinter(process.getErrorStream(), System.err);
 *  StreamPrinter outPrinter = new StreamPrinter(process.getInputStream(), System.out);
 *  errPrinter.start();
 *  outPrinter.start();
 *  int status = process.waitFor();
 *  errPrinter.join();
 *  outPrinter.join();
 * </pre>
 *
 * Alternatively, the thread can be created externally (for example to create
 * the thread with non-default options, or to operate on the thread with methods
 * other than {@link Thread#start()} and {@link Thread#join()}):
 * 
 * <pre>
 *  Process process = processBuilder.start();
 *  StreamPrinter errPrinter = new StreamPrinter(process.getErrorStream(), System.err);
 *  StreamPrinter outPrinter = new StreamPrinter(process.getInputStream(), System.out);
 *  Thread errThread = new Thread(errPrinter);
 *  Thread outThread = new Thread(outPrinter);
 *  errThread.start();
 *  outThread.start();
 *  int status = process.waitFor();
 *  errThread.join();
 *  outThread.join();
 * </pre>
 *
 * <p>The code to handle the stream data line-by-line has been adapted from a
 * suggestion by Boris Daich at <a href="http://stackoverflow.com/a/1732506/1866402">
 * http://stackoverflow.com/questions/1732455/redirect-process-output-to-stdout</a>.</p>
 * 
 * <p>The code to decode the byte buffer has been adapted from a FileChannel example
 * in "Java Examples in a Nutshell", 3rd Edition (Flanagan, 2004) found at
 * <a href="http://www.java2s.com/Code/Java/File-Input-Output/ReadbytesfromthespecifiedchanneldecodethemusingthespecifiedCharsetandwritetheresultingcharacterstothespecifiedwriter.htm">
 * http://www.java2s.com/Code/Java/File-Input-Output/ReadbytesfromthespecifiedchanneldecodethemusingthespecifiedCharsetandwritetheresultingcharacterstothespecifiedwriter.htm</a>.
 * </p>
 * 
 * 
 *
 * @see java.lang.ProcessBuilder
 * 
 * @author pkeller
 *
 */
public class StreamPrinter implements Runnable {
    private InputStream is;
    private Writer outputWriter = null;
    private File outputFile;
    private boolean append;
    private String header;
    final private int cbufSize, delay;
    
    private Thread thread = null;
    
    // FIXME! Specify charset for both line-by-line and buffered handling
    // via a system property.
    private static final Charset charset = Charset.defaultCharset();

    private String lastLine = null;
    private CharBuffer lineSepBuf;
    private final boolean captureLastLine;
    
    /**
     * System property name for sizing internal character buffer, where caller has specified
     * the default size. The value may be negative, to switch to line-by-line operation
     */
    public final static String BUFSIZEPROPERTY = "co.gphl.streamprinter.cbufsize";
    public final static int defaultBufSize = 4096;
    
    /**
     * System property name for stream read delay, where caller has specified
     * the default. The value may be negative, to disable the read delay. This will lower
     * the latency, but may reduce the efficiency of the buffering.
     */
    public final static String DELAYPROPERTY = "co.gphl.streamprinter.delay";
    public final static int defaultDelay = 100;
    
    /**
     * Capture the line-by-line output of a stream and direct it to a {@link Writer}, a {@link File}, or both.
     * {@code outputWriter} is flushed after every line, so can be used where an immediate
     * response to new data on {@code inputStream} is needed. {@code outputFile} is not
     * flushed to reduce the likelihood of writes to it blocking. If output to only one of
     * {@code outputWriter} and {@code outputFile} is needed, use {@code null} for the
     * other one.
     * 
     * @param inputStream
     * @param outputWriter
     * @param outputFile
     * @param append if {@code true}, output is appended to {@code outputFile}.
     * @param header if not {@code null}, start output with this string
     */
    public StreamPrinter(InputStream inputStream, Writer outputWriter, File outputFile,
                         boolean append, String header) {
        this(inputStream, outputWriter, outputFile, append, header, -1, -1, true);
    }

    /**
     * Capture the output of a stream and direct it to a {@link Writer}, a {@link File}, or both.
     * When instantiated via this constructor, the StreamPrinter attempts to provide the
     * best of both worlds:
     * 
     * <ul>
     * <li>when the rate of output is high, efficient transfer using a buffer</li>
     * <li>when the rate of output is low, latency at the destination(s) almost as
     * low as if using blocking, unbuffered I/O</li>
     * </ul>
     * 
     * The stream data are read into a buffer, whose contents are sent to the
     * destination(s) when either:
     * 
     * <ul>
     * <li>the buffer fills up, or</li>
     * <li>no data are available from the stream for a period of {@code delay} milliseconds</li>
     * </ul>
     * 
     * <p>{@code outputFile} is not flushed to reduce the likelihood of writes to it blocking.
     * If output to only one of {@code outputWriter} and {@code outputFile} is needed,
     * use {@code null} for the other one.</p>
     * 
     * <p>The last line of the output will not be available on platforms
     * where the line separator string is more than one character long, when using buffering. An
     * {@code UnsupportedOperationException} will be thrown if the arguments to this constructor
     * imply a violation of this condition.</p>
     * 
     * @param inputStream
     * @param outputWriter
     * @param outputFile
     * @param append if {@code true}, output is appended to {@code outputFile}.
     * @param header if not {@code null}, start output with this string
     * @param cbufSize size of character buffer to be used for reading from inputStream.
     * {@code if (cbufSize < 0)} line-by-line behaviour will be used. {@code if (cbufSize == 0)}
     * the buffer size will be set from the system property {@code co.gphl.streamprinter.cbufsize} if set,
     * otherwise an internal default of 4096 characters will be used.
     * @param delay number of milliseconds to wait for more input, before sending
     * the output received so far to the destination(s). {@code if (delay < 0)}, the delay
     * will be disabled. {@code if (delay == 0)}, the delay will be set from the system property
     * {@code co.gphl.streamprinter.delay} if set, otherwise an internal default of 100
     * milliseconds will be used.
     * @param captureLastLine if {@code true} the last line of the stream will be available
     * from the method {@link #getLastLine()}
     * 
     * @throws UnsupportedOperationException if
     * {@code captureLastLine && cbufSize >= 0 && System.getProperty("line.separator").length() > 1}
     */
    public StreamPrinter(InputStream inputStream, Writer outputWriter, File outputFile,
            boolean append, String header, int cbufSize, int delay, boolean captureLastLine) {

        this.is = inputStream;
        this.outputWriter = outputWriter;
        this.outputFile = outputFile;
        this.append = append;
        this.header = header;
        this.captureLastLine = captureLastLine;

        if ( cbufSize == 0 ) {
            String cbufSizeStr = System.getProperty(StreamPrinter.BUFSIZEPROPERTY);
            if ( cbufSizeStr != null )
                cbufSize = Integer.parseInt(cbufSizeStr);
        }
        
        if ( cbufSize == 0 )
            cbufSize = StreamPrinter.defaultBufSize;
        
        this.cbufSize = cbufSize;

        if ( delay == 0 ) {
            String delayStr = System.getProperty(StreamPrinter.DELAYPROPERTY);
            if ( delayStr != null )
                delay = Integer.parseInt(delayStr);
        }
        
        if ( delay == 0 )
            delay = StreamPrinter.defaultDelay;
        else if ( delay < 0 )
            // Disabling the delay is done by setting the true internal delay to 0ms, so
            // we translate an input value of < 0 to 0 here.
            delay = 0;
        
        this.delay = delay;
        
        if ( this.cbufSize > 0 ) {
            this.lineSepBuf = CharBuffer.wrap(System.getProperty("line.separator"));
            
            // The algorithm in the buffered fetch will fail to identify the last line correctly
            // if the line separator string is more than one character long and it happens to
            // be split across two reads from the input stream.
            if ( this.lineSepBuf.length() > 1 && this.captureLastLine )
                throw new UnsupportedOperationException("Use line-by-line operation on platforms "
                        + "where the line separator string is more then one character");
        }
        
    }
    
    @Override
    public void run() {

        try ( Writer fileWriter = this.outputFile == null ?
                null : new FileWriter(outputFile, this.append) ) {

            if ( fileWriter != null )
                fileWriter.write(this.header);
            if ( this.outputWriter != null )
                this.outputWriter.write(this.header);


            if ( this.cbufSize <= 0 )
                this.pipeLineByLine(new BufferedReader(new InputStreamReader(is)), fileWriter);
            else
                this.pipeWithBuffer(is, fileWriter);
            
        }
        catch (IOException e) {
            if (this.header != null) {
                System.err.println(this.header);
            }
            // Don't abort workflow over this: it may still run OK even if
            // the user-facing output isn't being written properly to the file
            e.printStackTrace();
        }
    }

    private void pipeLineByLine(BufferedReader br, Writer fileWriter) throws IOException {

        String line=null;

        // For efficiency, don't test for destinations inside a
        // "while ( (line = br.readLine()) != null)" loop
        if ( this.outputWriter != null && fileWriter == null ) {
            while ( (line = br.readLine()) != null) {
                this.outputWriter.write(line + "\n");
                this.outputWriter.flush();
                this.lastLine = line;
            }
        }
        else if ( this.outputWriter != null && fileWriter != null ) {
            while ( (line = br.readLine()) != null) {
                this.outputWriter.write(line + "\n");
                this.outputWriter.flush();
                fileWriter.write(line + "\n");
                // Don't flush file writer.
                this.lastLine = line;
            }
        }
        else if ( this.outputWriter == null && fileWriter != null ) {
            fileWriter.write(line + "\n");
            this.lastLine = line;
        }

    }
    
    /* Reading from the stream directly rather than via a Reader is the key to
     * doing non-blocking I/O: we can use the available() method to ensure that
     * reads don't block when we don't want them to. The downsides are:
     * 
     * (1) we have to handle the conversion from bytes to characters ourselves, although
     * that isn't hard, 
     * 
     * (2) capturing the last line of output accurately (needed to check the exit status
     * of stratcal and simcal_predict) is fiddly
     * 
     * Unfortunately, Process/ProcessBuilder don't provide NIO access to stdout/stderr, so
     * we have to resort to some low-level trickery.
     * TODO: maybe this suggestion has the answer:
     * https://stackoverflow.com/a/1033795/1866402
     */
    private void pipeWithBuffer(InputStream is, Writer fileWriter) throws IOException {

        CharBuffer cbuf = CharBuffer.allocate(this.cbufSize);
        ByteBuffer bbuf = ByteBuffer.allocate(this.cbufSize);
        byte[] barray = bbuf.array();
        char[] carray = cbuf.array();
        int readThisTime = 0, available, bpos, cpos, clim;

        // These variables are used in the code that captures the last line of stdout
        StringBuilder lastLineBldr = new StringBuilder(200);
        int lineSepLen = lineSepBuf.length(), startOfLastLine, searchBolFrom;
        boolean gotEol = false;
        
        CharsetDecoder decoder = StreamPrinter.charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        CoderResult decodeResult = null;
        
        do {

            /* This inner loop does the following:
             * (i) If the receiving buffer is empty, read (blocking) until we get something from
             * the input stream.
             * (ii) If the receiving buffer is not empty and there is something available
             * from the stream, read it (non-blocking) and go around the loop again.
             * (iii) If the receiving buffer is not empty and there is nothing available from
             * the input stream, pause for this.delay (in milliseconds) and check again. If something has
             * become available, handle as (ii)
             * 
             * We exit the loop on one of the following conditions:
             * (1) Receiving buffer is full (loop's while(...) condition not satisfied)
             * (2) A read from the stream indicates end-of-stream (break)
             * (3) In case (iii), nothing more is available from the stream after
             * the second check (break)
             * 
             * On exit from this loop, if the buffer is not empty its contents are decoded
             * and emitted.
             */
            do {
                available = is.available();
                bpos = bbuf.position();

                if ( available == 0 && bpos > 0 ) {

                    try {
                        Thread.sleep(this.delay);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    available = is.available();
                    if ( available == 0 )
                        break;
                }
                
                if ( available == 0 )
                    // bpos can only be 0 if available == 0 here, so OK to use the
                    // single-argument form of read()
                    readThisTime = is.read( barray );
                else
                    // Never let the len parameter of is.read(...) be 0, otherwise we will not
                    // detect the end of stream and never exit this loop.
                    readThisTime = is.read( barray, bpos, Math.min( available, bbuf.remaining() ) );

                if ( readThisTime < 0 )
                    break;
                bbuf.position( bpos + readThisTime );

            } while ( bbuf.hasRemaining() );

            /* When the input stream is producing data rapidly, we will be handling
             * it in large chunks.
             * Testing for this.outputWriter/this.fileWriter != null each time we emit
             * the buffer contents is not an efficiency concern here, unlike
             * in the line-by-line case.
             */ 
            while ( bbuf.position() > 0 ) {
                
                bbuf.flip();
                decodeResult = decoder.decode(bbuf, cbuf, readThisTime < 0);
                cbuf.flip();
                
                cpos = cbuf.position();
                clim = cbuf.limit();
                
                if ( this.outputWriter != null ) {
                    this.outputWriter.write(cbuf.array(), cpos, cbuf.remaining());
                    this.outputWriter.flush();
                }
                if ( fileWriter != null )
                    fileWriter.write(cbuf.array(), cpos, cbuf.remaining());

                if ( decodeResult.isError() ) {
                    String msg = "\nError: Attempt to decode output of subprocess returned a\n"
                            + CoderResult.class.getName() + " value of " + decodeResult.toString() + "\n";
                    
                    int start = bbuf.position(), 
                            len = decodeResult.length(),
                            nBytes = Math.min(len, 10);
                    byte[] badBytes = Arrays.copyOfRange(barray, start, start + nBytes);
                    String badBytesStr = Arrays.toString(badBytes);
                    if ( nBytes < len )
                        badBytesStr = badBytesStr.replaceFirst("\\]\\s*$", ", ...]");

                    msg += "  Undecodable bytes are (or start with): " + badBytesStr + "\n";
                    
                    if ( this.outputWriter != null ) {
                        this.outputWriter.write(msg);
                        this.outputWriter.flush();
                    }
                    if ( fileWriter != null )
                        fileWriter.write(msg);
                    
                    bbuf.position( bbuf.position() + decodeResult.length() );
                    decodeResult = null;
                    
                }
                
                // Fiddly code to get last line from buffer, allowing for the fact
                // that the last line may be split across more than one read from
                // the stream. We avoid the use of java.lang.String as far as possible 
                // to reduce the amount of copying of characters from one place to another
                // (most methods/expressions that return an instance of String involve
                // copying character data around).
                
                else if ( this.captureLastLine && clim >= lineSepLen ) {
                
                    // Where in the buffer do we step backwards from, to look for the start of the last line?
                    // The end of the buffer, unless the buffer ends with a newline, in which case
                    // we start from immediately before the newline.
                    searchBolFrom = clim;
                    if ( cbuf.subSequence(clim - lineSepLen, clim).compareTo(lineSepBuf) == 0 ) {
                        gotEol = true;
                        searchBolFrom -= lineSepLen;
                    }
                
                    // Now step backwards to find the start of the last line. If we find it,
                    // we discard anything that we have accumulated from earlier iterations
                    // as (part of) the putative last line.
                    for ( startOfLastLine = searchBolFrom - 1; startOfLastLine >= cpos; startOfLastLine-- ) {
                        if ( cbuf.subSequence(startOfLastLine, startOfLastLine + lineSepLen).compareTo(lineSepBuf) == 0) {
                            lastLineBldr.delete(0, lastLineBldr.length());
                            startOfLastLine += lineSepLen;
                            break;
                        }
                    }
                    
                    // If we emerged from the end of the for loop, we will have gone one character too far
                    // back.
                    if ( startOfLastLine < cpos )
                        startOfLastLine = cpos;
                    
                    lastLineBldr.append(carray, startOfLastLine, searchBolFrom - startOfLastLine);
                    
                    if ( gotEol ) {
                        this.lastLine = lastLineBldr.toString();
                        // Reset state, in case this isn't in fact the
                        // last line and more output is on its way.
                        lastLineBldr.delete(0, lastLineBldr.length());
                        gotEol = false;
                    }
                    else
                        this.lastLine = null;
                    
                }
                bbuf.compact();
                cbuf.clear();
            }
            
        } while ( readThisTime >= 0 );
    }
    
    public boolean isCaptureLastLine() {
        return this.captureLastLine;
    }
    
    /**
     * Returns the last line of output from the input stream.
     * 
     * @return last line of output.
     * 
     * @throws UnsupportedOperationException {@code if ( !this.isCaptureLastLine() )}
     */
    public String getLastLine() {
        if ( ! this.captureLastLine )
            throw new UnsupportedOperationException("This StreamPrinter has not been configured to "
                    + "capture the last line of output");
        return this.lastLine;
    }

    /**
     * Creates a new thread for this instance, and starts it. The thread
     * is created with {@code new Thread(this)}.
     * 
     * @throws IllegalStateException if this method has been previously invoked
     * on this instance.
     */
    public void start() {
        if ( this.thread != null )
            throw new IllegalStateException("BUG: You have called start() more than once!");
        this.thread = new Thread(this);
        
        this.thread.start();
    }

    /**
     * Calls {@link Thread#join()} on the thread that was created with the
     * {@link #start()} instance.
     * 
     * @throws IllegalStateException if {@link #start()} has not been previously
     * invoked on this instance, or the thread is in a {@link java.lang.Thread.State#NEW}
     * state.
     * @throws InterruptedException
     */
    public void join() throws InterruptedException {
        Thread.State state = this.thread == null ? null : this.thread.getState();
        if ( state == null || state == Thread.State.NEW )
            throw new IllegalStateException("BUG: No started thread associated with this StreamPrinter: "
                    + "have you forgotten to call start()?");
        
        this.thread.join();
    }

}