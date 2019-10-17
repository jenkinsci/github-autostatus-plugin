/*
The MIT License

Copyright (c) 2018, CloudBees, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun

// check plugin installed and version for github-autostatus

def pluginGitHubAutostatus = Jenkins.instance.pluginManager.activePlugins.find{ it.getShortName() == 'github-autostatus' }
if(!pluginGitHubAutostatus){
  println "You do not have the [github-autostatus] plugin and so does not seem to be vulnerable, if you had installed it, please comment this check"
  return
}

def pluginVersion = pluginGitHubAutostatus?.getVersionNumber()
if(pluginVersion && pluginVersion.isOlderThan(new hudson.util.VersionNumber("3.0.0"))){
  println "The plugin [ghprb:${pluginVersion}] is vulnerable, please upgrade at least to [3.0.0] and re-run this script."
  return
}

println ''

int totalNumberOfBuilds = 0
int totalNumberOfCorrectedBuilds = 0
int totalNumberOfFailedBuilds = 0

Jenkins.instance.getAllItems(WorkflowMultiBranchProject).each { WorkflowMultiBranchProject project ->
  
  println "Project ${project.name}"
  
  project.getAllJobs().each { WorkflowJob job -> 
	boolean affected = false
    WorkflowRun build = job.getLastBuild()
    while(build != null){
		print "\t#${build.number} of type [${build.class.simpleName}] " 

	    totalNumberOfBuilds++
	    List<InvisibleAction> actions = build.getActions(InvisibleAction)
	    if (actions.any{ it instanceof org.jenkinsci.plugins.githubautostatus.BuildStatusAction }) {      
	      print "=> possibly affected"
	      try {
    	    build.save()
        
        	totalNumberOfCorrectedBuilds++
	        println ", re-saved to erase clear-text credentials"
    	  } catch(e) {
	        println ", but save FAILED: ${e.message}"
            e.printStackTrace()
	       	totalNumberOfFailedBuilds++
    	  }  
        } else {
	      println "=> not affected"
        }
          
	    build = build.getPreviousBuild()
    }
  }
}

println "Builds found: ${totalNumberOfBuilds}, affected and corrected: ${totalNumberOfCorrectedBuilds}"
if(totalNumberOfFailedBuilds > 0){
  println "  ${totalNumberOfFailedBuilds} build save failed, please look above for details of which builds was not saved correctly"
}
return
