package dev.noren.staybooking.property.internal.web.controller;

import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.bootstrap.exception.GlobalExceptionHandler;
import dev.noren.staybooking.bootstrap.pagination.PaginationCriteria;
import dev.noren.staybooking.bootstrap.pagination.SortCriteria;
import dev.noren.staybooking.bootstrap.pagination.SortDirection;
import dev.noren.staybooking.bootstrap.web.mapper.JsonNullablePatchMapper;
import dev.noren.staybooking.bootstrap.web.mapper.MoneyWebMapper;
import dev.noren.staybooking.bootstrap.web.mapper.PaginationWebMapper;
import dev.noren.staybooking.bootstrap.web.parser.SortItemConverter;
import dev.noren.staybooking.bootstrap.web.parser.VersionHeader;
import dev.noren.staybooking.property.internal.application.input.PropertyCatalogCriteria;
import dev.noren.staybooking.property.internal.application.input.PropertyFilterCriteria;
import dev.noren.staybooking.property.internal.application.result.PropertyCatalogResult;
import dev.noren.staybooking.property.internal.application.service.PropertyService;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import dev.noren.staybooking.property.internal.web.mapper.PropertyWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(PropertyFilterControllerTest.MapperConfig.class)
class PropertyFilterControllerTest {

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackageClasses = PropertyWebMapper.class)
    @Import({JsonNullablePatchMapper.class, MoneyWebMapper.class, PaginationWebMapper.class})
    static class MapperConfig {
    }

    @Autowired
    private PropertyWebMapper mapper;

    private PropertyService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PropertyService.class);
        when(service.list(any())).thenReturn(new PropertyCatalogResult(List.of(), 0, 20, 0, 0));

        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        var conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new SortItemConverter());
        mvc = MockMvcBuilders.standaloneSetup(new PropertyController(
                        service,
                        mapper,
                        mock(VersionHeader.class)
                ))
                .setValidator(validator)
                .setConversionService(conversionService)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validFiltersAreNormalizedAndPassedToService() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("city", "  Kyiv  ")
                        .param("minPrice", "100.50")
                        .param("maxPrice", "200.00")
                        .param("minGuests", "2")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(PropertyCatalogCriteria.class);
        verify(service).list(captor.capture());
        assertThat(captor.getValue().filters()).isEqualTo(new PropertyFilterCriteria(
                "Kyiv", Money.of(new BigDecimal("100.50")), Money.of(new BigDecimal("200.00")), 2,
                PropertyStatus.ACTIVE
        ));
        assertThat(captor.getValue().pagination()).isEqualTo(defaultPagination());
    }

    @Test
    void cityLengthIsValidatedAfterTrimming() throws Exception {
        String city = "x".repeat(100);

        mvc.perform(get("/api/properties").param("city", "  " + city + "  "))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(PropertyCatalogCriteria.class);
        verify(service).list(captor.capture());
        assertThat(captor.getValue().filters().city()).isEqualTo(city);

        mvc.perform(get("/api/properties").param("city", "  " + city + "x  "))
                .andExpect(status().isBadRequest());
        verifyNoMoreInteractions(service);
    }

    @Test
    void omittedFiltersRemainOptional() throws Exception {
        mvc.perform(get("/api/properties"))
                .andExpect(status().isOk());

        verify(service).list(new PropertyCatalogCriteria(
                new PropertyFilterCriteria(null, null, null, null, null),
                defaultPagination()
        ));
    }

    @Test
    void paginationAndRepeatedSortArePassedInRequestOrder() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "pricePerNight,asc", "title,desc"))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(PropertyCatalogCriteria.class);
        verify(service).list(captor.capture());
        assertThat(captor.getValue().pagination()).isEqualTo(new PaginationCriteria(
                2, 5, List.of(
                        new SortCriteria("pricePerNight", SortDirection.ASC),
                        new SortCriteria("title", SortDirection.DESC)
                )
        ));
    }

    @Test
    void singleSortIsBoundAsOneSortItem() throws Exception {
        mvc.perform(get("/api/properties").param("sort", "title,asc"))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(PropertyCatalogCriteria.class);
        verify(service).list(captor.capture());
        assertThat(captor.getValue().pagination().sort()).containsExactly(
                new SortCriteria("title", SortDirection.ASC)
        );
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "page | -1",
            "page | abc",
            "size | 0",
            "size | 101",
            "sort | unknown,asc",
            "sort | title,up",
            "sort | title",
            "sort | title,asc,extra"
    })
    void invalidPaginationReturnsBadRequest(String name, String value) throws Exception {
        mvc.perform(get("/api/properties").param(name, value))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void pageOffsetBeyondJpaLimitReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("page", "21474837")
                        .param("size", "100"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void duplicateSortFieldReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("sort", "title,asc", "title,desc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({
            "city, '   '",
            "minPrice, 0",
            "minPrice, 1.234",
            "maxPrice, -1",
            "minGuests, 0",
            "status, UNKNOWN"
    })
    void invalidFiltersReturnBadRequest(String name, String value) throws Exception {
        mvc.perform(get("/api/properties").param(name, value))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void invertedPriceRangeReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("minPrice", "200")
                        .param("maxPrice", "100"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    private PaginationCriteria defaultPagination() {
        return new PaginationCriteria(0, 20, List.of());
    }
}
