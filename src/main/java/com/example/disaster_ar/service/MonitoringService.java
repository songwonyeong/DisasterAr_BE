package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.BeaconState;
import com.example.disaster_ar.dto.monitoring.*;
import com.example.disaster_ar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final ClassroomRepository classroomRepository;
    private final StudentRepositoryV4 studentRepositoryV4;
    private final BeaconElementMapRepositoryV4 beaconElementMapRepositoryV4;
    private final ObjectMapper objectMapper;

    public MonitoringMapResponse getMonitoringMap(String classroomId) {

        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        RoomMapVersionV4 mapVersion = classroom.getActiveMapVersion();

        if (mapVersion == null) {
            throw new IllegalArgumentException("활성 구조도가 없습니다.");
        }

        if (mapVersion.getFloorsJson() == null || mapVersion.getFloorsJson().isBlank()) {
            throw new IllegalArgumentException("활성 구조도의 floorsJson이 비어 있습니다.");
        }

        List<StudentV4> students = studentRepositoryV4
                .findByClassroom_IdOrderByJoinedAtAsc(classroom.getId());

        Map<String, List<StudentV4>> studentsByBeaconId = students.stream()
                .filter(s -> s.getLastBeacon() != null)
                .filter(s -> s.getBeaconState() == BeaconState.DETECTED)
                .collect(Collectors.groupingBy(s -> s.getLastBeacon().getId()));

        List<MonitoringFloorResponse> floors = parseFloors(
                mapVersion.getFloorsJson(),
                classroom,
                studentsByBeaconId
        );

        return MonitoringMapResponse.builder()
                .classroomId(classroom.getId())
                .mapVersionId(mapVersion.getId())
                .floors(floors)
                .build();
    }

    private List<MonitoringFloorResponse> parseFloors(
            String floorsJson,
            ClassroomV4 classroom,
            Map<String, List<StudentV4>> studentsByBeaconId
    ) {
        try {
            Object root = objectMapper.readValue(floorsJson, Object.class);

            List<?> floorList;

            if (root instanceof Map<?, ?> rootMap && rootMap.get("floors") instanceof List<?> list) {
                floorList = list;
            } else if (root instanceof List<?> list) {
                floorList = list;
            } else {
                return List.of();
            }

            List<MonitoringFloorResponse> result = new ArrayList<>();

            for (Object floorObj : floorList) {
                if (!(floorObj instanceof Map<?, ?> floor)) {
                    continue;
                }

                String floorLabel = asString(firstNonNull(
                        floor.get("floorLabel"),
                        floor.get("floor_label"),
                        floor.get("name")
                ));

                MonitoringImageResponse image = parseImage(floor);

                List<Map<String, Object>> elements = parseElements(floor);

                Integer floorIndex = resolveFloorIndex(floor, elements);

                List<BeaconMarkerResponse> beaconMarkers = buildBeaconMarkers(
                        classroom,
                        floorIndex,
                        elements,
                        studentsByBeaconId
                );

                result.add(
                        MonitoringFloorResponse.builder()
                                .floorIndex(floorIndex)
                                .floorLabel(floorLabel)
                                .image(image)
                                .elements(elements)
                                .beaconMarkers(beaconMarkers)
                                .build()
                );
            }

            return result;

        } catch (Exception e) {
            throw new IllegalArgumentException("구조도 JSON 파싱 실패", e);
        }
    }

    private Integer resolveFloorIndex(
            Map<?, ?> floor,
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
         * 일부 구조도 JSON은 floor 객체가 아니라 element 안에 floor 값을 넣는다.
         * 현재 배포 데이터가 이 케이스:
         * floor.floorIndex = null
         * element.floor = 0
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
            Integer parsed = asInteger(value);
            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    private MonitoringImageResponse parseImage(Map<?, ?> floor) {
        Object imageObj = firstNonNull(
                floor.get("image"),
                floor.get("imageUrl"),
                floor.get("uploadedImage"),
                floor.get("uploaded_image")
        );

        if (imageObj instanceof Map<?, ?> imageMap) {
            String src = asString(firstNonNull(
                    imageMap.get("src"),
                    imageMap.get("url")
            ));

            Double naturalWidth = null;
            Double naturalHeight = null;

            Object naturalObj = imageMap.get("natural");
            if (naturalObj instanceof Map<?, ?> naturalMap) {
                naturalWidth = asDouble(firstNonNull(
                        naturalMap.get("w"),
                        naturalMap.get("width")
                ));
                naturalHeight = asDouble(firstNonNull(
                        naturalMap.get("h"),
                        naturalMap.get("height")
                ));
            }

            return MonitoringImageResponse.builder()
                    .src(src)
                    .naturalWidth(naturalWidth)
                    .naturalHeight(naturalHeight)
                    .build();
        }

        String src = asString(imageObj);

        return MonitoringImageResponse.builder()
                .src(src)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseElements(Map<?, ?> floor) {
        Object elementsObj = firstNonNull(
                floor.get("elements"),
                floor.get("elementsJson"),
                floor.get("elements_json")
        );

        if (elementsObj instanceof String elementsString) {
            try {
                elementsObj = objectMapper.readValue(elementsString, Object.class);
            } catch (Exception e) {
                return List.of();
            }
        }

        if (!(elementsObj instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> elements = new ArrayList<>();

        for (Object obj : list) {
            if (obj instanceof Map<?, ?> map) {
                Map<String, Object> converted = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    converted.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                elements.add(converted);
            }
        }

        return elements;
    }

    private List<BeaconMarkerResponse> buildBeaconMarkers(
            ClassroomV4 classroom,
            Integer floorIndex,
            List<Map<String, Object>> elements,
            Map<String, List<StudentV4>> studentsByBeaconId
    ) {
        if (classroom.getSchool() == null || floorIndex == null) {
            return List.of();
        }

        /*
         * monitoring-map에서는 active 매핑만 내려준다.
         */
        List<BeaconElementMapV4> mappings =
                beaconElementMapRepositoryV4.findBySchool_IdAndFloorIndexAndActiveTrue(
                        classroom.getSchool().getId(),
                        floorIndex
                );

        List<BeaconMarkerResponse> markers = new ArrayList<>();

        for (BeaconElementMapV4 mapping : mappings) {
            if (mapping.getBeacon() == null) {
                continue;
            }

            if (!mapping.isEffectivelyActive()) {
                continue;
            }

            BeaconV4 beacon = mapping.getBeacon();

            /*
             * 1차 핵심:
             * elementId / zoneElementId는 구역 기준.
             * beaconElementId는 비콘 마커 위치 기준.
             */
            String zoneElementId = trimToNull(mapping.getEffectiveZoneElementId());

            if (zoneElementId == null) {
                continue;
            }

            String beaconElementId = trimToNull(mapping.getBeaconElementId());

            Map<String, Object> zoneElement = findElementById(elements, zoneElementId);

            /*
             * 핵심 방어:
             * DB에 active mapping이 남아 있어도,
             * 현재 활성 구조도에 없는 zoneElementId면 monitoring-map에 내려주지 않는다.
             *
             * 현재 케이스:
             * zoneElementId = auto-room-14
             * 현재 활성 구조도에는 auto-room-14 없음
             * → skip
             */
            if (zoneElement == null) {
                continue;
            }

            Map<String, Object> beaconElement = findElementById(elements, beaconElementId);

            /*
             * beaconElementId도 예전 구조도 값이면 위치 element를 못 찾는다.
             * 이 경우 zoneElement 또는 beacon 테이블 좌표 fallback을 쓰도록 null 처리.
             */
            if (beaconElementId != null && beaconElement == null) {
                beaconElementId = null;
            }

            /*
             * 좌표는 beaconElementId가 있으면 비콘 마커 element 기준.
             * 없으면 zoneElement 기준.
             * 둘 다 없으면 beacon 테이블의 x/y fallback.
             */
            Map<String, Object> positionElement = beaconElement != null
                    ? beaconElement
                    : zoneElement;

            List<StudentV4> detectedStudents =
                    studentsByBeaconId.getOrDefault(beacon.getId(), List.of());

            List<MonitoringStudentResponse> studentResponses = detectedStudents.stream()
                    .map(this::toMonitoringStudent)
                    .toList();

            markers.add(
                    BeaconMarkerResponse.builder()
                            .beaconId(beacon.getId())
                            .beaconNo(beacon.getBeaconNo())

                            /*
                             * 기존 호환용 elementId는 zoneElementId로 내려준다.
                             */
                            .elementId(zoneElementId)
                            .beaconElementId(beaconElementId)
                            .zoneElementId(zoneElementId)

                            /*
                             * placementName / zoneType은 mapping snapshot 우선.
                             * 없으면 구조도 JSON의 zone element에서 fallback.
                             */
                            .placementName(resolvePlacementName(mapping, zoneElement, beacon))
                            .zoneType(resolveZoneType(mapping, zoneElement))
                            .thresholdRssi(mapping.getEffectiveThresholdRssi())
                            .isActive(mapping.isEffectivelyActive())

                            .x(resolveDouble(positionElement, "x", beacon.getX()))
                            .y(resolveDouble(positionElement, "y", beacon.getY()))
                            .width(resolveDouble(positionElement, "width", null))
                            .height(resolveDouble(positionElement, "height", null))

                            .studentCount(studentResponses.size())
                            .students(studentResponses)
                            .build()
            );
        }

        return markers;
    }

    private MonitoringStudentResponse toMonitoringStudent(StudentV4 student) {
        return MonitoringStudentResponse.builder()
                .studentId(student.getId())
                .studentName(student.getStudentName())
                .beaconState(
                        student.getBeaconState() != null
                                ? student.getBeaconState().name()
                                : null
                )
                .lastRssi(student.getLastBeaconRssi())
                .lastSeenAt(
                        student.getLastBeaconSeenAt() != null
                                ? student.getLastBeaconSeenAt().toString()
                                : null
                )
                .build();
    }

    private Map<String, Object> findElementById(
            List<Map<String, Object>> elements,
            String elementId
    ) {
        if (elementId == null) {
            return null;
        }

        for (Map<String, Object> element : elements) {
            String currentId = asString(firstNonNull(
                    element.get("id"),
                    element.get("elementId"),
                    element.get("element_id")
            ));

            if (elementId.equals(currentId)) {
                return element;
            }
        }

        return null;
    }

    private String resolvePlacementName(Map<String, Object> element, BeaconV4 beacon) {
        if (element != null) {
            String value = asString(firstNonNull(
                    element.get("placementName"),
                    element.get("name"),
                    element.get("label"),
                    element.get("elementName"),
                    element.get("element_name")
            ));

            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return beacon.getName();
    }

    private String resolvePlacementName(
            BeaconElementMapV4 mapping,
            Map<String, Object> zoneElement,
            BeaconV4 beacon
    ) {
        /*
         * 현재 활성 구조도 JSON 값을 우선한다.
         * DB snapshot은 과거 구조도 값일 수 있기 때문이다.
         */
        if (zoneElement != null) {
            String currentName = trimToNull(asString(firstNonNull(
                    zoneElement.get("placementName"),
                    zoneElement.get("name"),
                    zoneElement.get("label"),
                    zoneElement.get("elementName"),
                    zoneElement.get("element_name")
            )));

            if (currentName != null) {
                return currentName;
            }
        }

        String snapshotName = trimToNull(mapping.getPlacementName());
        if (snapshotName != null) {
            return snapshotName;
        }

        return beacon.getName();
    }

    private String resolveZoneType(
            BeaconElementMapV4 mapping,
            Map<String, Object> zoneElement
    ) {
        /*
         * 현재 활성 구조도 JSON 값을 우선한다.
         */
        String currentZoneType = trimToNull(resolveZoneType(zoneElement));

        if (currentZoneType != null) {
            return currentZoneType;
        }

        return trimToNull(mapping.getZoneType());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resolveZoneType(Map<String, Object> element) {
        if (element == null) {
            return null;
        }

        return asString(firstNonNull(
                element.get("zoneType"),
                element.get("elementType"),
                element.get("element_type"),
                element.get("type")
        ));
    }

    private Double resolveDouble(Map<String, Object> element, String key, Double fallback) {
        if (element == null) {
            return fallback;
        }

        Double value = asDouble(element.get(key));
        return value != null ? value : fallback;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Integer asInteger(Object value) {
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

    private Double asDouble(Object value) {
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
}