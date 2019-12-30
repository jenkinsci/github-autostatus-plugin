package org.jenkinsci.plugins.githubautostatus;

import org.jenkinsci.plugins.githubautostatus.notifiers.InfluxDbNotifierSchemas;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Jeff Pearce (github jeffpearce)
 */
public class InfluxDbNotifierSchemasTest {

    private InfluxDbNotifierSchemas.SchemaInfo.V1 v1Schema;
    private InfluxDbNotifierSchemas.SchemaInfo.V2 v2Schema;

    @Before
    public void setUp() {
        v1Schema = new InfluxDbNotifierSchemas.SchemaInfo.V1();
        v2Schema = new InfluxDbNotifierSchemas.SchemaInfo.V2();

    }

    @Test
    public void testV1Job() {
        assertEquals(
                "job,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch,result=CompletedError,blocked=1 jobtime=2,blockedtime=3,passed=4",
                v1Schema.formatJob("mockjobname", "mockowner", "mockrepo", "mockbranch", "CompletedError", 1, 2, 3, 4, "ubuildurl", 5, "cause"));
    }
    @Test
    public void testV2Job() {
        assertEquals(
                "job,owner=mockowner,repo=mockrepo,result=CompletedError jobname=\"mockjobname\",branch=\"mockbranch\",blocked=1,jobtime=2,blockedtime=3,passed=4,buildurl=\"buildurl\",buildnumber=5,trigger=\"cause\"",
                v2Schema.formatJob("mockjobname", "mockowner", "mockrepo", "mockbranch", "CompletedError", 1, 2, 3, 4, "buildurl", 5, "cause"));
    }
    @Test
    public void testV1Stage() {
        assertEquals(
                "stage,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch,stagename=mockstage,result=CompletedError stagetime=1,passed=2",
                v1Schema.formatStage("mockjobname", "mockowner", "mockrepo", "mockbranch", "mockstage", "CompletedError", 1, 2, "buildurl", 5, "cause"));
    }
    @Test
    public void testV2Stage() {
        assertEquals(
                "stage,owner=mockowner,repo=mockrepo,stagename=mockstage,result=CompletedError jobname=\"mockjobname\",branch=\"mockbranch\",stagetime=1,passed=2,buildurl=\"buildurl\",buildnumber=5,trigger=\"cause\"",
                v2Schema.formatStage("mockjobname", "mockowner", "mockrepo", "mockbranch", "mockstage", "CompletedError", 1, 2, "buildurl", 5, "cause"));
    }
    @Test
    public void testV1Coverage() {
        assertEquals(
                "coverage,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch classes=1.0000,conditionals=2.0000,files=3.0000,lines=4.0000,methods=5.0000,packages=6.0000",
                v1Schema.formatCoverage("mockjobname", "mockowner", "mockrepo", "mockbranch", 1, 2, 3, 4, 5, 6, 7, "buildurl", 8, "cause"));
    }
    @Test
    public void testV2Coverage() {
        assertEquals(
                "coverage,owner=mockowner,repo=mockrepo jobname=\"mockjobname\",branch=\"mockbranch\",classes=1.0000,conditionals=2.0000,files=3.0000,lines=4.0000,methods=5.0000,packages=6.0000,instructions=7.0000,buildurl=\"buildurl\",buildnumber=8,trigger=\"cause\"",
                v2Schema.formatCoverage("mockjobname", "mockowner", "mockrepo", "mockbranch", 1, 2, 3, 4, 5, 6, 7, "buildurl", 8, "cause"));
    }
    @Test
    public void testV1Tests() {
        assertEquals("tests,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch passed=1,skipped=2,failed=3",
                v1Schema.formatTests("mockjobname", "mockowner", "mockrepo", "mockbranch", 1, 2, 3, "buildurl", 5, "cause" ));

    }
    @Test
    public void testV2Tests() {
        assertEquals("tests,owner=mockowner,repo=mockrepo jobname=\"mockjobname\",branch=\"mockbranch\",passed=1,skipped=2,failed=3,buildurl=\"buildurl\",buildnumber=4,trigger=\"cause\"",
                v2Schema.formatTests("mockjobname", "mockowner", "mockrepo", "mockbranch", 1, 2, 3, "buildurl", 4, "cause" ));

    }
    @Test
    public void testV1TestSuite() {
        assertEquals("testsuite,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch,suite=mocksuite duration=1.0000,passed=2,skipped=3,failed=4",
                v1Schema.formatTestSuite("mockjobname", "mockowner", "mockrepo", "mockbranch", "mocksuite", 1, 2, 3, 4, "buildurl", 5, "cause"  ));
    }
    @Test
    public void testV2TestSuite() {
        assertEquals("testsuite,owner=mockowner,repo=mockrepo,suitename=mocksuite jobname=\"mockjobname\",branch=\"mockbranch\",suite=\"mocksuite\",duration=1.0000,passed=2,skipped=3,failed=4,buildurl=\"buildurl\",buildnumber=5,trigger=\"cause\"",
                v2Schema.formatTestSuite("mockjobname", "mockowner", "mockrepo", "mockbranch", "mocksuite", 1, 2, 3, 4, "buildurl", 5, "cause"  ));
    }
    @Test
    public void testV1TestCase() {
        assertEquals("testcase,jobname=mockjobname,owner=mockowner,repo=mockrepo,branch=mockbranch,suite=mocksuite,testcase=mocktestcase passed=1,skipped=2,failed=3",
                v1Schema.formatTestCase("mockjobname", "mockowner", "mockrepo", "mockbranch", "mocksuite","mocktestcase", 1, 2, 3, "buildurl", 4, "cause"  ));

    }
    @Test
    public void testV2TestCase() {
        assertEquals("testcase,owner=mockowner,repo=mockrepo,suitename=mocksuite jobname=\"mockjobname\",branch=\"mockbranch\",suite=\"mocksuite\",testcase=\"mocktestcase\",passed=1,skipped=2,failed=3,buildurl=\"buildurl\",buildnumber=4,trigger=\"cause\"",
                v2Schema.formatTestCase("mockjobname", "mockowner", "mockrepo", "mockbranch", "mocksuite","mocktestcase", 1, 2, 3, "buildurl", 4, "cause"  ));

    }
}