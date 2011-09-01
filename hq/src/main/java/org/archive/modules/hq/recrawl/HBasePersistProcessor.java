package org.archive.modules.hq.recrawl;

import org.apache.hadoop.hbase.util.Bytes;
import org.archive.modules.Processor;
import org.archive.modules.recrawl.PersistProcessor;

/**
 * A base class for processors for keeping de-duplication data in HBase.
 * <p>table schema is currently fixed. all data stored in single column
 * family {@code f}, with following columns:
 * <ul>
 * <li>{@code s}: fetch status (as integer text)</li>
 * <li>{@code d}: content digest (with {@code sha1:} prefix, Base32 text)</li>
 * <li>{@code e}: ETag (enclosing quotes stripped)</li>
 * <li>{@code m}: last-modified date-time (as integer timestamp, binary format)</li>
 * </ul>
 * </p>
 * <p>TODO: I could make this class a sub-class of {@link PersistProcessor}, but
 * I didn't because it has BDB specific code in it. Those BDB specific code could be
 * pulled-down into BDB-specific sub-class, making PersistProcessor reusable for
 * different storage types.</p>
 * @contributor kenji
 */
public abstract class HBasePersistProcessor extends Processor {

    protected HBaseClient client;

    public void setClient(HBaseClient client) {
        this.client = client;
    }

    public static final byte[] COLUMN_FAMILY = Bytes.toBytes("f");

    public static final byte[] COLUMN_STATUS = Bytes.toBytes("s");
    public static final byte[] COLUMN_CONTENT_DIGEST = Bytes.toBytes("d");
    public static final byte[] COLUMN_ETAG = Bytes.toBytes("e");
    public static final byte[] COLUMN_LAST_MODIFIED = Bytes.toBytes("m");

    public HBasePersistProcessor() {
        super();
    }

}
