package cz.vlada.beer.fridge.listener

import cz.vlada.beer.fridge.LinearPrediction
import cz.vlada.beer.fridge.repo.LastValuesRepository
import cz.vlada.beer.fridge.repo.StoredValue
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class FridgeMqttListener(
    temperatureNodeName: String,
    thermometerName: String,
    thermometerAddress: String,
    shellyRelayIndex: Int
) : MqttListener {
    private val setLowTemperatureTopic = "node/$temperatureNodeName/thermometer/low/temperature/set"
    private val setHighTemperatureTopic = "node/$temperatureNodeName/thermometer/high/temperature/set"
    private val currentTemperatureTopic = "node/$temperatureNodeName/$thermometerName/$thermometerAddress/temperature"
    private val powerSwitchTopic = "shellies/beer_fridge_shelly/relay/$shellyRelayIndex/command"

    companion object {
        private val PREDITION_WINDOW = Duration.ofMinutes(10)
    }

    private val log = LoggerFactory.getLogger("cz.vlada.beer.fridge.listener.FridgeMqttListener")

    private var lowTemperature: Float = (LastValuesRepository.get(setLowTemperatureTopic)?.value ?: "2F").toFloat()
    private var highTemperature: Float = (LastValuesRepository.get(setHighTemperatureTopic)?.value ?: "3F").toFloat()

    override suspend fun messageArrived(
        topic: String,
        message: MqttMessage,
        publish: suspend (String, String) -> Unit
    ) {
        val msg = String(message.payload).toFloat()
        when (topic) {
            setLowTemperatureTopic -> {
                log.info("Setting lowTemperature to $msg")
                lowTemperature = msg
            }
            setHighTemperatureTopic -> {
                log.info("Setting highTemperature to $msg")
                highTemperature = msg
            }
            currentTemperatureTopic -> {
                if (msg > highTemperature) {
                    log.info("Turning fridge on - temperature: $msg, highTemperature: $highTemperature")
                    publish(powerSwitchTopic, "on")
                } else if (msg < lowTemperature) {
                    log.info("Turning fridge off - temperature: $msg, lowTemperature: $lowTemperature")
                    publish(powerSwitchTopic, "off")
                } else if (LastValuesRepository.getLast(topic) != null) {
                    val last = LastValuesRepository.getLast(topic)!!
                    val predictedValue = LinearPrediction.getValueAtTime(
                        last,
                        StoredValue(Instant.now(), String(message.payload)),
                        Instant.now().plus(PREDITION_WINDOW)
                    )
                    if (predictedValue < lowTemperature) {
                        log.info(
                            "Turning fridge off - temperature: $msg, " +
                                    "lowTemperature: $lowTemperature, " +
                                    "predicted = $predictedValue")
                        publish(powerSwitchTopic, "off")
                    }
                    if (predictedValue > highTemperature) {
                        log.info(
                            "Turning fridge on - temperature: $msg, " +
                                    "highTemperature: $highTemperature, " +
                                    "predicted = $predictedValue")
                        publish(powerSwitchTopic, "on")
                    }

                }
            }
        }
        LastValuesRepository.add(topic, String(message.payload))
    }

    override fun getTopicsToListenTo() =
        listOf(currentTemperatureTopic, setLowTemperatureTopic, setHighTemperatureTopic)
}
