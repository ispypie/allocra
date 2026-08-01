package com.allocra.app.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Request/response DTOs for the internal booking API. Kept separate from domain
 * types.
 */
public final class ApiModel {

	private ApiModel() {
	}

	// --- availability search ---

	public record SearchRequest(Instant from, Instant to, List<RequirementPreferenceDto> preferences) {
	}

	public record RequirementPreferenceDto(UUID requirementId, List<UUID> requiredResourceIds,
			List<UUID> preferredResourceIds) {
	}

	public record SearchResponse(List<OptionDto> options) {
	}

	public record OptionDto(Instant start, Instant end, List<AssignmentDto> assignments, double score) {
	}

	public record AssignmentDto(UUID requirementId, UUID resourceId) {
	}

	// --- booking confirmation ---

	public record ConfirmRequest(UUID serviceTypeId, Instant start, SubjectDto subject,
			List<AssignmentDto> assignments) {
	}

	public record SubjectDto(String type, String displayName, String email, String phone, String externalRef) {
	}

	public record ConfirmResponse(UUID bookingId, String status) {
	}

	// --- booking reads ---

	public record BookingDto(UUID id, UUID serviceTypeId, SubjectDto subject, Instant start, Instant end, String status,
			String channel, List<BookingAssignmentDto> assignments) {
	}

	public record BookingAssignmentDto(UUID requirementId, UUID resourceId, String policy) {
	}

	public record BookingSummaryDto(UUID id, UUID serviceTypeId, String subjectDisplayName, Instant start, Instant end,
			String status, String channel) {
	}

	public record BookingListResponse(List<BookingSummaryDto> bookings) {
	}

	// --- reschedule ---

	public record RescheduleRequest(Instant start) {
	}
}
