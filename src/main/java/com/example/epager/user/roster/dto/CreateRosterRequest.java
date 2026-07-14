package com.example.epager.user.roster.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public class CreateRosterRequest {

	@NotNull
	private Long userId;

	@NotNull
	private Long shiftId;

	@NotNull
	private LocalDate rosterDate;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getShiftId() {
		return shiftId;
	}

	public void setShiftId(Long shiftId) {
		this.shiftId = shiftId;
	}

	public LocalDate getRosterDate() {
		return rosterDate;
	}

	public void setRosterDate(LocalDate rosterDate) {
		this.rosterDate = rosterDate;
	}

}
