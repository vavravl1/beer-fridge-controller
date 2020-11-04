package cz.vlada.beer.fridge.listener

import cz.vlada.beer.fridge.repo.LastValuesRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttMessageListener

class DelegatingMqttListener(
    private val listeners: List<MqttListener>
) {
    fun getTopicsToListenTo(): List<String> = listeners.flatMap(MqttListener::getTopicsToListenTo)

    fun createListener(publisher: suspend (String, String) -> Unit): IMqttMessageListener =
        IMqttMessageListener { topic, message ->
            LastValuesRepository.add(topic, String(message.payload))
            listeners.forEach {
                if(it.getTopicsToListenTo().contains(topic)) {
                    GlobalScope.launch { it.messageArrived(topic, message, publisher) }
                }
            }
        }
}