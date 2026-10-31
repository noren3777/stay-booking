package dev.noren.staybooking;

import com.tngtech.archunit.core.domain.JavaClass;
import dev.noren.staybooking.property.api.PropertyApi;
import dev.noren.staybooking.property.api.view.PropertyBookingView;
import dev.noren.staybooking.property.internal.application.service.PropertyApiImpl;
import dev.noren.staybooking.property.internal.persistence.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

public class ModularityTest {

    @Test
    public void testModularity() {
        ApplicationModules.of(StayBookingApplication.class).verify();
    }

    @Test
    public void detectsExpectedModules() {
        var modules = ApplicationModules.of(StayBookingApplication.class);

        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .containsExactlyInAnyOrder(
                        "identity",
                        "property",
                        "booking",
                        "payment",
                        "review",
                        "bootstrap"
                );
    }

    @Test
    public void propertyApiExposesOnlyBookingContract() {
        var property = ApplicationModules.of(StayBookingApplication.class)
                .getModuleByName("property").orElseThrow();
        var api = property.getNamedInterfaces().getByName("api").orElseThrow();

        assertThat(api.asJavaClasses().map(JavaClass::getName))
                .containsExactlyInAnyOrder(
                        PropertyApi.class.getName(),
                        PropertyBookingView.class.getName()
                );
    }

    @Test
    public void propertyImplementationAndRepositoryRemainInternal() {
        var property = ApplicationModules.of(StayBookingApplication.class)
                .getModuleByName("property").orElseThrow();

        assertThat(property.isExposed(PropertyApiImpl.class))
                .as("PropertyApiImpl must remain internal")
                .isFalse();
        assertThat(property.isExposed(PropertyRepository.class))
                .as("PropertyRepository must remain internal")
                .isFalse();
    }

}
