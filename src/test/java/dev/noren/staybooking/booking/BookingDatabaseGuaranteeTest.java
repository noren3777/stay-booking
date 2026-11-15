package dev.noren.staybooking.booking;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class BookingDatabaseGuaranteeTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-bookworm");

    @Test
    void migrationAcceptsExistingValidBookingsAndRejectsConflictingHistoryWithoutChangingIt() throws Exception {
        String valid = schemaAtVersionSix();
        IDs validIds = seed(valid);
        insert(valid, validIds.property(), validIds.guest(), 10, 14, "PENDING_PAYMENT");
        migrate(valid);
        assertThat(count(valid, validIds.property())).isEqualTo(1);
        assertExclusion(valid, validIds.property(), validIds.guest(), 11, 13);

        String invalid = schemaAtVersionSix();
        IDs invalidIds = seed(invalid);
        insert(invalid, invalidIds.property(), invalidIds.guest(), 10, 14, "PENDING_PAYMENT");
        insert(invalid, invalidIds.property(), invalidIds.guest(), 12, 16, "CONFIRMED");
        assertThatThrownBy(() -> migrate(invalid)).satisfies(error -> {
            Throwable root = error;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            assertThat(root).isInstanceOf(PSQLException.class);
            assertExclusionError((PSQLException) root);
        });
        assertThat(count(invalid, invalidIds.property())).isEqualTo(2);
        try (Connection c = connection(invalid); Statement s = c.createStatement();
             ResultSet result = s.executeQuery("select count(*) from pg_constraint where conname = 'ex_bookings_property_occupied_dates' and connamespace = '" + invalid + "'::regnamespace")) {
            result.next();
            assertThat(result.getInt(1)).isZero();
        }
    }

    @Test
    void exclusionCoversIntervalsPropertiesStatusesAndStatusUpdates() throws Exception {
        String schema = schemaAtVersionSix();
        IDs ids = seed(schema);
        migrate(schema);
        insert(schema, ids.property(), ids.guest(), 10, 14, "PENDING_PAYMENT");

        for (int[] range : new int[][]{{10, 14}, {8, 12}, {12, 16}, {8, 16}, {11, 13}}) {
            assertExclusion(schema, ids.property(), ids.guest(), range[0], range[1]);
        }
        insert(schema, ids.property(), ids.guest(), 8, 10, "CONFIRMED");
        insert(schema, ids.property(), ids.guest(), 14, 16, "CONFIRMED");
        insert(schema, ids.otherProperty(), ids.guest(), 10, 14, "CONFIRMED");

        for (String status : new String[]{"CANCELLED", "PAYMENT_FAILED", "COMPLETED"}) {
            UUID id = insert(schema, ids.property(), ids.guest(), 10, 14, status);
            assertThatThrownBy(() -> updateStatus(schema, id, "CONFIRMED"))
                    .isInstanceOf(PSQLException.class)
                    .satisfies(error -> assertExclusionError((SQLException) error));
        }
        assertThat(count(schema, ids.property())).isEqualTo(6);
    }

    @Test
    void changingAnOccupiedBookingToInactiveReleasesItsDates() throws Exception {
        for (String status : new String[]{"CANCELLED", "PAYMENT_FAILED", "COMPLETED"}) {
            String schema = schemaAtVersionSix();
            IDs ids = seed(schema);
            migrate(schema);

            UUID original = insert(schema, ids.property(), ids.guest(), 10, 14, "PENDING_PAYMENT");
            updateStatus(schema, original, status);
            insert(schema, ids.property(), ids.guest(), 10, 14, "CONFIRMED");

            assertThat(count(schema, ids.property())).isEqualTo(2);
        }
    }

    @Test
    void independentTransactionsCompeteForSameRange() throws Exception {
        String schema = schemaAtVersionSix();
        IDs ids = seed(schema);
        migrate(schema);
        CountDownLatch firstInserted = new CountDownLatch(1);
        CountDownLatch secondAttempting = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            Future<String> first = workers.submit(() -> {
                try (Connection c = connection(schema)) {
                    c.setAutoCommit(false);
                    insert(c, ids.property(), ids.guest(), 20, 23, "PENDING_PAYMENT");
                    firstInserted.countDown();
                    assertThat(releaseFirst.await(10, TimeUnit.SECONDS)).isTrue();
                    c.commit();
                    return "committed";
                }
            });
            Future<String> second = workers.submit(() -> competingInsert(schema, ids, firstInserted, secondAttempting));
            try {
                assertThat(secondAttempting.await(10, TimeUnit.SECONDS)).isTrue();
                assertThat(waitForBlockedWriter(schema)).isTrue();
            } finally {
                releaseFirst.countDown();
            }
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo("committed");
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo("23P01");
        }
        assertThat(count(schema, ids.property())).isEqualTo(1);
    }

    private static String competingInsert(String schema, IDs ids, CountDownLatch firstInserted,
                                          CountDownLatch attempting) throws Exception {
        try (Connection c = connection(schema)) {
            c.setAutoCommit(false);
            assertThat(firstInserted.await(10, TimeUnit.SECONDS)).isTrue();
            try (Statement s = c.createStatement()) {
                s.execute("set application_name = 'booking_conflict_waiter'");
            }
            attempting.countDown();
            try {
                insert(c, ids.property(), ids.guest(), 20, 23, "PENDING_PAYMENT");
                throw new AssertionError("The overlapping writer unexpectedly succeeded");
            } catch (SQLException ex) {
                c.rollback();
                assertExclusionError(ex);
                return ex.getSQLState();
            }
        }
    }

    @SuppressWarnings("BusyWait") // Polling PostgreSQL lock state is required to observe actual contention.
    private static boolean waitForBlockedWriter(String schema) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            try (Connection c = connection(schema); Statement s = c.createStatement();
                 ResultSet result = s.executeQuery("select count(*) from pg_stat_activity where application_name = 'booking_conflict_waiter' and wait_event_type = 'Lock'")) {
                result.next();
                if (result.getInt(1) == 1) {
                    return true;
                }
            }
            Thread.sleep(20);
        }
        return false;
    }

    private static void assertExclusion(String schema, UUID property, UUID guest, int in, int out) {
        assertThatThrownBy(() -> insert(schema, property, guest, in, out, "CONFIRMED"))
                .isInstanceOf(PSQLException.class)
                .satisfies(error -> assertExclusionError((SQLException) error));
    }

    private static void assertExclusionError(SQLException error) {
        assertThat(error.getSQLState()).isEqualTo("23P01");
        ServerErrorMessage details = Objects.requireNonNull(
                ((PSQLException) error).getServerErrorMessage(), "PostgreSQL must report constraint details"
        );
        assertThat(details.getConstraint())
                .isEqualTo("ex_bookings_property_occupied_dates");
    }

    private static IDs seed(String schema) throws SQLException {
        UUID host = UUID.randomUUID();
        UUID guest = UUID.randomUUID();
        UUID property = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        try (Connection c = connection(schema); Statement s = c.createStatement()) {
            s.executeUpdate("insert into users values ('" + host + "', now(), now(), 'host-" + host + "@example.com', 'hash', 'HOST')");
            s.executeUpdate("insert into users values ('" + guest + "', now(), now(), 'guest-" + guest + "@example.com', 'hash', 'GUEST')");
            for (UUID id : new UUID[]{property, other}) {
                s.executeUpdate("insert into properties (id, created_at, updated_at, host_id, title, country, city, address, price_per_night, max_guests, status, version) values ('" + id + "', now(), now(), '" + host + "', 'Stay', 'Ukraine', 'Kyiv', 'Street', 100, 2, 'ACTIVE', 0)");
            }
        }
        return new IDs(property, other, guest);
    }

    private static UUID insert(String schema, UUID property, UUID guest, int in, int out, String status) throws SQLException {
        try (Connection c = connection(schema)) {
            return insert(c, property, guest, in, out, status);
        }
    }

    private static UUID insert(Connection c, UUID property, UUID guest, int in, int out, String status) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement s = c.prepareStatement("insert into bookings (id, created_at, updated_at, property_id, guest_id, check_in, check_out, number_of_guests, price_per_night_snapshot, total_price, status) values (?, now(), now(), ?, ?, ?, ?, 1, 100, 400, ?)")) {
            s.setObject(1, id);
            s.setObject(2, property);
            s.setObject(3, guest);
            s.setObject(4, LocalDate.of(2027, 1, in));
            s.setObject(5, LocalDate.of(2027, 1, out));
            s.setString(6, status);
            s.executeUpdate();
        }
        return id;
    }

    private static void updateStatus(String schema, UUID id, String status) throws SQLException {
        try (Connection c = connection(schema); PreparedStatement s = c.prepareStatement("update bookings set status = ? where id = ?")) {
            s.setString(1, status);
            s.setObject(2, id);
            s.executeUpdate();
        }
    }

    private static int count(String schema, UUID property) throws SQLException {
        try (Connection c = connection(schema); PreparedStatement s = c.prepareStatement("select count(*) from bookings where property_id = ?")) {
            s.setObject(1, property);
            try (ResultSet result = s.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static Connection connection(String schema) throws SQLException {
        Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword());
        connection.setSchema(schema);
        return connection;
    }

    private static String schemaAtVersionSix() {
        String schema = "booking_test_" + UUID.randomUUID().toString().replace("-", "");
        flyway(schema, true).migrate();
        return schema;
    }

    private static void migrate(String schema) {
        flyway(schema, false).migrate();
    }

    private static Flyway flyway(String schema, boolean throughSix) {
        var config = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration/identity", "classpath:db/migration/property", "classpath:db/migration/booking");
        if (throughSix) {
            config.target(MigrationVersion.fromVersion("6"));
        }
        return config.load();
    }

    private record IDs(UUID property, UUID otherProperty, UUID guest) {}
}
