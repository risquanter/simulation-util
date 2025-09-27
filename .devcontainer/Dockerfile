# .devcontainer/Dockerfile
FROM mcr.microsoft.com/devcontainers/base:debian

# 1. Install curl & unzip for SDKMAM
RUN apt-get update \
  && apt-get install -y curl zip unzip \
  && rm -rf /var/lib/apt/lists/*

# 2. Install SDKMAN and your SDKs in one RUN (so they exist before extensions load)
ENV SDKMAN_DIR=/home/vscode/.sdkman
RUN curl -s "https://get.sdkman.io" | bash \
  && bash -lc "\
       source $SDKMAN_DIR/bin/sdkman-init.sh && \
       sdk install java 25-graalce && \
       sdk default java 25-graalce \
     "

# 3. Expose JAVA_HOME and put it on PATH
ENV JAVA_HOME=/home/vscode/.sdkman/candidates/java/current
ENV PATH=$JAVA_HOME/bin:${PATH}
