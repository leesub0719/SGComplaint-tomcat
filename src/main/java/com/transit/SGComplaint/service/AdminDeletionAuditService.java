package com.transit.SGComplaint.service;

import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.mapper.AdminDeletionAuditMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminDeletionAuditService {
    private final AdminDeletionAuditMapper mapper;

    public AdminDeletionAuditService(AdminDeletionAuditMapper mapper) {
        this.mapper = mapper;
    }

    public void record(Employee administrator, String type, Long targetNo,
                       String summary, Map<String, ?> snapshot) {
        String safeSummary = summary == null ? "" : summary.strip();
        if (safeSummary.length() > 200) safeSummary = safeSummary.substring(0, 200);
        mapper.insert(administrator.getEmpNo(), administrator.getEmpId(), type,
                targetNo, safeSummary, toJson(snapshot));
    }

    private String toJson(Map<String, ?> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append('"').append(escape(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) json.append(value);
            else json.append('"').append(escape(String.valueOf(value))).append('"');
        }
        return json.append('}').toString();
    }

    private String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) escaped.append(String.format("\\u%04x", (int) character));
                    else escaped.append(character);
                }
            }
        }
        return escaped.toString();
    }
}
