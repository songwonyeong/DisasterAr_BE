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

    public record SyncResult(
            int floorsProcessed,
            int beaconsProcessed,
            int mappingsCreated,
            int mappingsUpdated,
            int mappingsDeactivated,
            int unmatchedBeacons
    ) {
        public static SyncResult empty() {
            return new SyncResult(0, 0, 0, 0, 0, 0);
        }
    }

    @Transactional
    public SyncResult syncForActiveMap(ClassroomV4 classroom) {
        if (classroom == null || classroom.getSchool() == null || classroom.getActiveMapVersion() == null) {
            return SyncResult.empty();
        }

        RoomMapVersionV4 mapVersion = classroom.getActiveMapVersion();

        if (mapVersion.getFloorsJson() == null || mapVersion.getFloorsJson().isBlank()) {
            return SyncResult.empty();
        }

        String schoolId = classroom.getSchool().getId();

        Map<Integer, List<Map<String, Object>>> elementsByFloor =
                parseElementsByFloor(mapVersion.getFloorsJson());

        if (elementsByFloor.isEmpty()) {
            return SyncResult.empty();
        }

        int floorsProcessed = 0;
        int beaconsProcessed = 0;
        int mappingsCreated = 0;
        int mappingsUpdated = 0;
        int mappingsDeactivated = 0;
        int unmatchedBeacons = 0;

        for (Map.Entry<Integer, List<Map<String, Object>>> entry : elementsByFloor.entrySet()) {
            Integer floorIndex = entry.getKey();

            if (floorIndex == null) {
                continue;
            }

            List<Map<String, Object>> elements =
                    entry.getValue() != null ? entry.getValue() : List.of();

            floorsProcessed++;

            List<ZoneElement> zones = extractZoneElements(elements);
            Set<String> currentZoneElementIds = toZoneIdSet(zones);

            List<BeaconV4> beacons =
                    beaconRepositoryV4.findBySchool_IdAndFloorIndexOrderByBeaconNoAsc(
                            schoolId,
                            floorIndex
                    );

            Set<String> matchedBeaconIds = new HashSet<>();

            for (BeaconV4 beacon : beacons) {
                beaconsProcessed++;

                if (beacon.getX() == null || beacon.getY() == null) {
                    unmatchedBeacons++;
                    continue;
                }

                ZoneElement matchedZone = findContainingZone(beacon, zones);

                if (matchedZone == null) {
                    unmatchedBeacons++;
                    continue;
                }

                String beaconElementId = findBeaconElementId(beacon, elements);

                boolean created = upsertMapping(
                        classroom,
                        floorIndex,
                        beacon,
                        matchedZone,
                        beaconElementId
                );

                if (created) {
                    mappingsCreated++;
                } else {
                    mappingsUpdated++;
                }

                matchedBeaconIds.add(beacon.getId());
            }

            /*
             * 핵심 수정:
             * 현재 활성 구조도에 더 이상 존재하지 않는 zoneElementId 매핑은 inactive 처리한다.
             *
             * 주의:
             * 여기서는 "매칭 실패한 모든 매핑"을 끄지 않는다.
             * 프로젝트에 수동 매핑 API가 있으므로, 수동으로 현재 구조도 zone에 연결한 매핑까지
             * sync 때 꺼버리면 안 된다.
             */
            mappingsDeactivated += deactivateMappingsMissingFromCurrentMap(
                    schoolId,
                    floorIndex,
                    currentZoneElementIds,
                    matchedBeaconIds
            );
        }

        return new SyncResult(
                floorsProcessed,
                beaconsProcessed,
                mappingsCreated,
                mappingsUpdated,
                mappingsDeactivated,
                unmatchedBeacons
        );
    }

    private Map<Integer, List<Map<String, Object>>> parseElementsByFloor(String floorsJson) {
        try {
            Object root = objectMapper.readValue(floorsJson, Object.class);

            List<Map<String, Object>> floors = extractFloors(root);

            Map<Integer, List<Map<String, Object>>> result = new LinkedHashMap<>();

            for (Map<String, Object> floor : floors) {
                List<Map<String, Object>> elements = extractElements(floor);

                Integer floorIndex = resolveFloorIndex(floor, elements);

                if (floorIndex != null) {
                    result.put(
                            floorIndex,
                            elements != null ? elements : List.of()
                    );
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

    private List<Map<String, Object>> extractFloors(Object root) {
        if (root instanceof List<?> list) {
            return toMapList(list);
        }

        if (root instanceof Map<?, ?> map) {
            Object floors = map.get("floors");
            return toMapList(floors);
        }

        return List.of();
    }

    private List<Map<String, Object>> extractElements(Map<String, Object> floor) {
        Object elements = firstNonNull(
                floor.get("elements"),
                floor.get("elementsJson"),
                floor.get("elements_json")
        );

        if (elements instanceof String text) {
            try {
                elements = objectMapper.readValue(text, Object.class);
            } catch (Exception e) {
                return List.of();
            }
        }

        return toMapList(elements);
    }

    private List<Map<String, Object>> toMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }

            Map<String, Object> converted = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            result.add(converted);
        }

        return result;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }

        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private List<ZoneElement> extractZoneElements(List<Map<String, Object>> elements) {
        List<ZoneElement> zones = new ArrayList<>();

        for (Map<String, Object> element : elements) {
            /*
             * 비콘 마커는 위치 표시용 element이지 zone이 아니다.
             * 이걸 zone 후보로 넣으면 beacon 좌표가 자기 beacon element 안에 들어가면서
             * zone_element_id = beacon-... / zone_type = BEACON 으로 잘못 저장된다.
             */
            if (isBeaconElement(element)) {
                continue;
            }

            String id = firstText(
                    getString(element, "id"),
                    getString(element, "elementId"),
                    getString(element, "element_id")
            );

            String name = firstText(
                    getString(element, "placementName"),
                    getString(element, "name"),
                    getString(element, "label"),
                    getString(element, "elementName"),
                    getString(element, "element_name")
            );

            String rawZoneType = resolveAutoMappableZoneType(element);
            String zoneType = normalizeZoneType(rawZoneType);

            if (id == null || zoneType == null) {
                continue;
            }

            Double x = toDouble(element.get("x"));
            Double y = toDouble(element.get("y"));
            Double width = firstDouble(element.get("width"), element.get("w"));
            Double height = firstDouble(element.get("height"), element.get("h"));

            /*
             * 현재 자동 매핑은 rect 기준으로만 판정한다.
             * x/y/width/height 없는 zone은 자동 판정 불가.
             */
            if (x == null || y == null || width == null || height == null) {
                continue;
            }

            zones.add(new ZoneElement(
                    id,
                    name,
                    zoneType,
                    x,
                    y,
                    width,
                    height
            ));
        }

        return zones;
    }

    private String resolveAutoMappableZoneType(Map<String, Object> element) {
        if (element == null) {
            return null;
        }

        /*
         * zoneType이 명시되어 있으면 이것을 최우선으로 사용한다.
         * 정상 예: FIRE_ZONE, SAFE_ZONE, RESTRICTED_ZONE
         */
        String explicitZoneType = firstText(
                getString(element, "zoneType"),
                getString(element, "zone_type")
        );

        if (isAutoMappableZoneType(explicitZoneType)) {
            return explicitZoneType;
        }

        /*
         * 구버전 데이터 호환용 fallback.
         * 단, whitelist에 정확히 들어오는 값만 허용한다.
         * 방/비콘/건물윤곽은 여기서 제외된다.
         */
        String fallbackType = firstText(
                getString(element, "type"),
                getString(element, "elementType"),
                getString(element, "element_type")
        );

        if (isAutoMappableZoneType(fallbackType)) {
            return fallbackType;
        }

        return null;
    }

    private boolean isBeaconElement(Map<String, Object> element) {
        if (element == null) {
            return false;
        }

        if (getString(element, "serverBeaconId") != null
                || getString(element, "server_beacon_id") != null
                || getString(element, "beaconId") != null
                || getString(element, "beacon_id") != null
                || getString(element, "beaconUuid") != null
                || getString(element, "beacon_uuid") != null
                || element.get("beaconNo") != null
                || element.get("beacon_no") != null
                || element.get("beaconMajor") != null
                || element.get("beacon_major") != null
                || element.get("beaconMinor") != null
                || element.get("beacon_minor") != null) {
            return true;
        }

        String type = firstText(
                getString(element, "elementType"),
                getString(element, "element_type"),
                getString(element, "type")
        );

        if (type == null) {
            return false;
        }

        String upper = type.trim().toUpperCase(Locale.ROOT);
        String compact = compactText(type);

        return upper.equals("BEACON")
                || upper.contains("BEACON")
                || compact.equals("비콘");
    }

    private boolean isAutoMappableZoneType(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String upper = value.trim().toUpperCase(Locale.ROOT);
        String compact = compactText(value);
        String compactUpper = compact.toUpperCase(Locale.ROOT);

        /*
         * 명시적 제외. 이 값들은 zone이 아니다.
         */
        if (upper.equals("BEACON")
                || compact.equals("비콘")
                || compact.equals("방")
                || compact.equals("건물윤곽")
                || compactUpper.equals("ROOM")
                || compactUpper.equals("WALL")
                || compactUpper.equals("OUTLINE")) {
            return false;
        }

        /*
         * 자동 매핑 대상은 zone 계열만 허용한다.
         * contains가 아니라 exact match 중심으로 제한한다.
         */
        return upper.equals("SAFE_ZONE")
                || upper.equals("FIRE_ZONE")
                || upper.equals("DISASTER_ZONE")
                || upper.equals("RESTRICTED_ZONE")
                || upper.equals("EVACUATION_ZONE")
                || upper.equals("SAFE")
                || upper.equals("FIRE")
                || upper.equals("DISASTER")
                || upper.equals("RESTRICTED")
                || upper.equals("EVACUATION")
                || compact.equals("안전구역")
                || compact.equals("대피구역")
                || compact.equals("재난구역")
                || compact.equals("화재구역")
                || compact.equals("제한구역")
                || compact.equals("출입제한구역");
    }

    private Double firstDouble(Object... values) {
        if (values == null) {
            return null;
        }

        for (Object value : values) {
            Double parsed = toDouble(value);
            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    private Set<String> toZoneIdSet(List<ZoneElement> zones) {
        Set<String> result = new HashSet<>();

        if (zones == null) {
            return result;
        }

        for (ZoneElement zone : zones) {
            if (zone.id() != null && !zone.id().isBlank()) {
                result.add(zone.id().trim());
            }
        }

        return result;
    }

    private int deactivateMappingsMissingFromCurrentMap(
            String schoolId,
            Integer floorIndex,
            Set<String> currentZoneElementIds,
            Set<String> matchedBeaconIds
    ) {
        List<BeaconElementMapV4> activeMappings =
                beaconElementMapRepositoryV4.findBySchool_IdAndFloorIndexAndActiveTrue(
                        schoolId,
                        floorIndex
                );

        int deactivated = 0;
        LocalDateTime now = LocalDateTime.now();

        for (BeaconElementMapV4 mapping : activeMappings) {
            String beaconId = mapping.getBeacon() != null
                    ? mapping.getBeacon().getId()
                    : null;

            /*
             * 이번 sync에서 정상 매칭된 비콘은 유지한다.
             */
            if (beaconId != null && matchedBeaconIds.contains(beaconId)) {
                continue;
            }

            String zoneElementId = trimToNull(mapping.getEffectiveZoneElementId());

            /*
             * 핵심:
             * 현재 활성 구조도에 없는 zoneElementId면 stale mapping으로 본다.
             *
             * 현재 케이스:
             * auto-room-14는 새 구조도에 없음
             * → inactive 처리됨
             */
            if (zoneElementId == null || !currentZoneElementIds.contains(zoneElementId)) {
                mapping.setActive(false);
                mapping.setUpdatedAt(now);
                beaconElementMapRepositoryV4.save(mapping);
                deactivated++;
            }
        }

        return deactivated;
    }

    private String findBeaconElementId(
            BeaconV4 beacon,
            List<Map<String, Object>> elements
    ) {
        if (beacon == null || elements == null) {
            return null;
        }

        for (Map<String, Object> element : elements) {
            String elementId = firstText(
                    getString(element, "id"),
                    getString(element, "elementId"),
                    getString(element, "element_id")
            );

            if (elementId == null) {
                continue;
            }

            /*
             * 현재 데이터에 있는 값:
             * serverBeaconId = 993e1cc7-99a2-46a0-aa9a-3eb431b81228
             */
            String serverBeaconId = firstText(
                    getString(element, "serverBeaconId"),
                    getString(element, "server_beacon_id"),
                    getString(element, "beaconId"),
                    getString(element, "beacon_id")
            );

            if (serverBeaconId != null && serverBeaconId.equals(beacon.getId())) {
                return elementId;
            }

            Integer beaconNo = firstInteger(
                    element.get("beaconNo"),
                    element.get("beacon_no")
            );

            if (beaconNo != null
                    && beacon.getBeaconNo() != null
                    && Objects.equals(beaconNo, beacon.getBeaconNo())) {
                return elementId;
            }

            String uuid = firstText(
                    getString(element, "beaconUuid"),
                    getString(element, "beacon_uuid"),
                    getString(element, "uuid")
            );

            Integer major = firstInteger(
                    element.get("beaconMajor"),
                    element.get("beacon_major"),
                    element.get("major")
            );

            Integer minor = firstInteger(
                    element.get("beaconMinor"),
                    element.get("beacon_minor"),
                    element.get("minor")
            );

            if (uuid != null
                    && beacon.getUuid() != null
                    && uuid.equalsIgnoreCase(beacon.getUuid())
                    && Objects.equals(major, beacon.getMajor())
                    && Objects.equals(minor, beacon.getMinor())) {
                return elementId;
            }
        }

        return null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
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

    private boolean upsertMapping(
            ClassroomV4 classroom,
            Integer floorIndex,
            BeaconV4 beacon,
            ZoneElement zone,
            String beaconElementId
    ) {
        Optional<BeaconElementMapV4> existing =
                beaconElementMapRepositoryV4.findByBeacon_Id(beacon.getId());

        boolean created = existing.isEmpty();

        BeaconElementMapV4 mapping = existing.orElseGet(() -> BeaconElementMapV4.builder()
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
         * 구조도 JSON 안에 비콘 마커 element가 있으면 저장한다.
         * 현재 데이터 기준:
         * beaconElementId = beacon-1779967677882-cvvy
         */
        mapping.setBeaconElementId(beaconElementId);

        mapping.setPlacementName(zone.name());
        mapping.setZoneType(zone.zoneType());

        if (mapping.getThresholdRssi() == null) {
            mapping.setThresholdRssi(DEFAULT_THRESHOLD_RSSI);
        }

        /*
         * 중요:
         * 기존 코드는 active가 null일 때만 true로 만들었다.
         * 그러면 과거에 inactive 처리된 row가 다시 매칭돼도 살아나지 않을 수 있다.
         */
        mapping.setActive(true);

        mapping.setUpdatedAt(now);

        beaconElementMapRepositoryV4.save(mapping);

        return created;
    }

    private String normalizeZoneType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String upper = value.trim().toUpperCase(Locale.ROOT);
        String compact = compactText(value);

        if (upper.equals("BEACON")
                || compact.equals("비콘")
                || compact.equals("방")
                || compact.equals("건물윤곽")) {
            return null;
        }

        if (upper.equals("SAFE_ZONE")
                || upper.equals("SAFE")
                || upper.equals("EVACUATION_ZONE")
                || upper.equals("EVACUATION")
                || compact.equals("안전구역")
                || compact.equals("대피구역")
                || compact.equals("대피소")) {
            return "SAFE_ZONE";
        }

        if (upper.equals("FIRE_ZONE")
                || upper.equals("FIRE")
                || upper.equals("DISASTER_ZONE")
                || upper.equals("DISASTER")
                || compact.equals("화재구역")
                || compact.equals("재난구역")) {
            return "FIRE_ZONE";
        }

        if (upper.equals("RESTRICTED_ZONE")
                || upper.equals("RESTRICTED")
                || compact.equals("제한구역")
                || compact.equals("출입제한구역")) {
            return "RESTRICTED_ZONE";
        }

        return null;
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