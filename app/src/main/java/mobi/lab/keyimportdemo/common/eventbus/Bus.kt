package mobi.lab.keyimportdemo.common.eventbus

import mobi.lab.keyimportdemo.common.util.isDebugBuild
import org.greenrobot.eventbus.EventBus

object Bus {

    @Volatile
    private var instance: EventBus = createInstance()

    fun post(event: Any) {
        instance.post(event)
    }

    fun subscribe(subscriber: Any) {
        if (instance.isRegistered(subscriber)) {
            return
        }
        instance.register(subscriber)
    }

    fun unsubscribe(subscriber: Any) {
        instance.unregister(subscriber)
    }

    private fun createInstance(): EventBus {
        return EventBus.builder()
            .eventInheritance(false)
            .logNoSubscriberMessages(isDebugBuild())
            .logSubscriberExceptions(isDebugBuild())
            .build()
    }
}
