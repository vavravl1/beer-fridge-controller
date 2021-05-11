package cz.vlada.beer.fridge.listener

import cz.vlada.beer.fridge.LinearPrediction
import cz.vlada.beer.fridge.repo.LastValuesRepository
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

object FreezerController : MqttListener {
    private const val setLowTemperatureTopic = "node/BeerFreezer/thermometer/low/temperature/set"
    private const val setHighTemperatureTopic = "node/BeerFreezer/thermometer/high/temperature/set"
    private const val currentTemperatureTopic = "node/BeerFreezer/thermometer/0:1/temperature"
    private const val powerSwitchTopic = "shellies/beer_fridge_shelly/relay/1/command"

    private val predictionWindow = Duration.ofMinutes(10)

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
                val temperatureHistory = LastValuesRepository.getAll(topic)
                if(temperatureHistory.isNotEmpty()) {
                    val predictedValue = LinearPrediction.getValueAtTime(
                        temperatureHistory,
                        Instant.now().plus(predictionWindow)
                    )
                    log.debug(
                        "BeerFreezer - temperature: $msg, " +
                                "predicted = $predictedValue " +
                                "lowTemperature = $lowTemperature, " +
                                "highTemperature = $highTemperature"
                    )
                    if (predictedValue < lowTemperature) {
                        log.info(
                            "Turning BeerFreezer off - temperature: $msg, " +
                                    "lowTemperature: $lowTemperature, " +
                                    "predicted = $predictedValue")
                        publish(powerSwitchTopic, "off")
                    }
                    if (predictedValue > highTemperature) {
                        log.info(
                            "Turning BeerFreezer on - temperature: $msg, " +
                                    "highTemperature: $highTemperature, " +
                                    "predicted = $predictedValue")
                        publish(powerSwitchTopic, "on")
                    }
                }
            }
        }
    }

    override fun getTopicsToListenTo() =
        listOf(currentTemperatureTopic, setLowTemperatureTopic, setHighTemperatureTopic)
}
