# YOUR Jenkins image. ~/start-jenkins.sh builds and runs it.
# Plugins listed in plugins.txt are installed at build time; you can still
# install anything else later from Manage Jenkins -> Plugins.
FROM jenkins/jenkins:lts

USER root

# Docker CLI: talks to YOUR rootless Docker through the mounted socket, so
# pipelines can run "docker build". kubectl: deploys to your k8s namespace.
COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:27-cli /usr/local/libexec/docker/cli-plugins/docker-buildx /usr/local/libexec/docker/cli-plugins/docker-buildx
RUN curl -fsSLo /usr/local/bin/kubectl \
      "https://dl.k8s.io/release/$(curl -fsSL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" \
 && chmod 755 /usr/local/bin/kubectl

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN if grep -qvE '^[[:space:]]*(#|$)' /usr/share/jenkins/ref/plugins.txt; then \
      jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt; \
    else \
      echo "plugins.txt has no plugins - install them from the Jenkins web UI instead"; \
    fi
