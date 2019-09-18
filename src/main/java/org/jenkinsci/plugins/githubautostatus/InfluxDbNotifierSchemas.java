/*
 * The MIT License
 *
 * Copyright 2018 jxpearce.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.githubautostatus;


import java.lang.reflect.Type;

/**
 * Encapsulates the logic of determining influxdb configuration for a build.
 * @author Jeff Pearce (jeffpearce)
 */
public class InfluxDbNotifierSchemas {
    public static class SeriesNames {
        public static final String Coverage = "coverage";
        public static final String Stage = "stage";
        public static final String Job = "job";

    }
    public static class TagNames {
        public static final String Owner = "owner";
        public static final String Repo = "repo";
        public static final String Result = "result";
        public static final String StageName = "stagename";
    }
    public static class FieldNames {
        public static final String Blocked = "blocked";
        public static final String BlockedTime = "blockedtime";
        public static final String Branch = "branch";
        public static class Coverage {
            public static final String Conditionals = "conditionals";
            public static final String Classes = "classes";
            public static final String Files = "files";
            public static final String Lines = "lines";
            public static final String Methods = "methods";
            public static final String Packages = "packages";
        }
        public static final String Jobname = "jobname";
        public static final String JobTime = "jobtime";
        public static final String Passed = "passed";
        public static final String StageTime = "stagetime";
    }

    class FieldInfo {
        public String name;
        public Type type;

    }

    class SeriesInfo {
        public String seriesName;
        public FieldInfo[] Tags;
        public FieldInfo[] Fields;
    }

    interface SchemaInfo {
        public SeriesInfo getJob();
//        public SeriesInfo getStage();
//        public SeriesInfo getCoverage();
//        public SeriesInfo getTestResults();
    }

//    public class v1 implements SchemaInfo {
//        private SeriesInfo job = new SeriesInfo( ){
//            public String seriesName = SeriesNames.Job;
//            public FieldInfo[] Tags = {
//                    [TagNames.Owner, null]
//
//            };
//            public FieldInfo[] Fields = {
//
//            };
//
//        };
//        public SeriesInfo getJob() {
//            return job;
//        }
//
//    }
//    public class v2 implements SchemaInfo {
//
//    }

}
