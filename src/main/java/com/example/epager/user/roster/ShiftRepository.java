package com.example.epager.user.roster;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

	Optional<Shift> findByShiftNameIgnoreCase(String shiftName);

	List<Shift> findByActiveTrueOrderByStartTime();

	Optional<Shift> findByIdAndActiveTrue(Long id);

	@Query("""
			select s
			from Shift s
			where s.active = true
			and (
			    (
			        s.startTime <= s.endTime
			        and :currentTime between s.startTime and s.endTime
			    )
			    or
			    (
			        s.startTime > s.endTime
			        and (
			            :currentTime >= s.startTime
			            or :currentTime <= s.endTime
			        )
			    )
			)
			""")
	Optional<Shift> findCurrentShift(@Param("currentTime") LocalTime currentTime);

}