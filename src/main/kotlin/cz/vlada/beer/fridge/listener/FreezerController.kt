package cz.vlada.beer.fridge.listener

import cz.vlada.beer.fridge.repo.LastValuesRepository
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.slf4j.LoggerFactory

object FreezerController : MqttListener {
    private const val setLowTemperatureTopic = "node/BeerFreezer/thermometer/low/temperature/set"
    private const val setHighTemperatureTopic = "node/BeerFreezer/thermometer/high/temperature/set"
    private const val currentTemperatureTopic = "node/BeerFreezer/thermometer/0:1/temperature"
    private const val powerSwitchTopic = "shellies/beer_fridge_shelly/relay/1/command"

    private val log = LoggerFactory.getLogger("cz.vlada.beer.fridge.listener.FridgeMqttListener")

    private var lowTemperature: Float = (LastValuesRepository.get(setLowTemperatureTopic)?.value ?: "2F").toFloat()
    private var highTemperature: Float = (LastValuesRepository.get(setHighTemperatureTopic)?.value ?: "3F").toFloat()

    override suspend fun messageArrived(
        topic: String,
        message: MqttMessage,
        publish: MqttPublisher
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
                log.debug(
                    "BeerFreezer - temperature: $msg, " +
                            "lowTemperature = $lowTemperature, " +
                            "highTemperature = $highTemperature"
                )
                if (msg < lowTemperature) {
                    log.info(
                        "Turning BeerFreezer off - temperature: $msg, " +
                                "lowTemperature: $lowTemperature "
                    )
                    publish(powerSwitchTopic, "off")
                }
                if (msg > highTemperature) {
                    log.info(
                        "Turning BeerFreezer on - temperature: $msg, " +
                                "highTemperature: $highTemperature, "
                    )
                    publish(powerSwitchTopic, "on")
                }
            }
        }
    }

    override fun getTopicsToListenTo() =
        listOf(currentTemperatureTopic, setLowTemperatureTopic, setHighTemperatureTopic)
}
