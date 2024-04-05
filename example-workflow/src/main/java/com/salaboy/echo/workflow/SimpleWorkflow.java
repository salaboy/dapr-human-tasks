package com.salaboy.echo.workflow;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowContext;
import io.dapr.workflows.WorkflowStub;

import java.time.Duration;

import org.slf4j.Logger;

import com.salaboy.echo.workflow.HumanTaskActivity.Task;

public class SimpleWorkflow extends Workflow {
  
  @Override
  public WorkflowStub create() {
    return ctx -> {
      Logger logger = ctx.getLogger();    
      String instanceId = ctx.getInstanceId();
      logger.info("Starting Workflow: " + ctx.getName());
      logger.info("Instance ID: " + instanceId);
      logger.info("Current Orchestration Time: " + ctx.getCurrentInstant());

      WorkflowPayload workflowPayload = ctx.getInput(WorkflowPayload.class);
      workflowPayload.setWorkflowId(instanceId);
      
      Task task = createHumanTask("Task for salaboy", "loads of data from customer", ctx);


      ctx.complete(workflowPayload);

    };
  }


  public Task createHumanTask(String taskName, String taskPayload, WorkflowContext ctx){
     WorkflowPayload workflowPayload = ctx.getInput(WorkflowPayload.class);
     Logger logger = ctx.getLogger();
      workflowPayload.setTaskName(taskName);
      workflowPayload.setTaskPayload(taskPayload);

      workflowPayload = ctx.callActivity(HumanTaskActivity.class.getName(), workflowPayload, WorkflowPayload.class).await();
    
      if(workflowPayload.getTask() != null){ // A task was created, we need to wait for completion
        logger.info("A Task was created, wait for completion");
        ctx.waitForExternalEvent("HumanTaskCompleted", Duration.ofMinutes(5), boolean.class).await();
      } else{
        logger.info("No Task created");
      }
      return workflowPayload.getTask();
  }
    
}
