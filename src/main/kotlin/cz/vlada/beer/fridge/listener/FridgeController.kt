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
        val (predictedProbe, predictedExternal) = predictTemperatures(probe, external)
        val predictedValue = selectTemperatureToControll(predictedProbe, predictedExternal)
        log.debug(
            "BeerFridge - probe: $probe, external: $external, " +
                    "predicted = $predictedValue " +
                    "lowTemperature = $lowTemperature, " +
                    "highTemperature = $highTemperature"
        )
        if (predictedValue < lowTemperature) {
            log.info(
                "Turning BeerFridge off - probe: $probe, external: $external, " +
                        "lowTemperature: $lowTemperature, " +
                        "predicted = $predictedValue"
            )
            publish(powerSwitchTopic, "off")
        }
        if (predictedValue > highTemperature) {
            log.info(
                "Turning BeerFridge on - probe: $probe, external: $external, " +
                        "highTemperature: $highTemperature, " +
                        "predicted = $predictedValue"
            )
            publish(powerSwitchTopic, "on")
        }
    }

    private fun predictTemperatures(probe: Float, external: Float): Pair<Float, Float> {
        val probeNow = StoredValue.fromNow(probe)
        val externalNow = StoredValue.fromNow(external)
        val earliestProbe = LastValuesRepository.getEarliest(probeTemperatureTopic) ?: probeNow
        val earliestExternal = LastValuesRepository.getEarliest(externalTemperatureTopic) ?: externalNow
        val predictionTime = Instant.now().plus(DEFAULT_PREDICTION_WINDOW)
        return Pair(
            LinearPrediction.getValueAtTime(
                earliestProbe,
                probeNow,
                predictionTime
            ), LinearPrediction.getValueAtTime(
                earliestExternal,
                externalNow,
                predictionTime
            )
        )
    }

    private fun getActualTemperatures(): Pair<Float, Float>? {
        val probe = LastValuesRepository.get(probeTemperatureTopic)
        val external = LastValuesRepository.get(externalTemperatureTopic)
        if (probe == null || external == null) {
            return null
        }
        return Pair(probe.value.toFloat(), external.value.toFloat())
    }

    private fun selectTemperatureToControll(probe: Float, external: Float): Float {
        return if (probe > lowTemperature && probe < highTemperature) {
            external
        } else {
            probe
        }
    }

    override fun getTopicsToListenTo() =
        listOf(probeTemperatureTopic, externalTemperatureTopic, setLowTemperatureTopic, setHighTemperatureTopic)
}
