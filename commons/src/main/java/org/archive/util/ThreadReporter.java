/**
 * 
 */
package org.archive.util;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * ThreadReporter is meant to be implemented by {@link Thread}s for
 * providing extra information in textual format.
 * 
 * methods in this interface used to be part of base interface {@link Reporter}.
 * As we move to template-based reporting, they are now used just for printing
 * additional information about a thread.
 * 
 * @see DevUtils#extraInfo()
 * @author kenji
 *
 */
public interface ThreadReporter extends Reporter {

	/**
	 * Make a default report to the passed-in Writer. Should
	 * be equivalent to reportTo(null, writer)
	 * 
	 * @param writer to receive report
	 */
	public void reportTo(PrintWriter writer) throws IOException;

}
