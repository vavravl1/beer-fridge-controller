package cz.vlada.beer.fridge

import cz.vlada.beer.fridge.listener.DelegatingMqttListener
import cz.vlada.beer.fridge.listener.FridgeController
import cz.vlada.beer.fridge.listener.FreezerController
import cz.vlada.beer.fridge.listener.StatsPublishingListener

fun main(args: Array<String>) {

    val mqttListener = DelegatingMqttListener(listOf(
        FridgeController,
        FreezerController,
        StatsPublishingListener()
    ))

    MqttConnector(
        System.getenv("MQTT_BROKER_URL"),
        System.getenv("MQTT_BROKER_USERNAME"),
        System.getenv("MQTT_BROKER_PASSWORD"),
        mqttListener
    )
}