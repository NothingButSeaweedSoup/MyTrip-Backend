#!/bin/bash
# 2.5G 低内存环境启动脚本
# 总内存分配建议:
#   JVM heap  512m
#   Metaspace 128m
#   OS/其他   剩余 ~1.9G (含 MySQL + Redis)

JAVA_OPTS="\
  -Xmx512m -Xms256m \
  -XX:+UseG1GC \
  -XX:MaxMetaspaceSize=128m \
  -XX:+DisableExplicitGC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=./dumps \
  -Djava.awt.headless=true \
  -Dspring.jmx.enabled=false \
  -Dspring.devtools.restart.enabled=false \
"

exec java $JAVA_OPTS -jar target/backend-0.0.1-SNAPSHOT.jar "$@"
