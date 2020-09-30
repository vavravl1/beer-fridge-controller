package cz.vlada.beer.fridge

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.slf4j.LoggerFactory

class FridgeMqttListener {
    companion object {
        const val SET_LOW_TOPIC = "node/Beer/thermometer/low/temperature/set"
        const val SET_HIGH_TOPIC = "node/Beer/thermometer/high/temperature/set"
        const val TEMPERATURE_TOPIC = "node/Beer/thermometer/0:1/temperature"
        const val POWER_SWITCH_TOPIC = "node/PowerSwitch/power-switch/-/state/set"
    }

    private val log = LoggerFactory.getLogger("cz.vlada.beer.fridge.FridgeMqttListener")

    private var lowTemperature: Float = (LastValuesRepository.get(SET_LOW_TOPIC)?.value ?: "2F").toFloat()
    private var highTemperature: Float = (LastValuesRepository.get(SET_HIGH_TOPIC)?.value ?: "3F").toFloat()

    private fun messageArrived(topic: String, message: MqttMessage, publish: suspend (String, String) -> Unit) =
        GlobalScope.launch {
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
                        publish(POWER_SWITCH_TOPIC, """"on"""")
                    } else if (msg < lowTemperature) {
                        log.info("Turning fridge off - temperature: $msg, lowTemperature: $lowTemperature")
                        publish(POWER_SWITCH_TOPIC, """"off"""")
                    }
                }
            }
            LastValuesRepository.add(topic, String(message.payload))
        }

    fun getTopicsToListenTo() = listOf(TEMPERATURE_TOPIC, SET_LOW_TOPIC, SET_HIGH_TOPIC)

    fun createListener(publisher: suspend (String, String) -> Unit): IMqttMessageListener =
        IMqttMessageListener { topic, message ->
            messageArrived(topic, message, publisher)
        }
}
