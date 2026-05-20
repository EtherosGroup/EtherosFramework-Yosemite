package cn.skilfully.etherosframework.hook.core;

import cn.skilfully.etherosframework.hook.entity.Event;
import cn.skilfully.etherosframework.hook.entity.Priority;

import java.util.List;
import java.util.function.Consumer;

public interface HookManager {

    /**
     * 注册钩子
     * @param eventType 事件类型
     * @param hook 钩子实体
     * @return 钩子ID
     */
    String register(String eventType, Consumer<Event> hook, Priority priority);

    /**
     * 注册绑定多事件钩子
     * @param eventTypes 事件类型
     * @param hook 钩子实体
     * @return 钩子ID
     */
    String register(List<String> eventTypes, Consumer<Event> hook, Priority priority);

    /**
     * 调用所有事件钩子
     * @param eventType 事件类型
     * @param eventData 事件参数
     */
    void callEvent(String eventType, Event eventData);

    /**
     * 调用指定钩子
     * @param hookId 钩子ID
     * @param eventData 时间参数
     */
    void callHook(String hookId, Event eventData);

    /**
     * 移除钩子
     * @param eventType 事件类型
     * @param hook 钩子实体
     */
    void remove(String eventType, Consumer<Event> hook);

    /**
     * 移除钩子
     * @param hookId 钩子ID
     */
    void remove(String hookId);

    /**
     * 移除所有钩子
     * @param eventType 事件类型
     */
    void removeAll(String eventType);

}
