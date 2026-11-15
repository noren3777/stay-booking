package dev.noren.staybooking.property.internal.application.service;

import dev.noren.staybooking.property.internal.application.authorization.PropertyAuthorization;
import dev.noren.staybooking.property.internal.application.mapper.AmenityApplicationMapper;
import dev.noren.staybooking.property.internal.application.result.AmenityResult;
import dev.noren.staybooking.property.internal.domain.entity.Amenity;
import dev.noren.staybooking.property.internal.persistence.exception.AmenityPersistenceExceptionTranslator;
import dev.noren.staybooking.property.internal.persistence.repository.AmenityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityService {

    private final AmenityRepository repository;
    private final PropertyAuthorization authorization;
    private final AmenityPersistenceExceptionTranslator exceptionTranslator;
    private final AmenityApplicationMapper mapper;

    @Transactional(readOnly = true)
    public List<AmenityResult> getAll() {
        List<Amenity> amenities = repository.findAllByOrderByNameAsc();
        return mapper.toResult(amenities);
    }

    @Transactional
    public AmenityResult create(String name) {
        authorization.requireCurrentAdmin();

        try {
            Amenity amenity = repository.saveAndFlush(Amenity.create(name));
            return mapper.toResult(amenity);
        } catch (DataIntegrityViolationException ex) {
            throw exceptionTranslator.translate(ex);
        }
    }
}
