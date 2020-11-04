package cz.vlada.beer.fridge

import cz.vlada.beer.fridge.listener.DelegatingMqttListener
import cz.vlada.beer.fridge.listener.FridgeController
import cz.vlada.beer.fridge.listener.FridgeMqttListener
import cz.vlada.beer.fridge.listener.StatsPublishingListener
import java.time.Duration

fun main(args: Array<String>) {

    val mqttListener = DelegatingMqttListener(listOf(
        FridgeController(),
        FridgeMqttListener("BeerFreezer", "thermometer", "0:1", 1, Duration.ofMinutes(30)),
        StatsPublishingListener()
    ))

    MqttConnector(
        System.getenv("MQTT_BROKER_URL"),
        System.getenv("MQTT_BROKER_USERNAME"),
        System.getenv("MQTT_BROKER_PASSWORD"),
        mqttListener
    )
}