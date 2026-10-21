#!/usr/bin/env bash
# Git Bash 下可用的 Maven 启动器（规避 MSYS 路径转换问题）
MVNH="C://Users//Administrator//.m2//wrapper//dists//apache-maven-3.9.9-bin//4nf9hui3q3djbarqar9g711ggc//apache-maven-3.9.9"
PROJ="C://Users//Administrator//Desktop//ja3-main//ja3-main//ja4//ja3-main//springBootTest"
cd "/c/Users/Administrator/Desktop/ja3-main/ja3-main/ja4/ja3-main/springBootTest" || exit 1
MSYS_NO_PATHCONV=1 exec java \
  -classpath "$MVNH\\boot\\plexus-classworlds-2.8.0.jar" \
  "-Dclassworlds.conf=$MVNH\\bin\\m2.conf" \
  "-Dmaven.home=$MVNH" \
  "-Dmaven.multiModuleProjectDirectory=$PROJ" \
  org.codehaus.plexus.classworlds.launcher.Launcher "$@"
