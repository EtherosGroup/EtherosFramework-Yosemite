package cn.skilfully.etheros.etherosframework.hook.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class HookEvent {

    private String eventType;
    private Map<Object, Object> data;
    private boolean cancelled;
    private String cancelReason;

    public HookEvent(String eventType, Map<Object, Object> data) {
        this.eventType = eventType;
        this.data = data;
        this.cancelled = false;
        this.cancelReason = null;
    }
}
