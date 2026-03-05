/*
 * See the documentation for more options:
 * https://github.com/jenkins-infra/pipeline-library/
 */
buildPlugin(
  // run this number of tests in parallel for faster feedback.
  // If the number terminates with a 'C', the value will be
  // multiplied by the number of available CPU cores:
  forkCount: '1C',

  // Set to `false` if you need to use Docker for containerized tests:
  useContainerAgent: true,

  configurations: [
    // Test the common case (i.e., a recent LTS release) on both Linux and Windows
    // with same core version as the lowest baseline requested by pom.xml
    [ platform: 'linux', jdk: '17' ],
    [ platform: 'windows', jdk: '21' ],

    // Test the bleeding edge of the compatibility spectrum (i.e., the latest supported Java runtime).
    // see also https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/
    [ platform: 'linux', jdk: '25', jenkins: '2.541.1' ],
  ]
)
