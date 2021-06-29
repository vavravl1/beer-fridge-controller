package cz.vlada.beer.fridge

import cz.vlada.beer.fridge.listener.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main(args: Array<String>) {

    val mqttListener = DelegatingMqttListener(listOf(
        FridgeController,
        FreezerController,
        StatsPublishingListener(),
        ISpindel,
    ))

    MqttConnector(
        System.getenv("MQTT_BROKER_URL"),
        System.getenv("MQTT_BROKER_USERNAME"),
        System.getenv("MQTT_BROKER_PASSWORD"),
        mqttListener
    )
}