package cz.vlada.beer.fridge.listener

import org.eclipse.paho.client.mqttv3.MqttMessage

typealias MqttPublisher = suspend (topic: String, message: String) -> Unit

interface MqttListener {
    suspend fun messageArrived(topic: String, message: MqttMessage, publish: MqttPublisher)
    fun getTopicsToListenTo(): List<String>
}