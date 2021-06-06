package cz.vlada.beer.fridge.listener

import org.eclipse.paho.client.mqttv3.MqttMessage

object MessageExtentions {
    fun MqttMessage.asString() = String(this.payload)
    fun MqttMessage.isOn() = this.asString() == "on"
}