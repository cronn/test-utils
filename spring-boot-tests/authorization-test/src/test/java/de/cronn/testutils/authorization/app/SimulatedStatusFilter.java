package de.cronn.testutils.authorization.app;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SimulatedStatusFilter extends OncePerRequestFilter {

	private static final AtomicInteger SIMULATED_STATUS = new AtomicInteger(0);

	public static void simulateStatus(int status) {
		SIMULATED_STATUS.set(status);
	}

	public static void reset() {
		SIMULATED_STATUS.set(0);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		int simulated = SIMULATED_STATUS.get();
		if (simulated > 0) {
			response.setStatus(simulated);
			return;
		}
		chain.doFilter(request, response);
	}
}
