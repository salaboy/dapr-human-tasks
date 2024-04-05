package com.salaboy.echo.workflow;

import com.salaboy.echo.workflow.HumanTaskActivity.Task;

public class WorkflowPayload {
    private String workflowId;
	private String taskName;
	private String taskPayload;
	private Task task;	

	public WorkflowPayload(String workflowId) {
		this.workflowId = workflowId;

	}

	public WorkflowPayload(String workflowId, Task task) {
		this.workflowId = workflowId;
		this.task = task;

	}


	public WorkflowPayload(String workflowId, String taskName, String taskPayload) {
		this.workflowId = workflowId;
		this.taskName = taskName;
		this.taskPayload = taskPayload;

	}
	
	
	public WorkflowPayload() {
	}

	public void setWorkflowId(String workflowId) {
		this.workflowId = workflowId;
	}

    public String getWorkflowId(){
        return this.workflowId;
    }

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getTaskPayload() {
		return taskPayload;
	}

	public void setTaskPayload(String taskPayload) {
		this.taskPayload = taskPayload;
	}

	

}
