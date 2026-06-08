package com.example.epager.project;

import com.example.epager.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportGroupMemberRepository extends JpaRepository<SupportGroupMember, Long> {

    List<SupportGroupMember> findBySupportGroupAndActiveTrue(SupportGroup supportGroup);

    Optional<SupportGroupMember> findBySupportGroupAndUserAndActiveTrue(SupportGroup supportGroup, AppUser user);
}
