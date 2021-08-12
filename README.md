[![CI](https://github.com/cronn/test-utils/workflows/CI/badge.svg)](https://github.com/cronn/test-utils/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.cronn/test-utils/badge.svg)](http://maven-badges.herokuapp.com/maven-central/de.cronn/test-utils)
[![Apache 2.0](https://img.shields.io/github/license/cronn/test-utils.svg)](http://www.apache.org/licenses/LICENSE-2.0)

# cronn test-utils

Utility classes for JUnit 5 tests.

### JUnit5MisusageCheck

JUnit5 `@BeforeAll`, `@BeforeEach`, `@Test`, `@AfterEach` and `@AfterAll` require that annotated methods are not overriden/hidden, otherwise they are silently ignored. JUnit5MisusageCheck is a simple way to detect such situations and fail fast rather than sweep it under the carpet.

Preferred way to use this extension is to [automatically register it for all tests](https://junit.org/junit5/docs/current/user-guide/#extensions-registration-automatic):
 - add `META-INF/services/org.junit.jupiter.api.extension.Extension` file containing `de.cronn.testutils.JUnit5MisusageCheck`
 - add `junit.jupiter.extensions.autodetection.enabled=true` property (for example to [`junit-platform.properties`](https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params) file)


## Usage
Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```
