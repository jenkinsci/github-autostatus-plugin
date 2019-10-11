package org.jenkinsci.plugins.githubautostatus.notifiers;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InfluxDbLineBuilderTest {

    private InfluxDbLineBuilder builder;
    private String measurement = "msr";

    @Before
    public void setUp() throws Exception {
        builder = new InfluxDbLineBuilder(measurement);
    }

    @Test
    public void appendStringTagValue() {
        builder.appendTagValue("tag", "value");
        assertEquals("msr,tag=value", builder.build());
    }

    @Test
    public void appendIntTagValue() {
        builder.appendTagValue("tag", 10);
        assertEquals("msr,tag=10", builder.build());
    }
    @Test
    public void appendStringFieldValue() {
        builder.appendFieldValue("field", "value");
        assertEquals("msr field=\"value\"", builder.build());
    }

    @Test
    public void appendIntFieldValue() {
        builder.appendFieldValue("field", 12);
        assertEquals("msr field=12", builder.build());
    }

    @Test
    public void appendFloatFieldValue() {
        builder.appendFieldValue("field", 12.34);
        assertEquals("msr field=12.3400", builder.build());
    }

    @Test
    public void appendFloatFieldValueRound() {
        builder.appendFieldValue("field", 12.34567);
        assertEquals("msr field=12.3457", builder.build());
    }

    @Test
    public void appendTwoFieldTwoTag() {
        builder.appendFieldValue("field1", "value1");
        builder.appendFieldValue("field2", "value2");
        builder.appendTagValue("tag1", "value3");
        builder.appendTagValue("tag2", "value4");

        assertEquals("msr,tag1=value3,tag2=value4 field1=\"value1\",field2=\"value2\"", builder.build());
    }


    @Test
    public void build() {
    }
}