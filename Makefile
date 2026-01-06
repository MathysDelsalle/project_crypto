.PHONY: test test-api test-collector

test: test-api test-collector

test-api:
	mvn -f api/pom.xml clean verify jacoco:report

test-collector:
	mvn -f collector/pom.xml clean verify jacoco:report
