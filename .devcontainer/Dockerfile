# .devcontainer/Dockerfile
FROM mcr.microsoft.com/devcontainers/base:debian

# Install prerequisites early and clean up apt lists
RUN apt-get update \
  && apt-get install -y --no-install-recommends \
     curl \
     zip \
     unzip \
     ca-certificates \
     gnupg2 \
     dirmngr \
     bash \
     procps \
     sudo \
  && rm -rf /var/lib/apt/lists/*

# Ensure /home/vscode exists and is owned by vscode
RUN mkdir -p /home/vscode && chown -R 1000:1000 /home/vscode

# Install SDKMAN and SDKs as the vscode user (non-interactive)
USER 1000
ENV HOME=/home/vscode
ENV SDKMAN_DIR=${HOME}/.sdkman
ENV SDKMAN_NON_INTERACTIVE=true

# Install SDKMAN and candidates; run via bash -lc so profile is sourced
RUN bash -lc 'curl -s "https://get.sdkman.io" | bash' \
  && bash -lc 'source "$SDKMAN_DIR/bin/sdkman-init.sh" \
      && sdk install java 25-graalce \
      && sdk default java 25-graalce \
      && sdk install maven 3.9.11 \
      && sdk flush archives' 

# Expose JAVA_HOME and add candidates to PATH for non-interactive shells
ENV JAVA_HOME=/home/vscode/.sdkman/candidates/java/current
ENV PATH=/home/vscode/.sdkman/candidates/maven/current/bin:${JAVA_HOME}/bin:${PATH}

# Keep runtime user as vscode
USER 1000
ENV HOME=/home/vscode
