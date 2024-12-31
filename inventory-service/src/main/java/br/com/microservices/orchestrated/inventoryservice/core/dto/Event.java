package br.com.microservices.orchestrated.inventoryservice.core.dto;



import br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private String id;
    private String transactionId;
    private String orderId;
    private Order payload;
    private String source;
    private ESagaStatus status;
    private List<History> EventHistory;
    private LocalDateTime createTime;

    public void addToHistory(History history) {
        if(isEmpty(this.EventHistory)) {
            this.EventHistory = new ArrayList<>();
        }
        this.EventHistory.add(history);
    }

}
