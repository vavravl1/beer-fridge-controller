package cz.vlada.beer.fridge.listener

import cz.vlada.beer.fridge.LastValuesRepository
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.slf4j.LoggerFactory

class FridgeMqttListener : MqttListener {
    companion object {
        const val SET_LOW_TOPIC = "node/BeerFridge/thermometer/low/temperature/set"
        const val SET_HIGH_TOPIC = "node/BeerFridge/thermometer/high/temperature/set"
        const val TEMPERATURE_TOPIC = "node/BeerFridge/thermometer/0:1/temperature"
        const val POWER_SWITCH_TOPIC = "shellies/beer_fridge_shelly/relay/0/command"
    }

    private val log = LoggerFactory.getLogger("cz.vlada.beer.fridge.listener.FridgeMqttListener")

    private var lowTemperature: Float = (LastValuesRepository.get(SET_LOW_TOPIC)?.value ?: "2F").toFloat()
    private var highTemperature: Float = (LastValuesRepository.get(SET_HIGH_TOPIC)?.value ?: "3F").toFloat()

    override suspend fun messageArrived(
        topic: String,
        message: MqttMessage,
        publish: suspend (String, String) -> Unit
    ) {
        val msg = String(message.payload).toFloat()
        when (topic) {
            SET_LOW_TOPIC -> {
                log.info("Setting lowTemperature to $msg")
                lowTemperature = msg
            }
            SET_HIGH_TOPIC -> {
                log.info("Setting highTemperature to $msg")
                highTemperature = msg
            }
            TEMPERATURE_TOPIC -> {
                if (msg > highTemperature) {
                    log.info("Turning fridge on - temperature: $msg, highTemperature: $highTemperature")
                    publish(POWER_SWITCH_TOPIC, "on")
                } else if (msg < lowTemperature) {
                    log.info("Turning fridge off - temperature: $msg, lowTemperature: $lowTemperature")
                    publish(POWER_SWITCH_TOPIC, "off")
                }
            }
        }
        LastValuesRepository.add(topic, String(message.payload))
    }

    override fun getTopicsToListenTo() = listOf(TEMPERATURE_TOPIC, SET_LOW_TOPIC, SET_HIGH_TOPIC)
}
