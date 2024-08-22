package de.cronn.testutils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ThreadLeakCheck.class)
class ExecutorServiceExtensionTest {

    @RegisterExtension
    ExecutorServiceExtension executorServiceExtension = new ExecutorServiceExtension(Duration.ofSeconds(10));

    @Test
    void testHappyCase() throws Exception {
        executorServiceExtension.submit(() -> "one");
        executorServiceExtension.submit(() -> "two");
        executorServiceExtension.submit(() -> "three");

        executorServiceExtension.awaitAllFutures();

        List<Future<?>> futures = executorServiceExtension.getFutures();
        assertThat(futures).hasSize(3);
        assertThat(futures)
                .map(Future::get)
                .map(Object::toString)
                .containsExactlyInAnyOrder("one", "two", "three");
    }

}
