package com.example.epager.user.roster.dto;

import java.time.LocalDate;

public class RosterResponse {

	private Long id;

	private Long userId;

	private String username;

	private Long shiftId;

	private String shiftName;

	private LocalDate rosterDate;

	private Boolean active;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Long getShiftId() {
		return shiftId;
	}

	public void setShiftId(Long shiftId) {
		this.shiftId = shiftId;
	}

	public String getShiftName() {
		return shiftName;
	}

	public void setShiftName(String shiftName) {
		this.shiftName = shiftName;
	}

	public LocalDate getRosterDate() {
		return rosterDate;
	}

	public void setRosterDate(LocalDate rosterDate) {
		this.rosterDate = rosterDate;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

}
