/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.archive.crawler.jmx;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.archive.modules.writer.WARCWriterProcessor;

/**
 * JMX attribute value bean carrying report from {@link WARCWriterProcessor}.
 * <p>this class is a place holder in the trunk since WARCWriterProcessor in
 * the trunk does not expose any useful statistics. more fields are added
 * at runtime.</p>
 * @contributor kenji
 */
public class WARCWriterReport extends ReportBean implements CompositeDataView {
    private WARCWriterProcessor target;
    public WARCWriterReport(WARCWriterProcessor target) {
        this.target = target;
    }
    /**
     * this is a dummy property getter to suppress runtime error from
     * MXBean converter. Apparently attribute value bean must have at
     * least one property. 
     * @return false
     */
    public boolean getWriteRequests() {
        return false;
    }
    
    // order of elements in OPENTYPES, PRIMITIVE_CLASSES, COMPLEX_CLASSES must match.
    private static final OpenType<?> OPENTYPES[] = {
        SimpleType.VOID, SimpleType.BOOLEAN, SimpleType.CHARACTER, SimpleType.BYTE,
        SimpleType.SHORT, SimpleType.INTEGER, SimpleType.LONG, SimpleType.FLOAT, 
        SimpleType.DOUBLE, SimpleType.STRING, SimpleType.BIGDECIMAL, SimpleType.BIGINTEGER,
        SimpleType.DATE, SimpleType.OBJECTNAME
    };
    private static final List<Class<?>> PRIMITIVE_CLASSES = Arrays.asList(new Class<?>[] {
        void.class, boolean.class, char.class, byte.class,
        short.class, int.class, long.class, float.class,
        double.class
    });
    private static final List<Class<?>> COMPLEX_CLASSES = Arrays.asList(new Class<?>[] {
       Void.class, Boolean.class, Character.class, Byte.class,
       Short.class, Integer.class, Long.class, Float.class,
       Double.class, String.class, BigDecimal.class, BigInteger.class,
       Date.class, ObjectName.class
    });

    protected OpenType<?> getOpenType(Class<?> type) {
        // sucks... is there a better way??
        // TODO: support array types
        // CompositeData and TabularData are also allowed types, but they needs special
        // handling as they are interfaces. java.util.Date also sub-class-able, so want
        // to do instanceof comparison.
        int idx = type.isPrimitive() ?
                PRIMITIVE_CLASSES.indexOf(type) :
                    COMPLEX_CLASSES.indexOf(type);
        if (idx == -1) return null;
        return OPENTYPES[idx];
    }

    @Override
    public CompositeData toCompositeData(CompositeType ct) {
        try {
            List<String> itemNames = new ArrayList<String>(/*ct.keySet()*/);
            List<String> itemDescriptions = new ArrayList<String>();
            List<OpenType<?>> itemTypes = new ArrayList<OpenType<?>>();
            List<Object> values = new ArrayList<Object>();
            // javadoc for CompositeDataView had this sample code, but
            // number of types and number of values will not match if
            // this class has one or more properties. if there's none,
            // we don't need to do this anyway.
//            for (String item : itemNames) {
//                itemDescriptions.add(ct.getDescription(item));
//                itemTypes.add(ct.getType(item));
//            }
            BeanInfo beanInfo = Introspector.getBeanInfo(WARCWriterProcessor.class);
            PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                Method get = pd.getReadMethod();
                if (get == null) continue;
                Class<?> t = pd.getPropertyType();
                OpenType<?> ot = getOpenType(t);
                if (ot != null) {
                    // supported type
                    itemNames.add(pd.getName());
                    itemTypes.add(ot);
                    itemDescriptions.add(pd.getName());
                    values.add(get.invoke(target, new Object[0]));
                }
            }
            System.out.format("names=%s types=%s values=%s\n", itemNames, itemTypes, values);
            CompositeType xct =
                new CompositeType(ct.getTypeName(),
                        ct.getDescription(),
                        itemNames.toArray(new String[0]),
                        itemDescriptions.toArray(new String[0]),
                        itemTypes.toArray(new OpenType<?>[0]));
            CompositeData cd = new CompositeDataSupport(xct, itemNames.toArray(new String[0]), values.toArray(new Object[0]));
            assert ct.isValue(cd);  // check we've done it right
            return cd;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
