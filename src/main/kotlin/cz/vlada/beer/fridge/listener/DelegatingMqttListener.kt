package cz.vlada.beer.fridge.listener

import cz.vlada.beer.fridge.repo.LastValuesRepository
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.MqttAsyncClient

class DelegatingMqttListener(
    private val listeners: List<MqttListener>
) {
    fun getTopicsToListenTo(): List<String> = listeners.flatMap(MqttListener::getTopicsToListenTo)

    fun createListener(client: MqttAsyncClient, publisher: suspend (String, String) -> Unit): IMqttMessageListener =
        IMqttMessageListener { topic, message ->
            GlobalScope.launch {
                LastValuesRepository.add(topic, String(message.payload))
                listeners
                    .filter { it.getTopicsToListenTo().contains(topic) }
                    .map { launch { it.messageArrived(topic, message, publisher) } }
                    .joinAll()
                client.messageArrivedComplete(message.id, message.qos)
            }
        }
}