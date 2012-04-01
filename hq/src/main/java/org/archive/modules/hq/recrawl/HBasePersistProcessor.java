package org.archive.modules.hq.recrawl;

import org.apache.hadoop.hbase.util.Bytes;
import org.archive.modules.Processor;
import org.archive.modules.recrawl.PersistProcessor;
import org.springframework.beans.factory.annotation.Required;

/**
 * A base class for processors for keeping de-duplication data in HBase.
 * Table schema is defined by {@link RecrawlDataSchema} implementation. 
 * <p>TODO: I could make this class a sub-class of {@link PersistProcessor}, but
 * I didn't because it has BDB specific code in it. Those BDB specific code could be
 * pulled-down into BDB-specific sub-class, making PersistProcessor reusable for
 * different storage types.</p>
 * @contributor kenji
 */
public abstract class HBasePersistProcessor extends Processor {

    protected HBaseClient client;

    protected RecrawlDataSchema schema;

    public void setClient(HBaseClient client) {
        this.client = client;
    }

    public RecrawlDataSchema getSchema() {
        return schema;
    }

    @Required
    public void setSchema(RecrawlDataSchema schema) {
        this.schema = schema;
    }

    public static final byte[] COLUMN_FAMILY = Bytes.toBytes("f");

    public static final byte[] COLUMN_NOCRAWL = Bytes.toBytes("z");

    public HBasePersistProcessor() {
        super();
    }

}
