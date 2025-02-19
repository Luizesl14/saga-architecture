package br.com.microservices.orchestrated.orchestratorservice.core.service;

import br.com.microservices.orchestrated.orchestratorservice.core.dto.Event;
import br.com.microservices.orchestrated.orchestratorservice.core.dto.History;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import br.com.microservices.orchestrated.orchestratorservice.core.producer.SagaOrchestratorProducer;
import br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaExecutionController;
import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSource.ORCHESTRATOR;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus.FAIL;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus.SUCCESS;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics.NOTIFY_ENDING;

@Slf4j
@Service
@AllArgsConstructor
public class OrchestratorService {

    private final JsonUtil jsonUtil;
    private final SagaOrchestratorProducer producer;
    private final SagaExecutionController controller;

    public void startSaga(Event event) {
        event.setSource(ORCHESTRATOR);
        event.setStatus(SUCCESS);
        log.info("SAGA STARTED...");
        var topic = getTopic(event);
        this.addHistory(event, "Starting Saga");
        producer.sendEvent(jsonUtil.toJson(event), topic.getTopic());


    }

    public void finishSagaSuccess(Event event){
        event.setSource(ORCHESTRATOR);
        event.setStatus(SUCCESS);
        log.info("SAGA FINISHED SUCCESSFULLY :D:D FOR EVENT: {}", event.getId());
        this.addHistory(event, "Saga finished successfully");
        this.notifyFinishSaga(event);

    }

    public void finishSagaFail(Event event){
        event.setSource(ORCHESTRATOR);
        event.setStatus(FAIL);
        log.info("SAGA FINISHED WITH ERRORS :(:( FOR EVENT: {}", event.getId());
        this.addHistory(event, "Saga finished with errors");
        this.notifyFinishSaga(event);
    }

    public void continueSaga(Event event){
        var topic = getTopic(event);
        log.info("SAGA CONTINUE SAGA STARTED  FOR EVENT: {}", event.getId());
        producer.sendEvent(jsonUtil.toJson(event), topic.getTopic());
    }

    private ETopics getTopic(Event event) {
        return this.controller.getNextTopic(event);
    }

    public void addHistory(Event event, String message) {
        var history = History.builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createTime(event.getCreateTime())
                .build();
        event.addToHistory(history);
    }

    private void notifyFinishSaga(Event event) {
        producer.sendEvent(jsonUtil.toJson(event), NOTIFY_ENDING.getTopic());
    }
}
