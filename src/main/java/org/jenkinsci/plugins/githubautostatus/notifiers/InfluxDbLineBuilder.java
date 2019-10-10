package org.jenkinsci.plugins.githubautostatus.notifiers;

import javafx.util.Pair;

import java.util.ArrayList;

/**
 * Builds a single line for the Influxdb line protocol.
 *
 * @author Jeff Pearce (jeffpearce)
 */
public class InfluxDbLineBuilder {


    private String measurement;
    private ArrayList<Pair<String, Object>> fields = new ArrayList<Pair<String, Object>>();
    private ArrayList<Pair<String, Object>> tags = new ArrayList<Pair<String, Object>>();

    public InfluxDbLineBuilder(String measurement) {
        this.measurement = measurement;
    }

    public <T> InfluxDbLineBuilder AppendTagValue(String tag, T value) {
        tags.add(new Pair(tag, value));
        return this;
    }

    public <T> InfluxDbLineBuilder AppendFieldValue(String field, T value) {
        fields.add(new Pair(field, value));
        return this;
    }

    public String Build() {
        StringBuilder builder = new StringBuilder(measurement);

        for (Pair<String, Object> tag: this.tags) {
            if (tag.getValue() instanceof String) {
                builder.append(String.format(",%s=%s", tag.getKey(), escapeTagValue((String)tag.getValue())));
            } else {
                builder.append(String.format(",%s=%s", tag.getKey(), tag.getValue().toString()));
            }
        }
        boolean firstField = true;
        for (Pair<String, Object> field: this.fields) {
            if (firstField) {
                builder.append(" ");
                firstField = false;
            } else {
                builder.append(",");
            }
            if (field.getValue() instanceof String) {
                builder.append(String.format("%s=\"%s\"", field.getKey(), escapeFieldValue((String)field.getValue())));
            } else if (field.getValue() instanceof Float || field.getValue() instanceof Double) {
                builder.append(String.format("%s=%.4f", field.getKey(), field.getValue()));
            } else {
                builder.append(String.format("%s=%d", field.getKey(), field.getValue()));
            }
        }

        return builder.toString();
    }

    private static String escapeTagValue(String stringValue) {
        return stringValue.replace(" ", "\\ ")
                .replace(",", "\\,")
                .replace("=", "\\=");
    }
    private static String escapeFieldValue(String stringValue) {
        return stringValue.replace("\"", "\\\" ");
    }
}
