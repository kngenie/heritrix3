package org.archive.crawler.jmx;

import java.util.Map;

/**
 * <code>ReportBean</code> implements common service methods for use
 * in crawl statistics report beans.
 * @author kenji
 */
public class ReportBean {

    public ReportBean() {
        super();
    }

    /**
     * get named integer value from <code>map</code>.
     * @param map key-value pairs
     * @param name key
     * @return integer value. zero for missing or non-integer value.
     */
    protected int getInt(Map<String, ?> map, String name) {
        if (map == null) return 0;
        Object v = map.get(name);
        if (v instanceof Integer) {
            return ((Integer)v).intValue();
        } else {
            return 0;
        }
    }
    /**
     * get named long value from <code>map</code>.
     * currently it does <i>not</i> convert non-long number types into long.
     * for example, if named value is actually int, not long, zero will be returned.
     * @param map key-value pairs
     * @param name key
     * @return long value, zero for missing or non-long value.
     */
    protected long getLong(Map<String, ?> map, String name) {
        if (map == null) return 0;
        Object v = map.get(name);
        if (v instanceof Long) {
            return ((Long)v).longValue();
        } else {
            return 0;
        }
    }

    /**
     * get named double or float value from <code>map</code>.
     * converts float to double. no conversion is made for other number
     * types, specifically integral ones.
     * @param map key-value pairs
     * @param name key
     * @return double value, <code>0.0</code> for missing or non-float/double value.
     */
    protected double getDouble(Map<String, ?> map, String name) {
        if (map == null) return 0.0;
        Object v = map.get(name);
        if (v instanceof Double) {
            return ((Double)v).doubleValue();
        } else if (v instanceof Float) {
            return ((Float)v).doubleValue();
        } else {
            return 0.0;
        }
    }
    /**
     * get named String value from <code>map</code>.
     * converts non-String value into String with {@link #toString()}.
     * @param map key-value pairs
     * @param name key
     * @return String value. <code>null</code> if <code>key</code> does not exist in <code>map</code>.
     */
    protected String getString(Map<String, ?> map, String name) {
        if (map == null) return null;
        Object v = map.get(name);
        return v != null ? v.toString() : null;
    }

}
