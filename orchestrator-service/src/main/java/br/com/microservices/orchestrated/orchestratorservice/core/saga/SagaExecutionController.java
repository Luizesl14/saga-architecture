package br.com.microservices.orchestrated.orchestratorservice.core.saga;

import br.com.microservices.orchestrated.orchestratorservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orchestratorservice.core.dto.Event;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaHandler.*;
import static java.lang.String.format;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Component
@AllArgsConstructor
public class SagaExecutionController {

    private static final String SAGA_LOGGER = "ORDER ID: %s | TRANSACTION ID: %s | EVENT ID: %s";
    private static final String SUCCESS_LOGGER = "### CURRENT SAGA: {} | SUCCESS | NEXT TOPIC {} | {}";
    private static final String ROLLBACK_LOGGER = "### CURRENT SAGA: {} | SENDING TO ROLLBACK CURRENT SERVICE | NEXT TOPIC {} | {}";
    private static final String FAIL_LOGGER = "### CURRENT SAGA: {} | SENDING TO FAIL ROLLBACK PREVIOUS SERVICE | NEXT TOPIC {} | {}";

    public ETopics getNextTopic(Event event) {
        if(isEmpty(event.getSource()) || isEmpty(event.getStatus())){
           throw new ValidationException("Source and Status must be informed.");
        }
        var topic = findTopicsBySourceAndStatus(event);
        return topic;
    }

    private ETopics findTopicsBySourceAndStatus(Event event) {
        return (ETopics) (Arrays.stream(SAGA_HANDLERS)
                .filter(row-> isEventSourceAndStatusValid(event, row))
                .map(i-> i[TOPIC_INDEX])
                .findFirst()
                .orElseThrow(()-> new ValidationException("Topic not found!")));
    }

    public boolean isEventSourceAndStatusValid(Event event, Object[] row) {
        var source = row[EVENT_SOURCE_INDEX];
        var status = row[SAGA_STATUS_INDEX];
        return  event.getSource().equals(source) && event.getStatus().equals(status);
    }

    private void logCurrentSaga(Event event, ETopics topics) {
        var sagaId = createSagaId(event);
        var source = event.getSource();

        switch (event.getStatus()) {
            case SUCCESS -> log.info(SUCCESS_LOGGER, source, topics, sagaId);
            case ROLLBACK_PENDING -> log.info(ROLLBACK_LOGGER, source, topics, sagaId);
            case FAIL ->  log.info(FAIL_LOGGER, source, topics, sagaId);
        }

    }

    private String createSagaId(Event event) {
        return format(SAGA_LOGGER, event.getPayload().getId(),
                event.getTransactionId(), event.getId());
    }
}
