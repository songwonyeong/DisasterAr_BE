package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.BeaconElementMapV4;
import com.example.disaster_ar.domain.v4.BeaconV4;
import com.example.disaster_ar.domain.v4.ClassroomV4;
import com.example.disaster_ar.domain.v4.RoomMapVersionV4;
import com.example.disaster_ar.repository.BeaconElementMapRepositoryV4;
import com.example.disaster_ar.repository.BeaconRepositoryV4;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BeaconAutoMappingService {

    private static final int DEFAULT_THRESHOLD_RSSI = -85;

    private final ObjectMapper objectMapper;
    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final BeaconElementMapRepositoryV4 beaconElementMapRepositoryV4;

    @Transactional
    public void syncForActiveMap(ClassroomV4 classroom) {
        if (classroom == null || classroom.getSchool() == null || classroom.getActiveMapVersion() == null) {
            return;
        }

        RoomMapVersionV4 mapVersion = classroom.getActiveMapVersion();

        if (mapVersion.getFloorsJson() == null || mapVersion.getFloorsJson().isBlank()) {
            return;
        }

        String schoolId = classroom.getSchool().getId();

        Map<Integer, List<Map<String, Object>>> elementsByFloor =
                parseElementsByFloor(mapVersion.getFloorsJson());

        if (elementsByFloor.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, List<Map<String, Object>>> entry : elementsByFloor.entrySet()) {
            Integer floorIndex = entry.getKey();
            List<Map<String, Object>> elements = entry.getValue();

            if (floorIndex == null || elements == null || elements.isEmpty()) {
                continue;
            }

            List<ZoneElement> zones = extractZoneElements(elements);

            if (zones.isEmpty()) {
                continue;
            }

            List<BeaconV4> beacons =
                    beaconRepositoryV4.findBySchool_IdAndFloorIndexOrderByBeaconNoAsc(
                            schoolId,
                            floorIndex
                    );

            for (BeaconV4 beacon : beacons) {
                if (beacon.getX() == null || beacon.getY() == null) {
                    continue;
                }

                ZoneElement matchedZone = findContainingZone(beacon, zones);

                if (matchedZone == null) {
                    continue;
                }

                upsertMapping(classroom, floorIndex, beacon, matchedZone);
            }
        }
    }

    private Map<Integer, List<Map<String, Object>>> parseElementsByFloor(String floorsJson) {
        try {
            Object root = objectMapper.readValue(floorsJson, Object.class);

            List<Map<String, Object>> floors = extractFloors(root);

            Map<Integer, List<Map<String, Object>>> result = new LinkedHashMap<>();

            for (Map<String, Object> floor : floors) {
                List<Map<String, Object>> elements = extractElements(floor);

                Integer floorIndex = resolveFloorIndex(floor, elements);

                if (floorIndex != null && elements != null) {
                    result.put(floorIndex, elements);
                }
            }

            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("구조도 floorsJson 파싱 중 오류가 발생했습니다.", e);
        }
    }

    private Integer resolveFloorIndex(
            Map<String, Object> floor,
            List<Map<String, Object>> elements
    ) {
        Integer floorIndex = firstInteger(
                floor.get("floorIndex"),
                floor.get("floor_index"),
                floor.get("floor"),
                floor.get("index"),
                floor.get("floorNo"),
                floor.get("floorNumber")
        );

        if (floorIndex != null) {
            return floorIndex;
        }

        /*
         * 일부 구조도는 floor 객체가 아니라 element 안에 floor를 넣는다.
         * 예: element.floor = 0
         */
        if (elements != null) {
            for (Map<String, Object> element : elements) {
                floorIndex = firstInteger(
                        element.get("floorIndex"),
                        element.get("floor_index"),
                        element.get("floor"),
                        element.get("index"),
                        element.get("floorNo"),
                        element.get("floorNumber")
                );

                if (floorIndex != null) {
                    return floorIndex;
                }
            }
        }

        return null;
    }

    private Integer firstInteger(Object... values) {
        if (values == null) {
            return null;
        }

        for (Object value : values) {
            Integer parsed = toInteger(value);
            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFloors(Object root) {
        if (root instanceof List<?> list) {
            return (List<Map<String, Object>>) (List<?>) list;
        }

        if (root instanceof Map<?, ?> map) {
            Object floors = map.get("floors");
            if (floors instanceof List<?> list) {
                return (List<Map<String, Object>>) (List<?>) list;
            }
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractElements(Map<String, Object> floor) {
        Object elements = floor.get("elements");

        if (elements instanceof List<?> list) {
            return (List<Map<String, Object>>) (List<?>) list;
        }

        /*
         * 기존 createMapVersionFromChannelSet() 쪽에서는 elementsJson이라는 이름으로 들어갈 수 있음.
         */
        Object elementsJson = floor.get("elementsJson");

        if (elementsJson instanceof List<?> list) {
            return (List<Map<String, Object>>) (List<?>) list;
        }

        return List.of();
    }

    private List<ZoneElement> extractZoneElements(List<Map<String, Object>> elements) {
        List<ZoneElement> zones = new ArrayList<>();

        for (Map<String, Object> element : elements) {
            String id = getString(element, "id");
            String name = firstText(
                    getString(element, "placementName"),
                    getString(element, "name"),
                    getString(element, "label"),
                    getString(element, "elementName"),
                    getString(element, "element_name")
            );

            String zoneType = firstText(
                    getString(element, "zoneType"),
                    getString(element, "elementType"),
                    getString(element, "element_type"),
                    getString(element, "type")
            );

            if (id == null || zoneType == null) {
                continue;
            }

            if (!isZoneType(zoneType)) {
                continue;
            }

            Double x = toDouble(element.get("x"));
            Double y = toDouble(element.get("y"));
            Double width = toDouble(element.get("width"));
            Double height = toDouble(element.get("height"));

            /*
             * 1차 2차 첫 버전은 rect만 자동 판정.
             * x/y/width/height 없는 zone은 자동 판정 불가.
             */
            if (x == null || y == null || width == null || height == null) {
                continue;
            }

            zones.add(new ZoneElement(
                    id,
                    name,
                    normalizeZoneType(zoneType),
                    x,
                    y,
                    width,
                    height
            ));
        }

        return zones;
    }

    private ZoneElement findContainingZone(BeaconV4 beacon, List<ZoneElement> zones) {
        List<ZoneElement> matched = new ArrayList<>();

        for (ZoneElement zone : zones) {
            if (contains(zone, beacon.getX(), beacon.getY())) {
                matched.add(zone);
            }
        }

        if (matched.isEmpty()) {
            return null;
        }

        /*
         * 여러 zone이 겹칠 경우 우선순위.
         * 제한/화재 같은 위험구역을 안전구역보다 우선한다.
         */
        matched.sort(Comparator.comparingInt(z -> zonePriority(z.zoneType())));

        return matched.get(0);
    }

    private boolean contains(ZoneElement zone, Double px, Double py) {
        if (px == null || py == null) {
            return false;
        }

        double left = zone.x();
        double top = zone.y();
        double right = zone.x() + zone.width();
        double bottom = zone.y() + zone.height();

        return px >= left && px <= right && py >= top && py <= bottom;
    }

    private int zonePriority(String zoneType) {
        if (zoneType == null) {
            return 999;
        }

        String upper = zoneType.toUpperCase(Locale.ROOT);

        if (upper.contains("RESTRICT")) {
            return 10;
        }
        if (upper.contains("FIRE") || upper.contains("DISASTER")) {
            return 20;
        }
        if (upper.contains("SAFE")) {
            return 30;
        }

        return 999;
    }

    private void upsertMapping(
            ClassroomV4 classroom,
            Integer floorIndex,
            BeaconV4 beacon,
            ZoneElement zone
    ) {
        BeaconElementMapV4 mapping = beaconElementMapRepositoryV4
                .findByBeacon_Id(beacon.getId())
                .orElseGet(() -> BeaconElementMapV4.builder()
                        .id(UUID.randomUUID().toString())
                        .createdAt(LocalDateTime.now())
                        .thresholdRssi(DEFAULT_THRESHOLD_RSSI)
                        .active(true)
                        .build()
                );

        LocalDateTime now = LocalDateTime.now();

        if (mapping.getCreatedAt() == null) {
            mapping.setCreatedAt(now);
        }

        mapping.setSchool(classroom.getSchool());
        mapping.setFloorIndex(floorIndex);
        mapping.setBeacon(beacon);

        /*
         * legacy element_id는 zone_element_id와 동일하게 유지.
         */
        mapping.setElementId(zone.id());
        mapping.setZoneElementId(zone.id());

        /*
         * 아직 구조도에 별도 beacon marker element를 판별하지 않았으므로 null.
         * 나중에 비콘 마커 element가 생기면 여기에 넣는다.
         */
        mapping.setBeaconElementId(null);

        mapping.setPlacementName(zone.name());
        mapping.setZoneType(zone.zoneType());

        if (mapping.getThresholdRssi() == null) {
            mapping.setThresholdRssi(DEFAULT_THRESHOLD_RSSI);
        }

        if (mapping.getActive() == null) {
            mapping.setActive(true);
        }

        mapping.setUpdatedAt(now);

        beaconElementMapRepositoryV4.save(mapping);
    }

    private boolean isZoneType(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String upper = value.trim().toUpperCase(Locale.ROOT);
        String compact = compactText(value);

        return upper.contains("SAFE_ZONE")
                || upper.contains("FIRE_ZONE")
                || upper.contains("DISASTER_ZONE")
                || upper.contains("RESTRICTED_ZONE")
                || upper.contains("EVACUATION_ZONE")
                || upper.contains("SAFE")
                || upper.contains("FIRE")
                || upper.contains("DISASTER")
                || upper.contains("RESTRICTED")
                || upper.contains("RESTRICT")

                /*
                 * 한글 구역 타입
                 */
                || compact.contains("안전구역")
                || compact.contains("대피구역")
                || compact.contains("재난구역")
                || compact.contains("화재구역")
                || compact.contains("제한구역")
                || compact.contains("출입제한");
    }

    private String normalizeZoneType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String upper = value.trim().toUpperCase(Locale.ROOT);
        String compact = compactText(value);

        /*
         * 안전/대피
         */
        if (upper.contains("SAFE")
                || upper.contains("EVACUATION")
                || compact.contains("안전구역")
                || compact.contains("대피구역")
                || compact.contains("대피소")) {
            return "SAFE_ZONE";
        }

        /*
         * 현재 프로젝트에서는 재난 구역을 화재 구역으로 매핑한다.
         */
        if (upper.contains("FIRE")
                || upper.contains("DISASTER")
                || compact.contains("화재구역")
                || compact.contains("재난구역")) {
            return "FIRE_ZONE";
        }

        /*
         * 제한/출입 제한
         */
        if (upper.contains("RESTRICTED")
                || upper.contains("RESTRICT")
                || compact.contains("제한구역")
                || compact.contains("출입제한")) {
            return "RESTRICTED_ZONE";
        }

        return upper;
    }

    private String compactText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replaceAll("\\s+", "")
                .replace("_", "")
                .replace("-", "")
                .trim();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return null;
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private record ZoneElement(
            String id,
            String name,
            String zoneType,
            Double x,
            Double y,
            Double width,
            Double height
    ) {
    }
}