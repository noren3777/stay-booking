package dev.noren.staybooking;

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

}
