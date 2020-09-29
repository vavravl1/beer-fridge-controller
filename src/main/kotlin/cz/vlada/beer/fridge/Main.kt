package cz.vlada.beer.fridge

fun main(args: Array<String>) {
    MqttConnector(
        System.getenv("MQTT_BROKER_URL"),
        System.getenv("MQTT_BROKER_USERNAME"),
        System.getenv("MQTT_BROKER_PASSWORD")
    )
}