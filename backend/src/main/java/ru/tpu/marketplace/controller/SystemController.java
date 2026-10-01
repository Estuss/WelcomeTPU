
package ru.tpu.marketplace.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController
{
    private final JdbcTemplate jdbcTemplate;

    public SystemController(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("ping-db")
    public Map<String, Object> pingdb()
    {
        var result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        return Map.of(
            "status",       "ok",
            "database",     "avaible",
            "query_result", result
        );
    }

}
