package com.example.epager.user.roster;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.epager.user.AppUser;

public interface AppUserRosterRepository extends JpaRepository<AppUserRoster, Long> {

	Optional<AppUserRoster> findByUserIdAndRosterDateAndActiveTrue(Long userId, LocalDate rosterDate);

	List<AppUserRoster> findByRosterDateAndActiveTrue(LocalDate rosterDate);

	List<AppUserRoster> findByUserIdOrderByRosterDateDesc(Long userId);

	List<AppUserRoster> findByShiftIdAndRosterDateAndActiveTrue(Long shiftId, LocalDate rosterDate);

	boolean existsByUserIdAndShiftIdAndRosterDate(Long userId, Long shiftId, LocalDate rosterDate);

	boolean existsByUserIdAndRosterDate(Long userId, LocalDate rosterDate);

	List<AppUserRoster> findByRosterDate(LocalDate rosterDate);

	List<AppUserRoster> findByActiveTrue();

	@Query("""
			select r.user
			from AppUserRoster r
			where r.shift.id = :shiftId
			and r.rosterDate = :rosterDate
			and r.active = true
			""")
	List<AppUser> findUsersByShift(@Param("shiftId") Long shiftId, @Param("rosterDate") LocalDate rosterDate);

	@Query("""
			select r
			from AppUserRoster r
			join fetch r.user
			join fetch r.shift
			where r.rosterDate = :rosterDate
			and r.active = true
			order by r.shift.startTime
			""")
	List<AppUserRoster> getRoster(@Param("rosterDate") LocalDate rosterDate);

}