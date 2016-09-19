package pipeline.rails

import pipeline.ApplicationPipeline

public class RailsApplicationPipeline extends ApplicationPipeline {
  String rubyVersion

  RailsApplicationPipeline(String appName, String githubRepoUrl, String rubyVersion) {
    super(appName, githubRepoUrl)
    this.rubyVersion = rubyVersion
  }

  def Map createJobs(dsl) {
    createBaseJob(dsl)
    createBaseRubyJob(dsl)
    [
      'poll-version-control': createPollVersionControlJob,
      'run-static-analysis': createStaticAnalysisJob,
      'run-unit-tests': createUnitTestJob
    ]
  }

  def createBaseRubyJob = { dsl ->
    defineJob(dsl, "base-${appName}-ruby-job-dsl") {
      using "base-${appName}-job-dsl"
      wrappers {
        rvm("${rubyVersion}@${appName}")
        colorizeOutput('xterm')
      }
    }
  }

  def createStaticAnalysisJob = { dsl ->
    defineJob(dsl, fullJobName("run-static-analysis")) {
      using "base-${appName}-ruby-job-dsl"

      publishers {
        publishHtml {
          report('tmp/rubycritic/'){
            reportName('Ruby Critic')
            reportFiles('overview.html')
            keepAll()
          }
        }
      }
    }
  }

  def createUnitTestJob = { dsl ->
    defineJob(dsl, fullJobName("run-unit-tests")) {
      using "base-${appName}-ruby-job-dsl"

      publishers {
        configure { job ->
          job / 'publishers' / 'hudson.plugins.rubyMetrics.rcov.RcovPublisher' {
            reportDir 'coverage/rcov'
            targets {
              'hudson.plugins.rubyMetrics.rcov.model.MetricTarget' {
                metric 'TOTAL_COVERAGE'
                healthy '80'
                unhealthy '0'
                unstable '0'
              }
              'hudson.plugins.rubyMetrics.rcov.model.MetricTarget' {
                metric 'CODE_COVERAGE'
                healthy '80'
                unhealthy '0'
                unstable '0'
              }
            }
          }
        }
      }
    }
  }
}
