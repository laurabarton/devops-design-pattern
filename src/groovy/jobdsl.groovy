import pipeline.rails.RailsApplicationPipeline
import pipeline.ApplicationPipeline

appName='some-app'
githubRepoUrl='some-github-repo/${appName}'
rubyVersion='2.3.1'

// Create Rails Framework class
ApplicationPipeline pipeline = new RailsApplicationPipeline(appName, rubyVersion, githubRepoUrl)

//Create nested view
pipeline.pipelineSteps << [
  "Build ${appName}": [
    'commit': [
      'poll-version-control',
      'run-static-analysis',
      'run-unit-tests'
    ]
  ]
]
def jobs = pipeline.createJobs(this)
pipeline.setupPipelines(this, jobs)
