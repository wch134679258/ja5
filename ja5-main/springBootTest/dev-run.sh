#!/bin/sh
# 后端启动辅助脚本（AI 沙盒专用）
# 作用：清掉沙盒注入的 SERVER__PORT / SERVER_PORT 等变量，
#       否则 Spring Boot 宽松绑定会把 server.port 覆盖成随机端口（实测会跑到 8080）。
# 用法：sh dev-run.sh
set -e

cd "$(dirname "$0")"

# 清除会干扰 Spring 端口绑定的环境变量
unset SERVER_PORT
unset SERVER__PORT
unset SERVER_ADDRESS
unset SERVER__ADDRESS
unset SERVER_HOST
unset SERVER__HOST
export HTTP_PROXY=""
export HTTPS_PROXY=""
export http_proxy=""
export https_proxy=""
unset MAVEN_OPTS

MVN_HOME="${MVN_HOME:-$HOME/.m2/wrapper/dists/apache-maven-3.9.9/977a63e90f436cd6ade95b4c0e10c20c}"

exec java -classpath "$MVN_HOME/boot/plexus-classworlds-2.8.0.jar" \
  -Dclassworlds.conf="$MVN_HOME/bin/m2.conf" \
  -Dmaven.home="$MVN_HOME" \
  -Dmaven.multiModuleProjectDirectory="$PWD" \
  org.codehaus.plexus.classworlds.launcher.Launcher "$@" \
  -Dspring-boot.run.jvmArguments="-Dserver.port=8081"
