package org.jenkinsci.plugins.githubautostatus.notifiers;

import org.junit.After;
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
        builder.AppendTagValue("tag", "value");
        assertEquals("msr,tag=value", builder.Build());
    }

    @Test
    public void appendIntTagValue() {
        builder.AppendTagValue("tag", 10);
        assertEquals("msr,tag=10", builder.Build());
    }
    @Test
    public void appendStringFieldValue() {
        builder.AppendFieldValue("field", "value");
        assertEquals("msr field=\"value\"", builder.Build());
    }

    @Test
    public void appendIntFieldValue() {
        builder.AppendFieldValue("field", 12);
        assertEquals("msr field=12", builder.Build());
    }

    @Test
    public void appendFloatFieldValue() {
        builder.AppendFieldValue("field", 12.34);
        assertEquals("msr field=12.3400", builder.Build());
    }

    @Test
    public void appendFloatFieldValueRound() {
        builder.AppendFieldValue("field", 12.34567);
        assertEquals("msr field=12.3457", builder.Build());
    }

    @Test
    public void appendTwoFieldTwoTag() {
        builder.AppendFieldValue("field1", "value1");
        builder.AppendFieldValue("field2", "value2");
        builder.AppendTagValue("tag1", "value3");
        builder.AppendTagValue("tag2", "value4");

        assertEquals("msr,tag1=value3,tag2=value4 field1=\"value1\",field2=\"value2\"", builder.Build());
    }


    @Test
    public void build() {
    }
}