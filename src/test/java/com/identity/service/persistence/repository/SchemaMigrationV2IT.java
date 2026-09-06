package com.identity.service.persistence.repository;

import com.identity.service.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationV2IT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayHistoryShouldContainV1AndV2() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank");

        assertThat(rows).anySatisfy(row -> {
            assertThat(row.get("version").toString()).startsWith("1");
            assertThat(row.get("success")).isEqualTo(true);
        });
        assertThat(rows).anySatisfy(row -> {
            assertThat(row.get("version").toString()).startsWith("2");
            assertThat(row.get("success")).isEqualTo(true);
        });
    }

    @Test
    void emailColumnShouldBeVarchar320NotNull() {
        Map<String, Object> col = jdbcTemplate.queryForMap(
            "SELECT data_type, character_maximum_length, is_nullable " +
            "FROM information_schema.columns " +
            "WHERE table_name = 'users' AND column_name = 'email'");

        assertThat(col.get("data_type")).isEqualTo("character varying");
        assertThat(col.get("character_maximum_length")).isEqualTo(320);
        assertThat(col.get("is_nullable")).isEqualTo("NO");
    }

    @Test
    void uniqueConstraintShouldExistOnEmail() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT tc.constraint_name, tc.constraint_type, kcu.column_name " +
            "FROM information_schema.table_constraints tc " +
            "JOIN information_schema.key_column_usage kcu " +
            "  ON tc.constraint_name = kcu.constraint_name " +
            "  AND tc.table_schema = kcu.table_schema " +
            "WHERE tc.table_name = 'users' " +
            "  AND tc.constraint_type = 'UNIQUE' " +
            "  AND kcu.column_name = 'email'");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("constraint_name").toString()).isEqualTo("uk_users_email");
    }

    @Test
    void v1ColumnsShouldRemainUnchanged() {
        assertColumn("id", "uuid", null, "NO");
        assertColumn("status", "character varying", null, "NO");
        assertColumn("created_at", null, null, "NO");
        assertColumn("updated_at", null, null, "NO");
    }

    private void assertColumn(String name, String dataType, Integer maxLength, String nullable) {
        Map<String, Object> col = jdbcTemplate.queryForMap(
            "SELECT column_name, data_type, character_maximum_length, is_nullable " +
            "FROM information_schema.columns " +
            "WHERE table_name = 'users' AND column_name = ?",
            name);

        assertThat(col.get("column_name")).isEqualTo(name);
        if (dataType != null) {
            assertThat(col.get("data_type")).isEqualTo(dataType);
        }
        if (maxLength != null) {
            assertThat(col.get("character_maximum_length")).isEqualTo(maxLength);
        }
        assertThat(col.get("is_nullable")).isEqualTo(nullable);
    }
}
