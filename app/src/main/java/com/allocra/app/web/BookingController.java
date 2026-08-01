package com.allocra.app.web;

import com.allocra.app.application.BookingLifecycleService;
import com.allocra.app.application.ConfirmBookingService;
import com.allocra.app.application.ConfirmBookingService.ConfirmCommand;
import com.allocra.app.web.ApiModel.ConfirmRequest;
import com.allocra.app.web.ApiModel.ConfirmResponse;
import com.allocra.app.web.ApiModel.SubjectDto;
import com.allocra.bookings.BookingStatus;
import com.allocra.bookings.BookingSubject;
import com.allocra.common.tenant.TenantContext;
import com.allocra.common.tenant.TenantId;
import com.allocra.membership.Membership;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Booking confirmation and lifecycle endpoints (PRD-BKG-004/008/009).
 * Confirmation returns 201; lifecycle actions return 200. The exception handler
 * maps a reservation conflict to 409, an infeasible request to 422, and an
 * invalid lifecycle transition to 409.
 */
@RestController
@RequestMapping("/v1/bookings")
public class BookingController {

	private final ConfirmBookingService confirmService;
	private final BookingLifecycleService lifecycleService;

	public BookingController(ConfirmBookingService confirmService, BookingLifecycleService lifecycleService) {
		this.confirmService = confirmService;
		this.lifecycleService = lifecycleService;
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
		return new ConfirmResponse(bookingId, BookingStatus.CONFIRMED.name());
	}

	@PostMapping("/{bookingId}/cancel")
	public ConfirmResponse cancel(@PathVariable UUID bookingId,
			@RequestAttribute(TenantAuthFilter.MEMBERSHIP_ATTRIBUTE) Membership membership) {
		lifecycleService.cancel(TenantContext.require(), membership, bookingId);
		return new ConfirmResponse(bookingId, BookingStatus.CANCELLED.name());
	}

	@PostMapping("/{bookingId}/complete")
	public ConfirmResponse complete(@PathVariable UUID bookingId,
			@RequestAttribute(TenantAuthFilter.MEMBERSHIP_ATTRIBUTE) Membership membership) {
		lifecycleService.complete(TenantContext.require(), membership, bookingId);
		return new ConfirmResponse(bookingId, BookingStatus.COMPLETED.name());
	}

	@PostMapping("/{bookingId}/no-show")
	public ConfirmResponse noShow(@PathVariable UUID bookingId,
			@RequestAttribute(TenantAuthFilter.MEMBERSHIP_ATTRIBUTE) Membership membership) {
		lifecycleService.noShow(TenantContext.require(), membership, bookingId);
		return new ConfirmResponse(bookingId, BookingStatus.NO_SHOW.name());
	}

	private static BookingSubject subject(SubjectDto dto) {
		return new BookingSubject(dto.type(), dto.displayName(), dto.email(), dto.phone(), dto.externalRef());
	}
}
