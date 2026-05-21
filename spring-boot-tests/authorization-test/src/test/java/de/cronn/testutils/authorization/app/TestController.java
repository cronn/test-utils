package de.cronn.testutils.authorization.app;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

	@GetMapping("/public")
	public String publicEndpoint() {
		return "public";
	}

	@GetMapping("/authenticated")
	public String authenticated() {
		return "authenticated";
	}

	@GetMapping("/any-role")
	public String anyRole() {
		return "any-role";
	}

	@GetMapping("/admin")
	public String adminGet() {
		return "admin";
	}

	@PostMapping("/admin")
	public String adminPost() {
		return "admin";
	}

	@GetMapping("/user")
	public String user() {
		return "user";
	}

	@GetMapping("/guest-only")
	public String guestOnly() {
		return "guest";
	}

	@GetMapping("/locked")
	public String locked() {
		return "locked";
	}

	@GetMapping("/items/{id}")
	public String getItem(@PathVariable("id") String id) {
		return "item-" + id;
	}

	@DeleteMapping("/items/{id}")
	public void deleteItem(@PathVariable("id") String id) {
	}

	@GetMapping("/regex/{id:[0-9]+}")
	public String regexConstrained(@PathVariable("id") String id) {
		return "regex-" + id;
	}

	@GetMapping("/teapot")
	public ResponseEntity<String> teapot() {
		return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("teapot");
	}

	@GetMapping("/not-found")
	public ResponseEntity<String> notFound() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("missing");
	}

	@GetMapping("/server-error")
	public ResponseEntity<String> serverError() {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("boom");
	}

	@GetMapping("/actuator/info")
	public String actuatorInfo() {
		return "info";
	}

	@RequestMapping("/any-method")
	public String anyMethod() {
		return "any";
	}
}
