[![CI](https://github.com/cronn/test-utils/workflows/CI/badge.svg)](https://github.com/cronn/test-utils/actions)
[![Valid Gradle Wrapper](https://github.com/cronn/test-utils/workflows/Validate%20Gradle%20Wrapper/badge.svg)](https://github.com/cronn/test-utils/actions/workflows/gradle-wrapper-validation.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.cronn/test-utils/badge.svg)](http://maven-badges.herokuapp.com/maven-central/de.cronn/test-utils)
[![Apache 2.0](https://img.shields.io/github/license/cronn/test-utils.svg)](http://www.apache.org/licenses/LICENSE-2.0)

# cronn test-utils

Utility classes for JUnit 5 tests.

## Basic features

Add the following dependency to your project:

Gradle:
```groovy
testImplementation "de.cronn:test-utils:{version}"
```

Maven:
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>{version}</version>
    <scope>test</scope>
</dependency>
```

### JUnit5MisusageCheck

JUnit 5 lifecycle annotations (`@BeforeAll`, `@BeforeEach`, `@Test`, `@AfterEach`, `@AfterAll`) require that annotated methods are not overridden or hidden; otherwise they are silently ignored. `JUnit5MisusageCheck` detects such situations and fails fast instead of hiding the problem.

The preferred way to register this extension is [automatic registration for all tests](https://junit.org/junit5/docs/current/user-guide/#extensions-registration-automatic):
 - Add a `META-INF/services/org.junit.jupiter.api.extension.Extension` file containing `de.cronn.testutils.JUnit5MisusageCheck`
 - Add `junit.jupiter.extensions.autodetection.enabled=true` to your [`junit-platform.properties`](https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params)

If a subclass accidentally overrides a `@BeforeEach` method without re-annotating it, `JUnit5MisusageCheck` will throw an exception during test setup and report which method is affected.

### TestInfoExtension

`TestInfoExtension` implements `TestInfo` and can be registered as a field in your test class, avoiding the need to inject `TestInfo` as a method parameter in every lifecycle method.

```java
class MyTest {

    @RegisterExtension
    TestInfoExtension testInfo = new TestInfoExtension();

    @Test
    void example() {
        assertThat(testInfo.getDisplayName()).isEqualTo("example()");
    }
}
```

### TestClock

`TestClock` is an implementation of `Clock` with a fixed time useful for testing. It is initialized to `2016-01-01T00:00:00.123456Z` by default and does not advance on its own. Use `changeInstant()`, `windClock()`, or `reset()` to control its time.

```java
@Bean
TestClock testClock() {
    return new TestClock();
}

@Test
void orderExpiresAfterOneDay() {
    Order order = orderService.create();
    testClock.windClock(Duration.ofDays(1));
    assertThat(order.isExpired()).isTrue();
}
```

### ConcurrentTest

`ConcurrentTest` provides support for tests that need to run on multiple threads. It handles the overhead of submitting and evaluating a given number of tasks. It uses `ExecutorServiceUtils` to ensure that the `ExecutorService` is properly shut down and the task queue is cleared so that subsequent tests run without interference.

```java
class RegistrationServiceTest {

    @RegisterExtension
    TestInfoExtension testInfo = new TestInfoExtension();

    @Test
    void registrationIsThreadSafe() throws Exception {
        ConcurrentTest.create((int index) -> userService.register("user-" + index))
            .withConcurrencyLevel(20)
            .withThreadNamePrefix(testInfo.getDisplayName())
            .runAndAssertEachResult(user -> assertThat(user.getId()).isNotNull());
    }
}
```

Setting a thread name prefix is useful for debugging: log output from worker threads will include the prefix, making it easier to attribute log lines to a specific test. `withThreadNamePrefixFromClass()` is a shorthand when the class name is sufficient.

## Optional features

Some features have additional dependencies that are not included by default. Declare the relevant Gradle capability or Maven classifier to pull them in.

### 🗄️ H2 support

`H2Util` brings an H2 database back to a clean state between tests. Typical usage in a `@BeforeEach` callback:

```java
@BeforeEach
void cleanupDatabase(@Autowired H2Util h2Util) {
    h2Util.resetDatabase();
}
```

Empty state is defined as:
- Empty tables, sequences restarted for `resetDatabase()`
- Empty database schema for `dropAllObjects()`

`H2Util` is automatically provided as a Spring bean via auto-configuration when the h2-support feature and a single `DataSource` bean are present on the classpath. No manual `@Bean` definition or `@Import` is needed.

> [!NOTE]
> Hibernate can cache sequence values in advance. You may want to disable this by setting [`hibernate.id.optimizer.pooled.preferred`](https://docs.jboss.org/hibernate/orm/6.5/userguide/html_single/Hibernate_User_Guide.html#settings-mapping) to `NONE`, for example via `spring.jpa.properties.hibernate.id.optimizer.pooled.preferred=NONE`.

Gradle:
```groovy
testImplementation("de.cronn:test-utils:{version}") {
    capabilities {
        requireCapability("de.cronn:test-utils-h2-support")
    }
}
```

Maven:
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>{version}</version>
    <scope>test</scope>
    <classifier>h2-support</classifier>
</dependency>
```

### 🌱 Spring support

`ResetClockExtension` is a convenient JUnit 5 extension for Spring-based integration tests: it automatically resets a `TestClock` bean found in the test `ApplicationContext` before each test.

Gradle:
```groovy
testImplementation("de.cronn:test-utils:{version}") {
    capabilities {
        requireCapability("de.cronn:test-utils-spring-support")
    }
}
```

Maven:
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>{version}</version>
    <scope>test</scope>
    <classifier>spring-support</classifier>
</dependency>
```
