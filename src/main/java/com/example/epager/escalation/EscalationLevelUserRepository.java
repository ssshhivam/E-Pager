package com.example.epager.escalation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.epager.user.AppUser;

public interface EscalationLevelUserRepository extends JpaRepository<EscalationLevelUser, Long> {

	List<EscalationLevelUser> findByEscalationLevelId(Long levelId);

	@Query("""
			    select elu.user
			    from EscalationLevelUser elu
			    where elu.escalationLevel.id = :levelId
			""")
	List<AppUser> findUsersByEscalationLevelId(@Param("levelId") Long levelId);
}