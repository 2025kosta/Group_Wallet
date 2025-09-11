# Java 17 JDK (빌드+런타임 겸용)
FROM eclipse-temurin:17-jdk AS app

WORKDIR /app

# 의존 라이브러리 (MySQL 커넥터)
COPY lib ./lib
# 소스 코드
COPY src ./src

# UTF-8 콘솔용 설정 (선택)
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# 소스 컴파일: /app/out에 .class 생성
# * 리눅스 find 사용 — 윈도우 경로 구애 X
RUN find ./src -name "*.java" > sources.list \
 && javac -encoding UTF-8 -cp "./lib/*" -d ./out @sources.list \
 && rm sources.list

# 앱이 DB 준비될 때까지 기다리기 위한 간단한 쉘 스크립트
# (mysqladmin ping 기반, healthcheck와 별개)
RUN printf '%s\n' \
'#!/bin/sh' \
'HOST="${DB_HOST:-db}"' \
'PORT="${DB_PORT:-3306}"' \
'echo "Waiting for MySQL at $HOST:$PORT..."' \
'for i in $(seq 1 60); do' \
'  if /bin/sh -c "echo > /dev/tcp/$HOST/$PORT" 2>/dev/null; then' \
'    echo "MySQL port open."; exit 0; fi' \
'  sleep 1' \
'done' \
'echo "Timeout waiting for MySQL"; exit 1' \
> /app/wait-mysql.sh \
 && chmod +x /app/wait-mysql.sh

# 실행: classpath에 out + lib/* 추가, 메인클래스는 main.MainApp 가정
CMD ["java", "-cp", "out:lib/*", "main.MainApp"]
