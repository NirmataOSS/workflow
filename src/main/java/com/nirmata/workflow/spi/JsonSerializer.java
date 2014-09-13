package com.nirmata.workflow.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nirmata.workflow.models.Id;
import com.nirmata.workflow.models.Repetition;
import com.nirmata.workflow.models.ScheduleId;
import com.nirmata.workflow.models.ScheduleModel;
import com.nirmata.workflow.models.TaskId;
import com.nirmata.workflow.models.TaskModel;
import com.nirmata.workflow.models.TaskSet;
import com.nirmata.workflow.models.WorkflowId;
import com.nirmata.workflow.models.WorkflowModel;
import io.airlift.units.Duration;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonSerializer
{
    private static final ObjectMapper mapper = new ObjectMapper();

    public static ObjectNode newNode()
    {
        return mapper.createObjectNode();
    }

    public static ArrayNode newArrayNode()
    {
        return mapper.createArrayNode();
    }

    public static String toString(ObjectNode node)
    {
        try
        {
            return mapper.writeValueAsString(node);
        }
        catch ( JsonProcessingException e )
        {
            // TODO log
            throw new RuntimeException(e);
        }
    }

    public static JsonNode fromString(String str)
    {
        try
        {
            return mapper.readTree(str);
        }
        catch ( IOException e )
        {
            // TODO log
            throw new RuntimeException(e);
        }
    }

    public static void addWorkflow(ObjectNode node, WorkflowModel workflow)
    {
        ObjectNode workflowNode = newNode();
        addId(workflowNode, workflow.getWorkflowId());
        workflowNode.put("name", workflow.getName());
        addTaskSet(workflowNode, workflow.getTasks());
        node.set("workflow", workflowNode);
    }

    public static WorkflowModel getWorkflow(JsonNode node)
    {
        JsonNode workflowNode = node.get("workflow");
        return new WorkflowModel
        (
            new WorkflowId(getId(workflowNode)),
            workflowNode.get("name").asText(),
            getTaskSet(workflowNode)
        );
    }

    public static void addSchedule(ObjectNode node, ScheduleModel schedule)
    {
        ObjectNode scheduleNode = newNode();
        addRepetition(scheduleNode, schedule.getRepetition());
        addId(scheduleNode, schedule.getScheduleId());
        scheduleNode.put("workflowid", schedule.getWorkflowId().getId());
        scheduleNode.put("lastdate", newIsoDateFormatter().format(schedule.getLastExecution()));
        node.set("schedule", scheduleNode);
    }

    public static ScheduleModel getSchedule(JsonNode node)
    {
        JsonNode scheduleNode = node.get("schedule");
        try
        {
            return new ScheduleModel
            (
                new ScheduleId(getId(scheduleNode)),
                new WorkflowId(scheduleNode.get("workflowid").asText()),
                newIsoDateFormatter().parse(scheduleNode.get("lastdate").asText()),
                getRepetition(scheduleNode)
            );
        }
        catch ( ParseException e )
        {
            // TODO - add logging
            throw new RuntimeException(e);
        }
    }

    public static void addTaskSet(ObjectNode node, TaskSet taskSet)
    {
        ArrayNode tab = newArrayNode();
        for ( TaskId id : taskSet )
        {
            ObjectNode idNode = newNode();
            addId(idNode, id);
            tab.add(idNode);
        }
        node.set("taskset", tab);
    }

    public static TaskSet getTaskSet(JsonNode node)
    {
        List<TaskId> tasks = Lists.newArrayList();
        JsonNode tab = node.get("taskset");
        Iterator<JsonNode> elements = tab.elements();
        while ( elements.hasNext() )
        {
            JsonNode next = elements.next();
            tasks.add(new TaskId(getId(next)));
        }
        return new TaskSet(tasks);
    }

    public static void addTasks(ObjectNode node, Collection<TaskModel> tasks)
    {
        ArrayNode tab = newArrayNode();
        for ( TaskModel task : tasks )
        {
            ObjectNode taskNode = newNode();
            addTask(taskNode, task);
            tab.add(taskNode);
        }
        node.set("tasks", tab);
    }

    public static List<TaskModel> getTasks(JsonNode node)
    {
        ImmutableList.Builder<TaskModel> builder = ImmutableList.builder();
        JsonNode tab = node.get("tasks");
        Iterator<JsonNode> elements = tab.elements();
        while ( elements.hasNext() )
        {
            JsonNode next = elements.next();
            builder.add(getTask(next));
        }
        return builder.build();
    }

    public static void addTask(ObjectNode node, TaskModel task)
    {
        ObjectNode taskNode = newNode();
        addId(taskNode, task.getTaskId());
        taskNode.put("name", task.getName());
        taskNode.put("code", task.getTaskExecutionCode());
        taskNode.putPOJO("meta", task.getMetaData());
        node.set("task", taskNode);
    }

    public static TaskModel getTask(JsonNode node)
    {
        JsonNode taskNode = node.get("task");
        return new TaskModel
        (
            new TaskId(getId(taskNode)),
            taskNode.get("name").asText(),
            taskNode.get("code").asText(),
            getMap(taskNode.get("meta"))
        );
    }

    public static void addRepetition(ObjectNode node, Repetition repetition)
    {
        ObjectNode repetitionNode = newNode();
        repetitionNode.put("duration", repetition.getDuration().toString());
        repetitionNode.put("type", repetition.getType().name());
        node.set("repetition", repetitionNode);
    }

    public static Repetition getRepetition(JsonNode node)
    {
        JsonNode repetitionNode = node.get("repetition");
        String durationStr = repetitionNode.get("duration").asText();
        String typeStr = repetitionNode.get("type").asText();
        return new Repetition(Duration.valueOf(durationStr), Repetition.Type.valueOf(typeStr));
    }

    public static void addId(ObjectNode node, Id id)
    {
        node.put("id", id.getId());
    }

    public static String getId(JsonNode node)
    {
        return node.get("id").asText();
    }

    public static DateFormat newIsoDateFormatter()
    {
        // per http://stackoverflow.com/questions/2201925/converting-iso-8601-compliant-string-to-java-util-date
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }

    private static Map<String, String> getMap(JsonNode node)
    {
        Map<String, String> map = Maps.newHashMap();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while ( fields.hasNext() )
        {
            Map.Entry<String, JsonNode> nodeEntry = fields.next();
            map.put(nodeEntry.getKey(), nodeEntry.getValue().asText());
        }
        return map;
    }

    private JsonSerializer()
    {
    }
}
