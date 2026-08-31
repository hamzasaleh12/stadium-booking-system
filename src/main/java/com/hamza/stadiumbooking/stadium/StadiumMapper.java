package com.hamza.stadiumbooking.stadium;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StadiumMapper {

    StadiumResponse toResponse(Stadium stadium);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "lastLockAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateStadiumFromRequest(StadiumRequestForUpdate request, @MappingTarget Stadium stadium, @Context StadiumUpdateContext ctx);

    @AfterMapping
    default void applyStadiumUpdateValues(StadiumRequestForUpdate request,
                                         @MappingTarget Stadium stadium,
                                         @Context StadiumUpdateContext ctx) {
        if (request.features() != null && ctx != null) {
            if (stadium.getFeatures() == null || stadium.getFeatures().getClass().getName().contains("Immutable")) {
                stadium.setFeatures(new java.util.HashSet<>());
            }
            stadium.getFeatures().clear();
            stadium.getFeatures().addAll(ctx.features());
        }
    }

    Stadium toEntity(StadiumRequest request);
}
