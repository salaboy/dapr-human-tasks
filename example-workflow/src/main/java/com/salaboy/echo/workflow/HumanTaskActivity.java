package com.salaboy.echo.workflow;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;

public class HumanTaskActivity implements WorkflowActivity {

    private static RestTemplate restTemplate;
    
    @Override
    public Object run(WorkflowActivityContext ctx) {
        WorkflowPayload workflowPayload = ctx.getInput(WorkflowPayload.class);

        String humanTaskService = "http://localhost:8081";

        restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();


        Task task = new Task("", workflowPayload.getTaskName(), "", workflowPayload.getTaskPayload(), "");
        HttpEntity<Task> request = new HttpEntity<Task>(
            task, 
            headers);

        Task response = restTemplate.postForObject(
            humanTaskService + "/api/tasks/", 
            request, Task.class);

        System.out.println("Task stored: " + response);

        workflowPayload.setTask(response);

       
        
        return workflowPayload;
    } 

    public record Task(@JsonProperty("id") String id, 
                        @JsonProperty("name") String name, 
                        @JsonProperty("assignee") String assignee, 
                        @JsonProperty("payload") String payload, 
                        @JsonProperty("status") String status ){}



}
