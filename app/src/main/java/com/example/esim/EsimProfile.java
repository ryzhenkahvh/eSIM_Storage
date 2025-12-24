package com.example.esim;

import java.util.HashMap;
import java.util.Map;

public class EsimProfile {
    public String id;
    public String name;
    public String activationCode;
    public String matchingId;

    public EsimProfile(String id, String name, String activationCode, String matchingId) {
        this.id = id;
        this.name = name;
        this.activationCode = activationCode;
        this.matchingId = matchingId;
    }

    // Для новых профилей
    public EsimProfile(String name, String activationCode, String matchingId) {
        this(null, name, activationCode, matchingId);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("activationCode", activationCode);
        map.put("matchingId", matchingId);
        return map;
    }

    public static EsimProfile fromMap(String id, Map<String, Object> map) {
        String name = (String) map.get("name");
        String activationCode = (String) map.get("activationCode");
        String matchingId = (String) map.get("matchingId");
        return new EsimProfile(id, name, activationCode, matchingId);
    }
}