package cz.vlada.beer.fridge

import cz.vlada.beer.fridge.listener.DelegatingMqttListener
import cz.vlada.beer.fridge.listener.FridgeMqttListener
import cz.vlada.beer.fridge.listener.StatsPublishingListener

fun main(args: Array<String>) {

    val mqttListener = DelegatingMqttListener(listOf(
        FridgeMqttListener("BeerFridge", "probe-thermometer", "750301a2795d2028", 0),
        FridgeMqttListener("BeerFreezer", "thermometer", "0:1", 1),
        StatsPublishingListener()
    ))

    MqttConnector(
        System.getenv("MQTT_BROKER_URL"),
        System.getenv("MQTT_BROKER_USERNAME"),
        System.getenv("MQTT_BROKER_PASSWORD"),
        mqttListener
    )
}