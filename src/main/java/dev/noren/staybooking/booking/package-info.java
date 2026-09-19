@ApplicationModule(
        displayName = "Booking",
        allowedDependencies = {
                "payment::api",
                "property::api"
        }
)
package dev.noren.staybooking.booking;

import org.springframework.modulith.ApplicationModule;
