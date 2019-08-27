
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
	mvn -B verify
qa:
	mvn -B pmd:check -Dpmd.printFailingErrors=true
run:
	mvn spring-boot:run
run-jar: build-no-tests
	java -jar target/*.jar

release:
	mvn -B release:prepare release:perform
