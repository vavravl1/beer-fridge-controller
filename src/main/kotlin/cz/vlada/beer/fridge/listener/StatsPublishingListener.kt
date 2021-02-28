package cz.vlada.beer.fridge.listener

import org.eclipse.paho.client.mqttv3.MqttMessage

class StatsPublishingListener : MqttListener {
    companion object {
        const val RELAY_BEER_FRIDGE_TOPIC_0 = "shellies/beer_fridge_shelly/relay/0"
        const val RELAY_BEER_FRIDGE_TOPIC_1 = "shellies/beer_fridge_shelly/relay/1"
        const val POWER_BEER_FRIDGE_TOPIC_0 = "shellies/beer_fridge_shelly/relay/0/power"
        const val POWER_BEER_FRIDGE_TOPIC_1 = "shellies/beer_fridge_shelly/relay/1/power"
        const val RELAY_TEMPERATURE_BEER_FRIDGE_TOPIC = "shellies/beer_fridge_shelly/temperature"
    }

    override suspend fun messageArrived(
        topic: String,
        message: MqttMessage,
        publish: suspend (String, String) -> Unit
    ) = when (topic) {
        RELAY_BEER_FRIDGE_TOPIC_0 -> {
            publish(
                "node/BeerFridge/relay/0/state",
                "${message.isOn()}"
            )
        }
        RELAY_BEER_FRIDGE_TOPIC_1 -> {
            publish(
                "node/BeerFreezer/relay/1/state",
                "${message.isOn()}"
            )
        }
        POWER_BEER_FRIDGE_TOPIC_0 -> publish(
            "node/BeerFridge/powermeter/0/power",
            message.asString()
        )
        POWER_BEER_FRIDGE_TOPIC_1 -> publish(
            "node/BeerFreezer/powermeter/1/power",
            message.asString()
        )
        RELAY_TEMPERATURE_BEER_FRIDGE_TOPIC -> {
            publish(
                "node/BeerFridge/powermeter/0/temperature",
                message.asString()
            )
            publish(
                "node/BeerFreezer/powermeter/1/temperature",
                message.asString()
            )
        }
        else -> Unit
    }

    override fun getTopicsToListenTo(): List<String> = listOf(
        RELAY_BEER_FRIDGE_TOPIC_0,
        RELAY_BEER_FRIDGE_TOPIC_1,
        POWER_BEER_FRIDGE_TOPIC_0,
        POWER_BEER_FRIDGE_TOPIC_1,
        RELAY_TEMPERATURE_BEER_FRIDGE_TOPIC
    )

    private fun MqttMessage.asString() = String(this.payload)
    private fun MqttMessage.isOn() = this.asString() == "on"
}
