package com.example.epager.user.roster;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shifts")
public class Shift {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String shiftName;

	@Column(nullable = false)
	private LocalTime startTime;

	@Column(nullable = false)
	private LocalTime endTime;

	@Column(nullable = false)
	private Boolean active = true;

	private java.time.LocalDateTime createdOn;

	private java.time.LocalDateTime updatedOn;

	public Shift() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getShiftName() {
		return shiftName;
	}

	public void setShiftName(String shiftName) {
		this.shiftName = shiftName;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public java.time.LocalDateTime getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(java.time.LocalDateTime createdOn) {
		this.createdOn = createdOn;
	}

	public java.time.LocalDateTime getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(java.time.LocalDateTime updatedOn) {
		this.updatedOn = updatedOn;
	}
}
