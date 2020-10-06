package cz.vlada.beer.fridge.listener

import org.eclipse.paho.client.mqttv3.MqttMessage

class StatsPublishingListener : MqttListener {
    companion object {
        const val RELAY_BEER_FRIDGE_TOPIC = "shellies/beer_fridge_shelly/relay/0"
        const val POWER_BEER_FRIDGE_TOPIC = "shellies/beer_fridge_shelly/relay/0/power"
        const val RELAY_TEMPERATURE_BEER_FRIDGE_TOPIC = "shellies/beer_fridge_shelly/temperature"
    }

    override suspend fun messageArrived(
        topic: String,
        message: MqttMessage,
        publish: suspend (String, String) -> Unit
    ) = when (topic) {
            RELAY_BEER_FRIDGE_TOPIC -> publish(
                "node/BeerFridge/relay/0/status",
                """"${String(message.payload)}""""
            )
            POWER_BEER_FRIDGE_TOPIC -> publish(
                "node/BeerFridge/relay/0/power",
                String(message.payload)
            )
            RELAY_TEMPERATURE_BEER_FRIDGE_TOPIC -> publish(
                "node/BeerFridge/relay/0/temperature",
                String(message.payload)
            )
            else -> Unit
        }

    override fun getTopicsToListenTo(): List<String> = listOf(
        RELAY_BEER_FRIDGE_TOPIC,
        POWER_BEER_FRIDGE_TOPIC,
        RELAY_TEMPERATURE_BEER_FRIDGE_TOPIC
    )

}