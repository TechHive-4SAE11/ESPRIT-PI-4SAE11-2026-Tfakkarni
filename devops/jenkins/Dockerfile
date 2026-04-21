FROM jenkins/jenkins:lts-jdk17

# Switch to root to install system packages
USER root

# Install Docker CLI and Docker Compose, and configure Git safe directory
RUN apt-get update && \
    apt-get install -y docker.io docker-compose && \
    rm -rf /var/lib/apt/lists/* && \
    git config --global --add safe.directory '*'

# Switch back to the jenkins user
USER jenkins

# Skip initial setup wizard
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

# Tell Jenkins where to find the JCasC config
ENV CASC_JENKINS_CONFIG="/var/jenkins_home/casc.yaml"

# Install essential plugins
RUN jenkins-plugin-cli --plugins configuration-as-code job-dsl git workflow-aggregator docker-workflow sonar nodejs maven-plugin docker-plugin
