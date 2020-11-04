package cz.vlada.beer.fridge.listener

import cz.vlada.beer.fridge.LinearPrediction
import cz.vlada.beer.fridge.repo.LastValuesRepository
import cz.vlada.beer.fridge.repo.StoredValue
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class FridgeController() : MqttListener {
    private val setLowTemperatureTopic = "node/BeerFridge/thermometer/low/temperature/set"
    private val setHighTemperatureTopic = "node/BeerFridge/thermometer/high/temperature/set"
    private val probeTemperatureTopic = "node/BeerFridge/probe-thermometer/750301a2795d2028/temperature"
    private val externalTemperatureTopic = "node/BeerFridge/thermometer/0:1/temperature"
    private val powerSwitchTopic = "shellies/beer_fridge_shelly/relay/0/command"

    companion object {
        private val DEFAULT_PREDICTION_WINDOW = Duration.ofMinutes(15)
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
            probeTemperatureTopic -> controlTemperature(publish)
            externalTemperatureTopic -> controlTemperature(publish)
        }
    }

    private suspend fun controlTemperature(publish: suspend (String, String) -> Unit) {
        val (probe, external) = getActualTemperatures() ?: return
        val (before, now) = controlExternalTemperatureIfProbeWithinRange(probe, external)
        val predictedValue = LinearPrediction.getValueAtTime(
            before,
            now,
            Instant.now().plus(DEFAULT_PREDICTION_WINDOW)
        )
        log.debug(
            "BeerFridge  - temperature: ${now.value}, " +
                    "predicted = $predictedValue " +
                    "lowTemperature = $lowTemperature, " +
                    "highTemperature = $highTemperature"
        )
        if (predictedValue < lowTemperature) {
            log.info(
                "Turning BeerFridge off - temperature: ${now.value}, " +
                        "lowTemperature: $lowTemperature, " +
                        "predicted = $predictedValue"
            )
            publish(powerSwitchTopic, "off")
        }
        if (predictedValue > highTemperature) {
            log.info(
                "Turning BeerFridge on - temperature: ${now.value}, " +
                        "highTemperature: $highTemperature, " +
                        "predicted = $predictedValue"
            )
            publish(powerSwitchTopic, "on")
        }

    }

    private fun getActualTemperatures(): Pair<Float, Float>? {
        val probe = LastValuesRepository.get(probeTemperatureTopic)
        val external = LastValuesRepository.get(externalTemperatureTopic)
        if(probe == null || external == null) {
            return null
        }
        return Pair(probe.value.toFloat(), external.value.toFloat())
    }

    private fun controlExternalTemperatureIfProbeWithinRange(
        probe: Float,
        external: Float
    ): Pair<StoredValue, StoredValue> {
        return if (probe > lowTemperature && probe < highTemperature) {
            log.debug("BeerFridge controlled by external temperature, probe = $probe, external = $external")
            val externalSV = StoredValue(Instant.now(), external.toString())
            Pair(
                LastValuesRepository.getEarliest(externalTemperatureTopic) ?: externalSV,
                externalSV
            )
        } else {
            log.debug("BeerFridge controlled by probe temperature, probe = $probe, external = $external")
            val probeSV = StoredValue(Instant.now(), probe.toString())
            Pair(
                LastValuesRepository.getEarliest(probeTemperatureTopic) ?: probeSV,
                probeSV
            )
        }
    }

    override fun getTopicsToListenTo() =
        listOf(probeTemperatureTopic, externalTemperatureTopic, setLowTemperatureTopic, setHighTemperatureTopic)
}
