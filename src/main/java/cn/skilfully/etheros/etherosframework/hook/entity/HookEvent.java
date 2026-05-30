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

    private Map<Object, Object> data;
    private boolean cancelled;
    private String cancelReason;

}
