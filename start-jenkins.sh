#!/bin/bash
# Builds YOUR Jenkins image (with any plugins listed in ~/jenkins/plugins.txt)
# and starts it on your own port: student07 -> http://<sandbox-ip>:8107
#
# Safe to re-run at any time, e.g. after editing plugins.txt. Jobs, users and
# settings live in the "jenkins_home" Docker volume and are kept.
set -euo pipefail
set -a; . /opt/sandbox/lab.env; set +a
PORT="81${USER#student}"
RUNTIME_DIR="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}"

cd "$HOME/jenkins"
echo "Building your Jenkins image (the first build takes a few minutes)..."
docker build -t my-jenkins .

KUBE_MOUNT=()
if [ -f "$HOME/.kube/config" ]; then
  KUBE_MOUNT=(-v "$HOME/.kube/config:/kube/config:ro")
else
  echo "NOTE: ~/.kube/config is missing, so Jenkins can't deploy to Kubernetes yet."
  echo "      Ask your instructor, then run this script again."
fi

docker rm -f jenkins > /dev/null 2>&1 || true
# --user root: in rootless Docker, "root" in the container is just YOU on the
# host, and it lets Jenkins use your Docker socket to build images.
docker run -d --name jenkins --restart unless-stopped \
  --user root \
  --memory 1200m \
  -p "${PORT}:8080" \
  -e JAVA_OPTS="-Xmx640m -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true" \
  -e KUBECONFIG=/kube/config \
  -e K8S_PUBLIC_IP="$K8S_PUBLIC_IP" \
  -e REGISTRY="$REGISTRY" \
  -e STUDENT_USER="$USER" \
  -v jenkins_home:/var/jenkins_home \
  -v "${RUNTIME_DIR}/docker.sock:/var/run/docker.sock" \
  -v "$HOME/sample-app:/repos/sample-app:ro" \
  -v "$HOME/springboot-app:/repos/springboot-app:ro" \
  "${KUBE_MOUNT[@]}" \
  my-jenkins > /dev/null

echo "Waiting for Jenkins to start..."
for _ in $(seq 1 60); do
  CODE=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${PORT}/login" || true)
  case "$CODE" in 000|502|503) ;; *) break ;; esac
  sleep 3
done

echo ""
echo "Your Jenkins: http://${SANDBOX_PUBLIC_IP}:${PORT}"
if docker exec jenkins test -f /var/jenkins_home/secrets/initialAdminPassword 2> /dev/null; then
  echo "First-time setup - unlock it with this password:"
  docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
else
  echo "Log in with the admin account you created during setup."
fi

