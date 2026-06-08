package com.example.epager.notification;

import com.example.epager.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    List<UserDevice> findByUserAndActiveTrue(AppUser user);
}
