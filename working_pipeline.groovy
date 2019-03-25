def releaseName = "${ReleaseName}"
def threshold = "${Threshold}"
def sitResultMap =new HashMap();
def systemTestResultMap =new HashMap();
def smokeTestResultMap =new HashMap();
currentBuild.displayName = releaseName
stage('Reporting SDK Installation') {
    build job:'Reporting SDK Installation'
}
stage('System Tests') {
    build job:'BuildApp', propagate:false, parameters: [string(name: 'BUILD_ID', value: releaseName)]
}
stage('System Test Stage Gate') {
    def buildStatus = build job: 'CollectTestExecutionMetrics', propagate: false, parameters: [string(name: 'VIEW_NAME', value: 'App2.0'), string(name: 'PASS_PERCENTAGE', value: threshold),string(name: 'BUILD_ID', value: releaseName)]
    println 'System Test Result:' + buildStatus.result
    String url = "${BUILD_URL}";
    def result = buildStatus.description;
    def items = result.replace('{','').replace('}','').replace("\"","").split(',')
    for(def item: items) {
        def ritems = item.split(':')
        systemTestResultMap.put(ritems[0], ritems[1])
    }
    if(buildStatus.result.equals("SUCCESS")){
        currentBuild.result = 'SUCCESS'
    } else {
        def totalTests = systemTestResultMap.get('total')
        def passTests = systemTestResultMap.get('pass')
        def failTests = systemTestResultMap.get('fail')
        def skipTests = systemTestResultMap.get('skip')
        def percentage = systemTestResultMap.get('percentage')
        String body ="<html><header><h1>System Test Case Execution Summary</h1></header>" +
"<body><div><p style=\"color:green\">Total Number of Test Cases PASSED :<span>"+passTests+"</span></p><pstyle=\"color:red\">Total Number of Test Cases FAILED :<span>"+failTests+"</span></p>" +
  "<p>Current Threshold limit met :"+threshold+"</p>"+
  "<p>Actual Threshold limit met :"+percentage+"</p>"+
  "<p>PASS Rate has not met the required threshold limit, however you can override the action, <a href=\""+url+"/input/\"/>click here</a> to override</p>"+
"</div><div><p>Regards</p><p>ATOM Test Team</p></div></body></html>";
        mail body: body, mimeType:'text/html', subject: 'System tests - Stage gate alert [Threshold Limit not met - Action Required] ', to: 'yuvaraj.gunisetti@atombank.co.uk'
        String command = "curl \'https://api.twilio.com/2010-04-01/Accounts/xxxx/Messages.json\' -X POST "+
"--data-urlencode 'To=+447340352378' "+
"--data-urlencode 'From=+441293344601' "+
"--data-urlencode 'Body="+buildStatus.description+"' "+
"-u xxxxx:xxxx"
        //sh label: '', returnStdout: true, script: command
        def submitter = input id: 'SystemTestResult', message: 'System tests failed. Do you want to proceed with Smoke test?', submitter: 'admin', submitterParameter: 'message'
        echo submitter + ' has approved the pipeline to proceed to smoke testing'
    }
}

stage('Atom\'s Smoke Tests') {
    build job:'run_sit_mobile_simulator_tests', propagate:false, parameters: [string(name: 'BUILD_ID', value: releaseName)]
}
stage('Smoke Test Stage Gate') {
    def buildStatus = build job: 'CollectTestExecutionMetrics', propagate: false, parameters: [string(name: 'VIEW_NAME', value: 'Smoke Tests'), string(name: 'PASS_PERCENTAGE', value: threshold), string(name: 'BUILD_ID', value: releaseName)]
    println 'Smoke Test Result:' + buildStatus.result
    String url = "${BUILD_URL}";
    def result = buildStatus.description;
    def items = result.replace('{','').replace('}','').replace("\"","").split(',')
    for(def item: items) {
        def ritems = item.split(':')
        smokeTestResultMap.put(ritems[0], ritems[1])
    }
    if(buildStatus.result.equals("SUCCESS")){
        currentBuild.result = 'SUCCESS'
    } else {
        def totalTests = smokeTestResultMap.get('total')
        def passTests = smokeTestResultMap.get('pass')
        def failTests = smokeTestResultMap.get('fail')
        def skipTests = smokeTestResultMap.get('skip')
        def percentage = smokeTestResultMap.get('percentage')
        String body ="<html><header><h1>Smoke Test Case Execution Summary</h1></header>" +
"<body><div><p style=\"color:green\">Total Number of Test Cases PASSED :<span>"+passTests+"</span></p><p style=\"color:red\">Total Number of Test Cases FAILED :<span>"+failTests+"</p>" +
  "<p>Current Threshold limit met :"+threshold+"</p>"+
  "<p>Actual Threshold limit met :"+percentage+"</p>"+
  "<p>PASS Rate has not met the required threshold limit, however you can override the action, <a href=\""+url+"/input/\"/>click here</a> to override</p>"+
   "</div><div><p>Regards</p><p>ATOM Test Team</p></div></body></html>";
        mail body: body, mimeType:'text/html', subject: 'Smoke tests - Stage gate alert [Threshold Limit not met - Action Required] ', to: 'yuvaraj.gunisetti@atombank.co.uk'
        String command = "curl \'https://api.twilio.com/2010-04-01/Accounts/xxxxxxx/Messages.json\' -X POST "+
"--data-urlencode 'To=+447340352378' "+
"--data-urlencode 'From=+441293344601' "+
"--data-urlencode 'Body="+buildStatus.description+"' "+
"-u xxx:xxxx"
        //sh label: '', returnStdout: true, script: command
        def submitter = input id: 'SmokeTestResult', message: 'Smoke Test failed. Do you want to proceed with System Integration test?', submitter: 'admin', submitterParameter: 'message'
        echo submitter + ' has approved the pipeline to proceed to system integration testing'
    }
}
stage('Run API and Atom Tests') {
    parallel (
        'API Tests': {
            stage('API Tests') {
               build job:'APITests', propagate:false, parameters: [string(name: 'BUILD_ID', value: releaseName)]
             }
        },
        'Atom\'s System Integration Tests': {
            stage('Atom\'s System Integration Tests') {
                build job:'Atom_System_Integration_Tests', propagate:false, parameters: [string(name: 'BUILD_ID', value: releaseName)]
            }
        }
    )
}

stage('System Integration Test Stage Gate') {
    def buildStatus = build job: 'CollectTestExecutionMetrics', propagate: false, parameters: [string(name: 'VIEW_NAME', value: 'System Integration Tests'), string(name: 'PASS_PERCENTAGE', value: threshold),string(name: 'BUILD_ID', value: releaseName)]
    println 'System Integration Test Result:' + buildStatus.result
    String url = "${BUILD_URL}";
    def result = buildStatus.description;
    def items = result.replace('{','').replace('}','').replace("\"","").split(',')
    for(def item: items) {
        def ritems = item.split(':')
        sitResultMap.put(ritems[0], ritems[1])
    }
    if(buildStatus.result.equals("SUCCESS")){
        currentBuild.result = 'SUCCESS'
    } else {    
        def totalTests = sitResultMap.get('total')
        def passTests = sitResultMap.get('pass')
        def failTests = sitResultMap.get('fail')
        def skipTests = sitResultMap.get('skip')
        def percentage = sitResultMap.get('percentage')
        String body ="<html><header><h1>System Integration Test Case Execution Summary</h1></header>" +
"<body><div><p style=\"color:green\">Total Number of Test Cases PASSED :<span>"+passTests+"</span></p><p style=\"color:red\">Total Number of Test Cases FAILED :<span>"+failTests+"</span></p>" +
  "<p>Current Threshold limit met :"+threshold+"</p>"+
  "<p>Actual Threshold limit met :"+percentage+"</p>"+
  "<p>PASS Rate has not met the required threshold limit, however you can override the action, <a href=\""+url+"/input/\"/>click here</a> to override</p>"+
"</div><div><p>Regards</p><p>ATOM Test Team</p></div></body></html>";
        mail body: body, mimeType:'text/html', subject: 'System Integration tests - Stage gate alert [Threshold Limit not met - Action Required] ', to: 'yuvaraj.gunisetti@atombank.co.uk'
        String command = "curl \'https://api.twilio.com/2010-04-01/Accounts/xxxxx/Messages.json\' -X POST "+
"--data-urlencode 'To=+447340352378' "+
"--data-urlencode 'From=+441293344601' "+
"--data-urlencode 'Body="+buildStatus.description+"' "+
"-u xxx:xxxx"
        //sh label: '', returnStdout: true, script: command
        def submitter = input id: 'SystemIntegrationTestResult', message: 'System Integration Test failed.Do you wish to provide the sign off for testing?', submitter: 'admin', submitterParameter: 'message'
        echo submitter + ' has signed off testing'   
    }
}
stage('Final Sign off') {
    mail body: getOverAllStatistics(sitResultMap, systemTestResultMap, smokeTestResultMap), mimeType:'text/html', subject: 'Test Execution Sign-off', to: 'yuvaraj.gunisetti@atombank.co.uk'
}
def getOverAllStatistics(sitResultMap, systemTestResultMap, smokeTestResultMap){
    String body = "<html><head><link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css\" integrity=\"sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T\" crossorigin=\"anonymous\"></head>"+
    "<body><div class=\"container\"><h2>Test Execution Sign off</h2><p>admin has signed off testing. Please find the complete test execution summary</p><div><h6>System Test Execution Summary</h6>"+
    "<table class=\"table table-striped\"><tbody><tr><td>Total Number of Test Cases PASSED</td><td>"+systemTestResultMap.get('pass')+
    "</td></tr><tr><td>Total Number of Test Cases FAILED</td><td>"+systemTestResultMap.get('fail')+
    "</td></tr><tr><td>Current Threshold limit met</td><td>"+threshold+"</td></tr><tr><td>Actual Threshold limit met</td><td>"+systemTestResultMap.get('percentage')+
    "</td></tr></tbody></table></div><div><h6>Smoke Test Execution Summary</h6><table class=\"table table-striped\"><tbody><tr><td>Total Number of Test Cases PASSED</td><td>"+smokeTestResultMap.get('pass')+
    "</td></tr><tr><td>Total Number of Test Cases FAILED</td><td>"+smokeTestResultMap.get('fail')+"</td></tr><tr><td>Current Threshold limit met</td><td>"+threshold+"</td></tr><tr><td>Actual Threshold limit met</td><td>"+smokeTestResultMap.get('percentage')+
    "</td></tr></tbody></table></div> <div><h6>System Integration Test Execution Summary</h6><table class=\"table table-striped\"><tbody><tr><td>Total Number of Test Cases PASSED</td><td>"+sitResultMap.get('pass')+
    "</td></tr><tr><td>Total Number of Test Cases FAILED</td><td>"+sitResultMap.get('fail')+"</td></tr><tr><td>Current Threshold limit met</td><td>"+threshold+"</td></tr>"+
    "<tr><td>Actual Threshold limit met</td><td>"+sitResultMap.get('percentage')+"</td></tr></tbody></table></div> <div><p>Regards</p><p>ATOM Test Team</p></div></div></body></html>";
    return body;
}
