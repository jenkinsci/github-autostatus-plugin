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


import org.jenkinsci.plugins.githubautostatus.notifiers.BuildNotifierConstants;
import org.jenkinsci.plugins.githubautostatus.notifiers.InfluxDbNotifier;

import java.lang.reflect.Type;

/**
 * Encapsulates the logic of determining influxdb configuration for a build.
 *
 * @author Jeff Pearce (jeffpearce)
 */
public class InfluxDbNotifierSchemas {
    public static SchemaInfo[]Schemas = {
            new SchemaInfo.v1(),
            new SchemaInfo.v2()
    };

    public static class SeriesNames {
        public static final String Coverage = "coverage";
        public static final String Stage = "stage";
        public static final String Job = "job";
        public static final String TestSuite = "testsuite";
        public static final String Tests = "tests";
        public static final String TestCase = "testcase";
    }

    public static class TagNames {
        public static final String Jobname = "jobname"; // This is for v1 compat; don't use as a tag going forward
        public static final String Owner = "owner";
        public static final String Branch = "branch"; // This is for v1 compat; don't use as a tag going forward
        public static final String Repo = "repo";
        public static final String Result = "result";
        public static final String Blocked = "blocked"; // This is for v1 compat; don't use as a tag going forward
        public static final String StageName = "stagename";
        public static final String Suite = "suite";

        public static class Test {
            public static final String Suite = "suite";
            public static final String TestCase = "testcase";
        }
    }

    public static class FieldNames {
        public static final String Blocked = "blocked";
        public static final String BlockedTime = "blockedtime";
        public static final String Branch = "branch";

        public static class Coverage {
            public static final String Conditionals = "conditionals";
            public static final String Classes = "classes";
            public static final String Files = "files";
            public static final String Instructions = "instructions";
            public static final String Lines = "lines";
            public static final String Methods = "methods";
            public static final String Packages = "packages";
        }

        public static class Test {
            public static final String Passed = "passed";
            public static final String Skipped = "skipped";
            public static final String Failed = "failed";
        }
        public static class TestSuite {
            public static final String Suite = "suite";
        }

        public static class TestCase {
            public static final String TestCase = "testcase";
        }
        public static final String JobName = "jobname";
        public static final String JobTime = "jobtime";
        public static final String Passed = "passed";
        public static final String StageTime = "stagetime";
        public static final String BuildUrl = "buildurl";
        public static final String BuildNumber = "buildnumber";
        public static final String Trigger = "trigger";
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

        public class v1 implements SchemaInfo {
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
                        .AppendTagValue(TagNames.Jobname, jobName)
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)
                        .AppendTagValue(TagNames.Branch, branch)
                        .AppendTagValue(TagNames.Result, result)
                        .AppendTagValue(TagNames.Blocked, blocked)

                        .AppendFieldValue(FieldNames.JobTime, jobtime)
                        .AppendFieldValue(FieldNames.BlockedTime, blockedtime)
                        .AppendFieldValue(FieldNames.Passed, passed)

                        .Build();
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
                        .AppendTagValue(TagNames.Jobname, jobName)
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)
                        .AppendTagValue(TagNames.Branch, branch)
                        .AppendTagValue(TagNames.StageName, stageName)
                        .AppendTagValue(TagNames.Result, result)

                        .AppendFieldValue(FieldNames.StageTime, stageTime)
                        .AppendFieldValue(FieldNames.Passed, passed)

                        .Build();
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
                        .AppendTagValue(TagNames.Jobname, jobName)
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)
                        .AppendTagValue(TagNames.Branch, branch)

                        .AppendFieldValue(FieldNames.Coverage.Classes, classes)
                        .AppendFieldValue(FieldNames.Coverage.Conditionals, conditionals)
                        .AppendFieldValue(FieldNames.Coverage.Files, files)
                        .AppendFieldValue(FieldNames.Coverage.Lines, lines)
                        .AppendFieldValue(FieldNames.Coverage.Methods, methods)
                        .AppendFieldValue(FieldNames.Coverage.Packages, packages)

                        .Build();
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
                        .AppendTagValue(TagNames.Jobname, jobName)
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)
                        .AppendTagValue(TagNames.Branch, branch)

                        .AppendFieldValue(FieldNames.Test.Passed, passed)
                        .AppendFieldValue(FieldNames.Test.Skipped, skipped)
                        .AppendFieldValue(FieldNames.Test.Failed, failed)

                        .Build();
            }

            // "testsuite,jobname=%s,owner=%s,repo=%s,branch=%s,suite=%s passed=%d,skipped=%d,failed=%d"
            public String formatTestSuite(String jobName,
                                          String owner,
                                          String repo,
                                          String branch,
                                          String suite,
                                          int passed,
                                          int skipped,
                                          int failed,
                                          String buildUrl,
                                          int buildNumber,
                                          String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.TestSuite)
                        .AppendTagValue(TagNames.Jobname, jobName)
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)
                        .AppendTagValue(TagNames.Branch, branch)
                        .AppendTagValue(TagNames.Test.Suite, suite)

                        .AppendFieldValue(FieldNames.Test.Passed, passed)
                        .AppendFieldValue(FieldNames.Test.Skipped, skipped)
                        .AppendFieldValue(FieldNames.Test.Failed, failed)

                        .Build();
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
                        .AppendTagValue(TagNames.Jobname, jobName)
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)
                        .AppendTagValue(TagNames.Branch, branch)
                        .AppendTagValue(TagNames.Test.Suite, suite)
                        .AppendTagValue(TagNames.Test.TestCase, testCase)

                        .AppendFieldValue(FieldNames.Test.Passed, passed)
                        .AppendFieldValue(FieldNames.Test.Skipped, skipped)
                        .AppendFieldValue(FieldNames.Test.Failed, failed)

                        .Build();
            }
        }

        public class v2 implements SchemaInfo {

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
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)
                        .AppendTagValue(TagNames.Result, result)

                        .AppendFieldValue(FieldNames.JobName, jobName)
                        .AppendFieldValue(FieldNames.Branch, branch)
                        .AppendFieldValue(FieldNames.JobTime, jobtime)
                        .AppendFieldValue(FieldNames.Blocked, blocked)
                        .AppendFieldValue(FieldNames.BlockedTime, blockedtime)
                        .AppendFieldValue(FieldNames.Passed, passed)
                        .AppendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .AppendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .AppendFieldValue(FieldNames.Trigger, buildCause)

                        .Build();
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
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)
                        .AppendTagValue(TagNames.StageName, stageName)
                        .AppendTagValue(TagNames.Result, result)

                        .AppendFieldValue(FieldNames.JobName, jobName)
                        .AppendFieldValue(FieldNames.Branch, branch)
                        .AppendFieldValue(FieldNames.StageTime, stageTime)
                        .AppendFieldValue(FieldNames.Passed, passed)
                        .AppendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .AppendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .AppendFieldValue(FieldNames.Trigger, buildCause)

                        .Build();
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
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)

                        .AppendFieldValue(FieldNames.JobName, jobName)
                        .AppendFieldValue(FieldNames.Branch, branch)
                        .AppendFieldValue(FieldNames.Coverage.Classes, classes)
                        .AppendFieldValue(FieldNames.Coverage.Conditionals, conditionals)
                        .AppendFieldValue(FieldNames.Coverage.Files, files)
                        .AppendFieldValue(FieldNames.Coverage.Lines, lines)
                        .AppendFieldValue(FieldNames.Coverage.Methods, methods)
                        .AppendFieldValue(FieldNames.Coverage.Packages, packages)
                        .AppendFieldValue(FieldNames.Coverage.Instructions, instructions)
                        .AppendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .AppendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .AppendFieldValue(FieldNames.Trigger, buildCause)

                        .Build();
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
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)

                        .AppendFieldValue(FieldNames.JobName, jobName)
                        .AppendFieldValue(FieldNames.Branch, branch)
                        .AppendFieldValue(FieldNames.Test.Passed, passed)
                        .AppendFieldValue(FieldNames.Test.Skipped, skipped)
                        .AppendFieldValue(FieldNames.Test.Failed, failed)
                        .AppendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .AppendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .AppendFieldValue(FieldNames.Trigger, buildCause)

                        .Build();
            }

            public String formatTestSuite(String jobName,
                                          String owner,
                                          String repo,
                                          String branch,
                                          String suite,
                                          int passed,
                                          int skipped,
                                          int failed,
                                          String buildUrl,
                                          int buildNumber,
                                          String buildCause) {
                return new InfluxDbLineBuilder(SeriesNames.TestSuite)
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)

                        .AppendFieldValue(FieldNames.JobName, jobName)
                        .AppendFieldValue(FieldNames.Branch, branch)
                        .AppendFieldValue(FieldNames.TestSuite.Suite, suite)
                        .AppendFieldValue(FieldNames.Test.Passed, passed)
                        .AppendFieldValue(FieldNames.Test.Skipped, skipped)
                        .AppendFieldValue(FieldNames.Test.Failed, failed)
                        .AppendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .AppendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .AppendFieldValue(FieldNames.Trigger, buildCause)

                        .Build();
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
                        .AppendTagValue(TagNames.Owner, owner)
                        .AppendTagValue(TagNames.Repo, repo)

                        .AppendFieldValue(FieldNames.JobName, jobName)
                        .AppendFieldValue(FieldNames.Branch, branch)
                        .AppendFieldValue(TagNames.Suite, suite)
                        .AppendFieldValue(FieldNames.TestCase.TestCase, testCase)
                        .AppendFieldValue(FieldNames.Test.Passed, passed)
                        .AppendFieldValue(FieldNames.Test.Skipped, skipped)
                        .AppendFieldValue(FieldNames.Test.Failed, failed)
                        .AppendFieldValue(FieldNames.BuildUrl, buildUrl)
                        .AppendFieldValue(FieldNames.BuildNumber, buildNumber)
                        .AppendFieldValue(FieldNames.Trigger, buildCause)

                        .Build();
            }

        }

    }
}
