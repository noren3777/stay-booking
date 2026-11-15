package dev.noren.staybooking.property.internal.application.service;

import dev.noren.staybooking.property.api.PropertyApi;
import dev.noren.staybooking.property.api.view.PropertyBookingView;
import dev.noren.staybooking.property.internal.application.mapper.PropertyApplicationMapper;
import dev.noren.staybooking.property.internal.persistence.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyApiImpl implements PropertyApi {

    private final PropertyRepository repository;
    private final PropertyApplicationMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<PropertyBookingView> findForBooking(UUID propertyId) {
        return repository.findById(propertyId)
                .map(mapper::toView);
    }

}
