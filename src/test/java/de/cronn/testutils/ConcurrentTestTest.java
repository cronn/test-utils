package de.cronn.testutils;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

class ConcurrentTestTest {

	@Test
	@Timeout(30)
	void shouldSubmitAndAssert() throws ExecutionException, InterruptedException {
		AtomicInteger atomicInteger = new AtomicInteger(0);
		Set<Integer> set = new HashSet<>();

		ConcurrentTest.create(index -> atomicInteger.addAndGet(1))
			.withThreadNamePrefixFromClass(ConcurrentTestTest.class)
			.withConcurrencyLevel(100)
			.runAndAssertEachResult(result -> {
				assertThat(set).doesNotContain(result);
				set.add(result);
			});

		assertThat(set).hasSize(100);
		assertThat(atomicInteger.get()).isEqualTo(100);
	}

	@Test
	@Timeout(30)
	void shouldRunConcurrentThreads() throws ExecutionException, InterruptedException {
		CyclicBarrier cyclicBarrier = new CyclicBarrier(100);
		ConcurrentTest.create(index -> {
				try {
					cyclicBarrier.await(3000, TimeUnit.MILLISECONDS);
					return true;
				} catch (BrokenBarrierException | TimeoutException e) {
					throw new RuntimeException(e);
				}
			})
			.withThreadNamePrefixFromClass(ConcurrentTestTest.class)
			.withConcurrencyLevel(100)
			.runAndAssertEachResult(result -> assertThat(result).isTrue());
		assertThat(cyclicBarrier.isBroken()).isFalse();
		assertThat(cyclicBarrier.getNumberWaiting()).isZero();
	}
}
