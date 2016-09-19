package pipeline

abstract class ApplicationPipeline {
  Map jobRegistry = [:]
  def env = System.getenv()

  String appName
  String githubRepoUrl


  Map pipelineSteps = [:]

  ApplicationPipeline(String appName, String githubRepoUrl) {
    this.appName = appName
    this.githubRepoUrl = this.githubRepoUrl
  }

  def stagesAndSteps(stages) {
    def stepNames = []

    stages.each { stage, steps ->
      steps.each { step ->
        stepNames.add([step, stage])
      }
    }
    return stepNames
  }

  def eachPair(items, Closure perPairAction) {
    [*items, null].collate(2, 1, false).each(perPairAction)
  }

  def defineJob(dsl, name, Closure jobDefinition) {
    def createdJob = dsl.job(name, jobDefinition)
    return createdJob
  }

  def fullJobName(stepName) {
    return "${appName}-${stepName}-dsl"
  }

  def getShellScript(jobName) {
    "#!/bin/bash\n${jobName}.sh"
  }

  abstract def Map createJobs(dsl)

  def createNestedView(dsl) {
    dsl.nestedView(appName) {
      views {
        deliveryPipelineView("Build ${appName}") {
          //name("Build ${appName}")

          pipelineInstances(10)
          showAggregatedPipeline(false)
          columns(1)
          updateInterval(2)
          showAvatars(false)
          showChangeLog(false)
          pipelines {
            component("${appName} build pipeline", fullJobName('poll-version-control'))
          }
        }
      }
    }
  }

  def createBaseJob(dsl) {
    defineJob(dsl, "base-${appName}-job-dsl") {
      scm {
        git {
          remote {
            github(githubRepoUrl, 'ssh')
          }
          branch('$revision')
          createTag(false)
        }
      }
    }
  }

  def createDefaultJob(dsl, stepName) {
    defineJob(dsl, fullJobName("${stepName}")) {
      using "base-${appName}-job-dsl"
    }
  }

  def createPollVersionControlJob = { dsl ->
    defineJob(dsl, fullJobName("poll-version-control")) {
      using "base-${appName}-job-dsl"

      parameters {
        stringParam('revision', 'master', 'The revision or branch to build. Defaults to master.')
      }

      scm {
        git {
          remote {
            github(githubRepoUrl, 'ssh')
          }
          branch('${revision}')
          createTag(false)
        }
      }
      triggers {
        scm('* * * * *')
      }
    }
  }

  def setupPipelines(dsl, jobs) {
    createNestedView(dsl)
    jobRegistry << jobs
    pipelineSteps.each { pipeline, stages ->

      def allStepNameStageNamePairs = stagesAndSteps(stages)

      //pair of pairs to be clear
      eachPair(allStepNameStageNamePairs) { currentStepStagePair, nextStepStagePair ->
        def stepName = currentStepStagePair[0]
        def stageName = currentStepStagePair[1]
        def nextStepName = nextStepStagePair == null ? null : nextStepStagePair[0]

        dsl.out.println("Step Name: ${stepName}")
        def currentJob = jobRegistry[stepName](dsl)
        currentJob.deliveryPipelineConfiguration(stageName, stepName)
        currentJob.steps {
          shell(getShellScript(stepName))
        }
        if (nextStepName != null) {
          //figure out the upstream stuff later
          currentJob.publishers {
            downstreamParameterized {
              trigger (fullJobName("${nextStepName}")) {
                condition('ALWAYS')
                parameters{
                  currentBuild()
                  sameNode()
                }
              }
            }
          }
        }
      }
    }
  }
}
