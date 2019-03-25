
import jenkins.*;
import jenkins.model.*;
import hudson.*;
import hudson.model.* 
def j = Jenkins.instance
def view = j.getView("${VIEW_NAME}")
def percentageThreshold = Float.valueOf("${PASS_PERCENTAGE}")
float totalTestPassPercentage = 0.0;
view.items.each { item -> 
  def testResultAction = item.getLastBuild().getTestResultAction();
  if (testResultAction) {
    int totalCount = testResultAction.getTotalCount()
    int totalfailCount = testResultAction.getFailCount()
    int totalSkipCount = testResultAction.getSkipCount()
    int totalPassCount = totalCount - (totalfailCount+totalSkipCount)
    println totalPassCount
    float passPercentage = (totalPassCount/totalCount)*100
    totalTestPassPercentage = totalTestPassPercentage+ passPercentage
  }
}
if(totalTestPassPercentage < percentageThreshold) {
    def build = Thread.currentThread().executable
    build.setDescription("The test execution pass rate has not met the required threshold. Actual Percentage:"+totalTestPassPercentage);
    build.result = 'FAILURE'

}
