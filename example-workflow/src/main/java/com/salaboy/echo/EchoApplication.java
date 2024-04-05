package com.salaboy.echo;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.salaboy.echo.workflow.HumanTaskActivity;
import com.salaboy.echo.workflow.SimpleWorkflow;
import com.salaboy.echo.workflow.WorkflowPayload;

import io.cloudevents.CloudEvent;
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EchoApplication {


	public static Map<String, WorkflowPayload> instancePayloads = new ConcurrentHashMap<>();

	public static void main(String[] args) {
		SpringApplication.run(EchoApplication.class, args);
	}

}

@RestController
@RequestMapping("/")
class SimpleWorkflowController {

	private static final Logger logger = LoggerFactory.getLogger(SimpleWorkflowController.class);

	@Value("${PUBLIC_IP:localhost}")
	private String publicIp;

	private final SimpMessagingTemplate simpMessagingTemplate;

	DaprWorkflowClient workflowClient = new DaprWorkflowClient();

	@PostMapping("events")
	CloudEvent echo(@RequestBody CloudEvent cloudEvent) {

		logger.info("Echo CloudEvent: " + cloudEvent.toString());
		emitWSEvent(cloudEvent);

		return cloudEvent;
	}

	public SimpleWorkflowController(SimpMessagingTemplate simpMessagingTemplate) {
		this.simpMessagingTemplate = simpMessagingTemplate;
		createWorkflowDefinition();
	}

	@Configuration
	class CloudEventHandlerConfiguration implements CodecCustomizer {

		@Override
		public void customize(CodecConfigurer configurer) {
			configurer.customCodecs().register(new CloudEventHttpMessageReader());
			configurer.customCodecs().register(new CloudEventHttpMessageWriter());
		}
	}

	@GetMapping("/server-info")
	public Info getInfo() {
		return new Info(publicIp);
	}

	@PostMapping("/start-workflow")
	public String startWorkflow() {
		System.out.println(" -> Starting a workflow instance");
		return startSimpleWorkflow();
	}

	@PostMapping("/complete-task")
	public String completeTask(@RequestParam("workflowId") String workflowId) {
		System.out.println(" -> Task Completed for workflow id: " + workflowId);
		workflowClient.raiseEvent(workflowId, "HumanTaskCompleted", true);
		return "OK";
	}

	@GetMapping("/status")
	public WorkflowPayload getWorkflowStatus(@RequestParam("workflowId") String workflowId){
		return EchoApplication.instancePayloads.get(workflowId);
	}

	public record Info(String publicIp) {
	}

	private void emitWSEvent(CloudEvent event) {
		System.out.println("Emitting Event via WS: " + event.toString());
		simpMessagingTemplate.convertAndSend("/topic/events",
				event);
	}

	private void createWorkflowDefinition(){
		WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder().registerWorkflow(SimpleWorkflow.class);
		builder.registerActivity(HumanTaskActivity.class);
		try (WorkflowRuntime runtime = builder.build()) {
		  System.out.println("Start workflow runtime");
		  runtime.start(false);
		}
	}

	private String startSimpleWorkflow(){
		WorkflowPayload payload = new WorkflowPayload();
	
		String instanceId = workflowClient.scheduleNewWorkflow(SimpleWorkflow.class, payload);
		System.out.printf("scheduled new workflow instance of PrizeWorkflow with instance ID: %s%n",
			instanceId);
	
		try {
		  workflowClient.waitForInstanceStart(instanceId, Duration.ofSeconds(10), false);
		  System.out.printf("workflow instance %s started%n", instanceId);
		} catch (TimeoutException e) {
		  System.out.printf("workflow instance %s did not start within 10 seconds%n", instanceId);
		}
		return instanceId;
	}

}
