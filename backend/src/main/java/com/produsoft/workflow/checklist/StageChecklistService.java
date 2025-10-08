package com.produsoft.workflow.checklist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.produsoft.workflow.domain.StageType;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class StageChecklistService {

    private static final String CHECKLIST_RESOURCE = "checklists.yml";

    private final Map<StageType, List<ChecklistTaskDefinition>> definitions;

    public StageChecklistService() {
        this.definitions = loadDefinitions(new ClassPathResource(CHECKLIST_RESOURCE));
    }

    public List<ChecklistTaskDefinition> definitionsFor(StageType stage) {
        return definitions.getOrDefault(stage, List.of());
    }

    public Map<String, Boolean> initializeState(StageType stage) {
        List<ChecklistTaskDefinition> tasks = definitionsFor(stage);
        Map<String, Boolean> state = new LinkedHashMap<>();
        tasks.forEach(task -> state.put(task.id(), false));
        return state;
    }

    public Map<String, Boolean> updateTask(StageType stage, Map<String, Boolean> currentState, String taskId, boolean completed) {
        List<ChecklistTaskDefinition> tasks = definitionsFor(stage);
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("No checklist configured for stage %s".formatted(stage));
        }
        Map<String, Boolean> safeState = currentState == null ? Map.of() : currentState;
        Map<String, Boolean> nextState = new LinkedHashMap<>();
        boolean found = false;
        for (ChecklistTaskDefinition task : tasks) {
            boolean value = safeState.getOrDefault(task.id(), false);
            if (Objects.equals(task.id(), taskId)) {
                value = completed;
                found = true;
            }
            nextState.put(task.id(), value);
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown checklist task %s for stage %s".formatted(taskId, stage));
        }
        return nextState;
    }

    public List<ChecklistItem> buildChecklist(StageType stage, Map<String, Boolean> state) {
        Map<String, Boolean> safeState = state == null ? Map.of() : state;
        return definitionsFor(stage).stream()
            .map(task -> new ChecklistItem(task.id(), task.label(), task.required(), safeState.getOrDefault(task.id(), false)))
            .collect(Collectors.toList());
    }

    public boolean isChecklistComplete(StageType stage, Map<String, Boolean> state) {
        Map<String, Boolean> safeState = state == null ? Map.of() : state;
        return definitionsFor(stage).stream()
            .filter(ChecklistTaskDefinition::required)
            .allMatch(task -> Boolean.TRUE.equals(safeState.get(task.id())));
    }

    private Map<StageType, List<ChecklistTaskDefinition>> loadDefinitions(Resource resource) {
        if (!resource.exists()) {
            throw new IllegalStateException("Checklist configuration not found on classpath: " + CHECKLIST_RESOURCE);
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, List<ChecklistTaskDefinition>> raw = mapper.readValue(inputStream,
                new TypeReference<Map<String, List<ChecklistTaskDefinition>>>() {});
            Map<String, List<ChecklistTaskDefinition>> normalized = new LinkedHashMap<>();
            if (raw != null) {
                raw.forEach((key, value) -> normalized.put(key.toUpperCase(Locale.ROOT), value == null ? List.of() : List.copyOf(value)));
            }
            Map<StageType, List<ChecklistTaskDefinition>> mapped = new EnumMap<>(StageType.class);
            for (StageType stage : StageType.values()) {
                mapped.put(stage, normalized.getOrDefault(stage.name(), List.of()));
            }
            return mapped;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load checklist configuration", ex);
        }
    }
}
