package com.hamza.stadiumbooking.booking;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {

    @Mapping(target = "stadiumId", source = "stadium.id")
    @Mapping(target = "stadiumName", source = "stadium.name")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    BookingResponse toResponse(Booking booking);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "stadium", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "startTime", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    void updateBookingFromRequest(BookingRequestForUpdate request, @MappingTarget Booking booking, @Context BookingUpdateContext ctx);

    @AfterMapping
    default void applyComputedBookingValues(BookingRequestForUpdate request,
                                           @MappingTarget Booking booking,
                                           @Context BookingUpdateContext ctx) {
        if (request.startTime() != null && ctx != null) {
            booking.setStartTime(ctx.newStartTime());
        }
        if (request.endTime() != null && ctx != null) {
            booking.setEndTime(ctx.newEndTime());
        }
        if (request.stadiumId() != null && ctx != null) {
            booking.setStadium(ctx.targetStadium());
        }
    }

    Booking toEntity(BookingRequest request);
}
