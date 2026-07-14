package com.example.epager.user.roster;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.epager.user.roster.dto.CreateRosterRequest;
import com.example.epager.user.roster.dto.RosterResponse;
import com.example.epager.user.roster.dto.UpdateRosterRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rosters")
public class AppUserRosterController {

	private final RosterService rosterService;

	public AppUserRosterController(RosterService rosterService) {
		this.rosterService = rosterService;
	}

	@PostMapping
	public RosterResponse create(@Valid @RequestBody CreateRosterRequest request) {
		return rosterService.create(request);
	}

	@PutMapping("/{id}")
	public RosterResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRosterRequest request) {
		return rosterService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		rosterService.delete(id);
	}

	@GetMapping("/{id}")
	public RosterResponse get(@PathVariable Long id) {
		return rosterService.get(id);
	}

	@GetMapping("/user/{userId}")
	public List<RosterResponse> getByUser(@PathVariable Long userId) {
		return rosterService.getByUser(userId);
	}

	@GetMapping
	public List<RosterResponse> getAll() {
		return rosterService.getAll();
	}
}
