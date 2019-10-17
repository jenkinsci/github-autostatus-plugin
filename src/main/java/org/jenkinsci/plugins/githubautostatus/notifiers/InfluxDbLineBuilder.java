package org.jenkinsci.plugins.githubautostatus.notifiers;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

/**
 * Builds a single line for the InfluxDB line protocol.
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class InfluxDbLineBuilder {

    private String measurement;
    private ArrayList<Pair<String, Object>> fields = new ArrayList<>();
    private ArrayList<Pair<String, Object>> tags = new ArrayList<>();

    public InfluxDbLineBuilder(String measurement) {
        this.measurement = measurement;
    }

    public <T> InfluxDbLineBuilder appendTagValue(String tag, T value) {
        tags.add(new ImmutablePair<String, Object>(tag, value));
        return this;
    }

    public <T> InfluxDbLineBuilder appendFieldValue(String field, T value) {
        fields.add(new ImmutablePair<String, Object>(field, value));
        return this;
    }

    public String build() {
        StringBuilder builder = new StringBuilder(measurement);

        for (Pair<String, Object> tag : this.tags) {
            if (tag.getRight() instanceof String) {
                builder.append(String.format(",%s=%s", tag.getLeft(), escapeTagValue((String) tag.getRight())));
            } else {
                builder.append(String.format(",%s=%s", tag.getLeft(), tag.getRight()));
            }
        }
        boolean firstField = true;
        for (Pair<String, Object> field : this.fields) {
            if (firstField) {
                builder.append(" ");
                firstField = false;
            } else {
                builder.append(",");
            }
            if (field.getRight() instanceof String) {
                builder.append(String.format("%s=\"%s\"", field.getLeft(), escapeFieldValue((String) field.getRight())));
            } else if (field.getRight() instanceof Float || field.getRight() instanceof Double) {
                builder.append(String.format("%s=%.4f", field.getLeft(), field.getRight()));
            } else {
                builder.append(String.format("%s=%d", field.getLeft(), field.getRight()));
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
