/**
 * 
 */
package org.archive.util;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * TextReporter is a {@link Reporter} that also has additional methods for generating
 * textual report.
 * 
 * Methods of this interface used to be part of {@link Reporter}. They are separated out
 * because templating is now a preferred way of generating textual reports.
 *
 * @see 
 * @author kenji
 *
 */
public interface TextReporter extends Reporter {

    /**
     * Write a short single-line summary report 
     * 
     * @param writer to receive report
     */
    public void shortReportLineTo(PrintWriter pw) throws IOException;
    
    /**
     * Return a legend for the single-line summary report as a String.
     * 
     * @return String single-line summary legend
     */
    @Deprecated
    public String shortReportLegend();

}
