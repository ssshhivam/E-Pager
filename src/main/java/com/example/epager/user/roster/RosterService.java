package com.example.epager.user.roster;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import com.example.epager.user.roster.dto.CreateRosterRequest;
import com.example.epager.user.roster.dto.RosterResponse;
import com.example.epager.user.roster.dto.UpdateRosterRequest;

@Service
public class RosterService {

	private final ShiftRepository shiftRepository;
	private final AppUserRosterRepository rosterRepository;
	private final AppUserRepository appUserRepository;

	public RosterService(ShiftRepository shiftRepository, AppUserRosterRepository rosterRepository,
			AppUserRepository appUserRepository) {
		this.shiftRepository = shiftRepository;
		this.rosterRepository = rosterRepository;
		this.appUserRepository = appUserRepository;
	}

	public Shift getCurrentShift() {

		return shiftRepository.findCurrentShift(LocalTime.now())
				.orElseThrow(() -> new RuntimeException("No active shift found."));
	}

	public List<AppUser> getCurrentShiftUsers() {

		Shift currentShift = getCurrentShift();

		return rosterRepository.findUsersByShift(currentShift.getId());
	}

	public List<AppUser> getCurrentShiftUsers(List<AppUser> escalationUsers) {

		if (escalationUsers == null || escalationUsers.isEmpty()) {
			return List.of();
		}

		List<AppUser> currentShiftUsers = getCurrentShiftUsers();

		Set<Long> currentShiftUserIds = currentShiftUsers.stream().map(AppUser::getId).collect(Collectors.toSet());

		return escalationUsers.stream().filter(user -> currentShiftUserIds.contains(user.getId())).toList();
	}

	public RosterResponse create(CreateRosterRequest request) {

		if (rosterRepository.existsByUserIdAndRosterDate(request.getUserId(), request.getRosterDate())) {

			throw new RuntimeException("User is already assigned to a shift on " + request.getRosterDate());
		}

		AppUser user = appUserRepository.findById(request.getUserId())
				.orElseThrow(() -> new RuntimeException("AppUser not found"));

		Shift shift = shiftRepository.findById(request.getShiftId())
				.orElseThrow(() -> new RuntimeException("Shift not found"));

		AppUserRoster roster = new AppUserRoster();

		roster.setUser(user);
		roster.setShift(shift);
		roster.setRosterDate(request.getRosterDate());

		roster.setActive(true);

		roster.setCreatedOn(LocalDateTime.now());

		roster.setUpdatedOn(LocalDateTime.now());

		roster = rosterRepository.save(roster);

		return convert(roster);
	}

	public RosterResponse update(Long rosterId, UpdateRosterRequest request) {

		AppUserRoster roster = rosterRepository.findById(rosterId)
				.orElseThrow(() -> new RuntimeException("Roster not found"));

		Shift shift = shiftRepository.findById(request.getShiftId())
				.orElseThrow(() -> new RuntimeException("Shift not found"));

		boolean duplicate = rosterRepository.existsByUserIdAndRosterDate(roster.getUser().getId(),
				request.getRosterDate());

		if (duplicate && !roster.getRosterDate().equals(request.getRosterDate())) {

			throw new RuntimeException("User already has a roster for " + request.getRosterDate());
		}

		roster.setShift(shift);
		roster.setRosterDate(request.getRosterDate());

		if (request.getActive() != null) {
			roster.setActive(request.getActive());
		}

		roster.setUpdatedOn(LocalDateTime.now());

		roster = rosterRepository.save(roster);

		return convert(roster);
	}

	public void delete(Long rosterId) {

		AppUserRoster roster = rosterRepository.findById(rosterId)
				.orElseThrow(() -> new RuntimeException("Roster not found"));

		roster.setActive(false);

		roster.setUpdatedOn(LocalDateTime.now());

		rosterRepository.save(roster);
	}

	private RosterResponse convert(AppUserRoster roster) {

		RosterResponse response = new RosterResponse();
		response.setId(roster.getId());
		response.setUserId(roster.getUser().getId());
		response.setUsername(roster.getUser().getName());
		response.setShiftId(roster.getShift().getId());
		response.setShiftName(roster.getShift().getShiftName());
		response.setRosterDate(roster.getRosterDate());
		response.setActive(roster.getActive());
		return response;
	}

	@Transactional(readOnly = true)
	public RosterResponse get(Long rosterId) {

		AppUserRoster roster = rosterRepository.findById(rosterId)
				.orElseThrow(() -> new RuntimeException("Roster not found"));

		return convert(roster);
	}

	@Transactional(readOnly = true)
	public List<RosterResponse> getAll() {

		return rosterRepository.findAll().stream().map(this::convert).toList();
	}

	@Transactional(readOnly = true)
	public List<RosterResponse> getByUser(Long userId) {

		return rosterRepository.findByUserIdOrderByRosterDateDesc(userId).stream().map(this::convert).toList();
	}

}