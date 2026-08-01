package com.allocra.app.web;

import com.allocra.app.application.ConfirmBookingService;
import com.allocra.app.application.ConfirmBookingService.ConfirmCommand;
import com.allocra.app.web.ApiModel.AssignmentDto;
import com.allocra.app.web.ApiModel.ConfirmRequest;
import com.allocra.app.web.ApiModel.ConfirmResponse;
import com.allocra.app.web.ApiModel.SubjectDto;
import com.allocra.bookings.BookingSubject;
import com.allocra.common.tenant.TenantContext;
import com.allocra.common.tenant.TenantId;
import com.allocra.membership.Membership;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Booking confirmation endpoint (PRD-BKG-004). Returns 201 on success; the
 * exception handler maps a reservation conflict to 409 and an infeasible
 * request to 422.
 */
@RestController
@RequestMapping("/v1/bookings")
public class BookingController {

	private final ConfirmBookingService confirmService;

	public BookingController(ConfirmBookingService confirmService) {
		this.confirmService = confirmService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ConfirmResponse confirm(@RequestBody ConfirmRequest request,
			@RequestAttribute(TenantAuthFilter.MEMBERSHIP_ATTRIBUTE) Membership membership) {
		TenantId tenantId = TenantContext.require();
		Map<UUID, UUID> assignments = new LinkedHashMap<>();
		if (request.assignments() != null) {
			request.assignments().forEach(a -> assignments.put(a.requirementId(), a.resourceId()));
		}
		UUID bookingId = confirmService.confirm(tenantId, membership,
				new ConfirmCommand(request.serviceTypeId(), request.start(), subject(request.subject()), assignments));
		return new ConfirmResponse(bookingId, "CONFIRMED");
	}

	private static BookingSubject subject(SubjectDto dto) {
		return new BookingSubject(dto.type(), dto.displayName(), dto.email(), dto.phone(), dto.externalRef());
	}
}
