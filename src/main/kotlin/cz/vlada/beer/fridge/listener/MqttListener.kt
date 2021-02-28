package cz.vlada.beer.fridge.listener

import org.eclipse.paho.client.mqttv3.MqttMessage

interface MqttListener {
    suspend fun messageArrived(topic: String, message: MqttMessage, publish: suspend (String, String) -> Unit)
    fun getTopicsToListenTo(): List<String>
}