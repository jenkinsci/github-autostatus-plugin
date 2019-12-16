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
package org.jenkinsci.plugins.githubautostatus.notifiers;


/**
 * Encapsulates the logic of determining influxdb configuration for a build.
 *
 * @author Jeff Pearce (GitHub jeffpearce)
 */
public class InfluxDbNotifierSchemas {

    private static final SchemaInfo[] schemas = {
            new SchemaInfo.V1(),
            new SchemaInfo.V2()
    };

    public static int getSchemaCount() {
        return schemas.length;
    }

    public static SchemaInfo getSchema(int schemaIndex) {
        return schemas[schemaIndex];
    }

    private static class SeriesNames {
        private static final String Coverage = "coverage";
        private static final String Stage = "stage";
        private static final String Job = "job";
        private static final String TestSuite = "testsuite";
        private static final String Tests = "tests";
        private static final String TestCase = "testcase";
    }

    private static class TagNames {
        private static final String Jobname = "jobname"; // This is for v1 compat; don't use as a tag going forward
        private static final String Owner = "owner";
        private static final String Branch = "branch"; // This is for v1 compat; don't use as a tag going forward
        private static final String Repo = "repo";
        private static final String Result = "result";
        private static final String Blocked = "blocked"; // This is for v1 compat; don't use as a tag going forward
        private static final String StageName = "stagename";
        @Deprecated
        private static final String Suite = "suite";
        private static final String SuiteName = "suitename";

        private static class Test {
            private static final String Suite = "suite";
            private static final String TestCase = "testcase";
        }
    }

    private static class FieldNames {
        private static final String Blocked = "blocked";
        private static final String BlockedTime = "blockedtime";
        private static final String Branch = "branch";

        public static class Coverage {
            private static final String Conditionals = "conditionals";
            private static final String Classes = "classes";
            private static final String Files = "files";
            private static final String Instructions = "instructions";
            private static final String Lines = "lines";
            private static final String Methods = "methods";
            private static final String Packages = "packages";
        }

        private static class Test {
            private static final String Passed = "passed";
            private static final String Skipped = "skipped";
            private static final String Failed = "failed";
        }

        private static class TestSuite {
            @Deprecated
            private static final String Suite = "suite";
            private static final String Duration = "duration";
        }

        private static class TestCase {
            private static final String TestCase = "testcase";
        }

        private static final String JobName = "jobname";
        private static final String JobTime = "jobtime";
        private static final String Passed = "passed";
        private static final String StageTime = "stagetime";
        private static final String BuildUrl = "buildurl";
        private static final String BuildNumber = "buildnumber";
        private static final String Trigger = "trigger";
    }

    public interface SchemaInfo {
        public String formatJob(String jobName,
                                String owner,
                                String repo,
                                String branch,
                                String result,
                                int blocked,
                                long jobtime,
                                long blockedtime,
                                int passed,
                                String buildUrl,
                                int buildNumber,
                                String buildCause);

        public String formatStage(String jobName,
                                  String owner,
                                  String repo,
                                  String branch,
                                  String stageName,
                                  String result,
                                  long stageTime,
                                  int passed,
                                  String buildUrl,
                                  int buildNumber,
                                  String buildCause);

        public String formatCoverage(String jobName,
                                     String owner,
                                     String repo,
                                     String branch,
                                     float classes,
                                     float conditionals,
                                     float files,
                                     float lines,
                                     float methods,
                                     float packages,
                                     float instructions,
                                     String buildUrl,
                                     int buildNumber,
                                     String buildCause);

        public String formatTests(String jobName,
                                  String owner,
                                  String repo,
                                  String branch,
                                  int passed,
                                  int skipped,
                                  int failed,
                                  String buildUrl,
                                  int buildNumber,
                                  String buildCause);

        public String formatTestSuite(String jobName,
                                      String owner,
                                      String repo,
                                      String branch,
                                      String suite,
                                      float duration,
                                      int passed,
                                      int skipped,
                                      int failed,
                                      String buildUrl,
                                      int buildNumber,
                                      String buildCause);

        public String formatTestCase(String jobName,
                                     String owner,
                                     String repo,
                                     String branch,
                                     String suite,
                                     String testCase,
                                     int passed,
                                     int skipped,
                                     int failed,
                                     String buildUrl,
                                     int buildNumber,
                                     String buildCause);

        public class V1 implements SchemaInfo {
            // "job,jobname=%s,owner=%s,repo=%s,branch=%s,result=%s,blocked=%d jobtime=%d,blockedtime=%d,passed=%d",
            public String formatJob(String jobName,
                                    String owner,
                                    String repo,
                                    String branch,
                                    String result,
                                    int blocked,
                                    long jobtime,
                                    long blockedtime,
                                    int passed,
                                    String buildUrl,
                                    int buildNumber,
                                    String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.Job)
                        .appendTagValue(TagNames.Jobname, jobName)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)
                        .appendTagValue(TagNames.Branch, branch)
                        .appendTagValue(TagNames.Result, result)
                        .appendTagValue(TagNames.Blocked, blocked)

                        .appendFieldValue(FieldNames.JobTime, jobtime)
                        .appendFieldValue(FieldNames.BlockedTime, blockedtime)
                        .appendFieldValue(FieldNames.Passed, passed)

                        .build();
            }

            // "stage,jobname=%s,owner=%s,repo=%s,branch=%s,stagename=%s,result=%s stagetime=%d,passed=%d"
            public String formatStage(String jobName,
                                      String owner,
                                      String repo,
                                      String branch,
                                      String stageName,
                                      String result,
                                      long stageTime,
                                      int passed,
                                      String buildUrl,
                                      int buildNumber,
                                      String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.Stage)
                        .appendTagValue(TagNames.Jobname, jobName)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)
                        .appendTagValue(TagNames.Branch, branch)
                        .appendTagValue(TagNames.StageName, stageName)
                        .appendTagValue(TagNames.Result, result)

                        .appendFieldValue(FieldNames.StageTime, stageTime)
                        .appendFieldValue(FieldNames.Passed, passed)

                        .build();
            }

            // coverage,jobname=%s,owner=%s,repo=%s,branch=%s "classes=%f,conditionals=%f,files=%f,lines=%f,methods=%f,packages=%f
            public String formatCoverage(String jobName,
                                         String owner,
                                         String repo,
                                         String branch,
                                         float classes,
                                         float conditionals,
                                         float files,
                                         float lines,
                                         float methods,
                                         float packages,
                                         float instructions,
                                         String buildUrl,
                                         int buildNumber,
                                         String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.Coverage)
                        .appendTagValue(TagNames.Jobname, jobName)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)
                        .appendTagValue(TagNames.Branch, branch)

                        .appendFieldValue(FieldNames.Coverage.Classes, classes)
                        .appendFieldValue(FieldNames.Coverage.Conditionals, conditionals)
                        .appendFieldValue(FieldNames.Coverage.Files, files)
                        .appendFieldValue(FieldNames.Coverage.Lines, lines)
                        .appendFieldValue(FieldNames.Coverage.Methods, methods)
                        .appendFieldValue(FieldNames.Coverage.Packages, packages)

                        .build();
            }

            // tests,jobname=%s,owner=%s,repo=%s,branch=%s passed=%d,skipped=%d,failed=%d"
            public String formatTests(String jobName,
                                      String owner,
                                      String repo,
                                      String branch,
                                      int passed,
                                      int skipped,
                                      int failed,
                                      String buildUrl,
                                      int buildNumber,
                                      String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.Tests)
                        .appendTagValue(TagNames.Jobname, jobName)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)
                        .appendTagValue(TagNames.Branch, branch)

                        .appendFieldValue(FieldNames.Test.Passed, passed)
                        .appendFieldValue(FieldNames.Test.Skipped, skipped)
                        .appendFieldValue(FieldNames.Test.Failed, failed)

                        .build();
            }

            // "testsuite,jobname=%s,owner=%s,repo=%s,branch=%s,suite=%s passed=%d,skipped=%d,failed=%d"
            public String formatTestSuite(String jobName,
                                          String owner,
                                          String repo,
                                          String branch,
                                          String suite,
                                          float duration,
                                          int passed,
                                          int skipped,
                                          int failed,
                                          String buildUrl,
                                          int buildNumber,
                                          String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.TestSuite)
                        .appendTagValue(TagNames.Jobname, jobName)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)
                        .appendTagValue(TagNames.Branch, branch)
                        .appendTagValue(TagNames.Test.Suite, suite)

                        .appendFieldValue(FieldNames.TestSuite.Duration, duration)
                        .appendFieldValue(FieldNames.Test.Passed, passed)
                        .appendFieldValue(FieldNames.Test.Skipped, skipped)
                        .appendFieldValue(FieldNames.Test.Failed, failed)

                        .build();
            }

            // "testcase,jobname=%s,owner=%s,repo=%s,branch=%s,suite=%s,testcase=%s passed=%d,skipped=%d,failed=%d"
            public String formatTestCase(String jobName,
                                         String owner,
                                         String repo,
                                         String branch,
                                         String suite, String testCase,
                                         int passed,
                                         int skipped,
                                         int failed,
                                         String buildUrl,
                                         int buildNumber,
                                         String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.TestCase)
                        .appendTagValue(TagNames.Jobname, jobName)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)
                        .appendTagValue(TagNames.Branch, branch)
                        .appendTagValue(TagNames.Test.Suite, suite)
                        .appendTagValue(TagNames.Test.TestCase, testCase)

                        .appendFieldValue(FieldNames.Test.Passed, passed)
                        .appendFieldValue(FieldNames.Test.Skipped, skipped)
                        .appendFieldValue(FieldNames.Test.Failed, failed)

                        .build();
            }
        }

        public class V2 implements SchemaInfo {

            public String formatJob(String jobName,
                                    String owner,
                                    String repo,
                                    String branch,
                                    String result,
                                    int blocked,
                                    long jobtime,
                                    long blockedtime,
                                    int passed,
                                    String buildUrl,
                                    int buildNumber,
                                    String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.Job)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)
                        .appendTagValue(TagNames.Result, result)

                        .appendFieldValue(FieldNames.JobName, jobName)
                        .appendFieldValue(FieldNames.Branch, branch)
                        .appendFieldValue(FieldNames.Blocked, blocked)
                        .appendFieldValue(FieldNames.JobTime, jobtime)
                        .appendFieldValue(FieldNames.BlockedTime, blockedtime)
                        .appendFieldValue(FieldNames.Passed, passed)
                        .appendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .appendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .appendFieldValue(FieldNames.Trigger, buildCause)

                        .build();
            }

            public String formatStage(String jobName,
                                      String owner,
                                      String repo,
                                      String branch,
                                      String stageName,
                                      String result,
                                      long stageTime,
                                      int passed,
                                      String buildUrl,
                                      int buildNumber,
                                      String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.Stage)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)
                        .appendTagValue(TagNames.StageName, stageName)
                        .appendTagValue(TagNames.Result, result)

                        .appendFieldValue(FieldNames.JobName, jobName)
                        .appendFieldValue(FieldNames.Branch, branch)
                        .appendFieldValue(FieldNames.StageTime, stageTime)
                        .appendFieldValue(FieldNames.Passed, passed)
                        .appendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .appendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .appendFieldValue(FieldNames.Trigger, buildCause)

                        .build();
            }

            public String formatCoverage(String jobName,
                                         String owner,
                                         String repo,
                                         String branch,
                                         float classes,
                                         float conditionals,
                                         float files,
                                         float lines,
                                         float methods,
                                         float packages,
                                         float instructions,
                                         String buildUrl,
                                         int buildNumber,
                                         String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.Coverage)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)

                        .appendFieldValue(FieldNames.JobName, jobName)
                        .appendFieldValue(FieldNames.Branch, branch)
                        .appendFieldValue(FieldNames.Coverage.Classes, classes)
                        .appendFieldValue(FieldNames.Coverage.Conditionals, conditionals)
                        .appendFieldValue(FieldNames.Coverage.Files, files)
                        .appendFieldValue(FieldNames.Coverage.Lines, lines)
                        .appendFieldValue(FieldNames.Coverage.Methods, methods)
                        .appendFieldValue(FieldNames.Coverage.Packages, packages)
                        .appendFieldValue(FieldNames.Coverage.Instructions, instructions)
                        .appendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .appendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .appendFieldValue(FieldNames.Trigger, buildCause)

                        .build();
            }

            public String formatTests(String jobName,
                                      String owner,
                                      String repo,
                                      String branch,
                                      int passed,
                                      int skipped,
                                      int failed,
                                      String buildUrl,
                                      int buildNumber,
                                      String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.Tests)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)

                        .appendFieldValue(FieldNames.JobName, jobName)
                        .appendFieldValue(FieldNames.Branch, branch)
                        .appendFieldValue(FieldNames.Test.Passed, passed)
                        .appendFieldValue(FieldNames.Test.Skipped, skipped)
                        .appendFieldValue(FieldNames.Test.Failed, failed)
                        .appendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .appendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .appendFieldValue(FieldNames.Trigger, buildCause)

                        .build();
            }

            public String formatTestSuite(String jobName,
                                          String owner,
                                          String repo,
                                          String branch,
                                          String suite,
                                          float duration,
                                          int passed,
                                          int skipped,
                                          int failed,
                                          String buildUrl,
                                          int buildNumber,
                                          String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.TestSuite)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)
                        .appendTagValue(TagNames.SuiteName, suite)


                        .appendFieldValue(FieldNames.JobName, jobName)
                        .appendFieldValue(FieldNames.Branch, branch)
                        .appendFieldValue(FieldNames.TestSuite.Suite, suite)
                        .appendFieldValue(FieldNames.TestSuite.Duration, duration)
                        .appendFieldValue(FieldNames.Test.Passed, passed)
                        .appendFieldValue(FieldNames.Test.Skipped, skipped)
                        .appendFieldValue(FieldNames.Test.Failed, failed)
                        .appendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .appendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .appendFieldValue(FieldNames.Trigger, buildCause)

                        .build();
            }

            public String formatTestCase(String jobName,
                                         String owner,
                                         String repo,
                                         String branch,
                                         String suite,
                                         String testCase,
                                         int passed,
                                         int skipped,
                                         int failed,
                                         String buildUrl,
                                         int buildNumber,
                                         String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.TestCase)
                        .appendTagValue(TagNames.Owner, owner)
                        .appendTagValue(TagNames.Repo, repo)
                        .appendTagValue(TagNames.SuiteName, suite)

                        .appendFieldValue(FieldNames.JobName, jobName)
                        .appendFieldValue(FieldNames.Branch, branch)
                        .appendFieldValue(TagNames.Suite, suite)
                        .appendFieldValue(FieldNames.TestCase.TestCase, testCase)
                        .appendFieldValue(FieldNames.Test.Passed, passed)
                        .appendFieldValue(FieldNames.Test.Skipped, skipped)
                        .appendFieldValue(FieldNames.Test.Failed, failed)
                        .appendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .appendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .appendFieldValue(FieldNames.Trigger, buildCause)

                        .build();
            }

        }

    }
}
