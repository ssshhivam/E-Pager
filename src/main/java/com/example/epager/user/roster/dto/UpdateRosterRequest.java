package com.example.epager.user.roster.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public class UpdateRosterRequest {

	@NotNull
	private Long shiftId;

	@NotNull
	private LocalDate rosterDate;

	private Boolean active;

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

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

}
