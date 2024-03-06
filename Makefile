
clean:
	./gradlew clean
build:
	./gradlew build
build-no-tests:
	./gradlew build -x test
jar:
	./gradlew jar
tests:
	./gradlew test
run:
	./gradlew bootRun
release:
	./gradlew release

SERVER ?= valery@laps.roborace.org
scp-jar: jar
	scp build/libs/roborace-laps-counter.jar ${SERVER}:/app/roborace-laps-counter/

remote-restart: scp-jar
	ssh ${SERVER} sudo service roborace restart

remote-restart-jr:
	ssh ${SERVER} sudo service jr-roborace restart
