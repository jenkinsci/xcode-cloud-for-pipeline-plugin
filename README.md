# xcode-cloud-for-pipeline
This is a simple Jenkins plugin that allows you to trigger a build on Xcode Cloud from a Jenkins pipeline.
Triggered during a build step, it will push the current branch to a target branch on Xcode Cloud and trigger a build on that branch.
Which enables you to build from private artifactory, complex build steps, hybrid apps, etc.
## Installation

1. Install the plugin from the Jenkins Update Center
2. Set a target branch for your project in the Xcode Cloud configuration
3. Add a build step to your project to trigger a build on Xcode Cloud
4. Make sure GIT_USERNAME and GIT_PASSWORD are set as environment variables in Jenkins and have matching target branch
   name on both Jenkins and Xcode Cloud

```
#Example Jenkinsfile

pipeline {
    agent any
    environment {
        GIT_USERNAME=credentials('git-username')
        GIT_PASSWORD=credentials('git-password')
    }
    stages{
        stage('Modify Something') {
            steps {
                sh """
                    echo "Hello World" > something
                """
            }
        }
        stage('Push to Xcode Cloud') {
            steps {
                step([$class: 'XcodeCloudForPipelineBuilder', branchName: '__jenkins_only__'])
            }
        }
    }
}

```

