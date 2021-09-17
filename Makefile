
USER_ID ?= $(shell stat -c "%u:%g" .)

clean:
	mvn -B clean
build:
	mvn -B package
build-no-tests:
	mvn -B package -DskipTests=true
unit-tests:
	mvn -B test
integration-tests:
	mvn -B failsafe:integration-test
qa:
	mvn -B pmd:check -Dpmd.printFailingErrors=true
run:
	mvn spring-boot:run
run-jar: build-no-tests
	java -jar target/*.jar

release:
	mvn -B release:prepare release:perform -Darguments="-DskipTests -Dmaven.javadoc.skip=true -Dmaven.deploy.skip=true"


PROJECT = roborace-laps-counter
USER    = pi
service-install:
	mkdir -p /app/${PROJECT}
	chown ${USER}:${USER} /app/${PROJECT}
	touch /var/log/${PROJECT}.log
	chown ${USER}:${USER} /var/log/${PROJECT}.log
	cp install/${PROJECT} /etc/init.d/${PROJECT}
	chmod +x /etc/init.d/${PROJECT}
	update-rc.d ${PROJECT} defaults

service-start:
	sudo service ${PROJECT} start

service-stop:
	sudo service ${PROJECT} stop

service-logs:
	service ${PROJECT} logs

target/roborace-laps-counter.jar:
	make build-no-tests

service-update-jar: target/roborace-laps-counter.jar
	cp target/roborace-laps-counter.jar /app/${PROJECT}/

SERVER ?= pi@192.168.1.200
scp-jar:
	scp target/roborace-laps-counter.jar ${SERVER}:/app/roborace-laps-counter/

