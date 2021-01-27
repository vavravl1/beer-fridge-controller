package cz.vlada.beer.fridge.listener

import cz.vlada.beer.fridge.LinearPrediction
import cz.vlada.beer.fridge.repo.LastValuesRepository
import cz.vlada.beer.fridge.repo.StoredValue
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

object FridgeController : MqttListener {
    private const val setColdTemperatureTopic = "node/BeerFridge/thermometer/cold/temperature/set"
    private const val setLowTemperatureTopic = "node/BeerFridge/thermometer/low/temperature/set"
    private const val setHighTemperatureTopic = "node/BeerFridge/thermometer/high/temperature/set"
    private const val probeTemperatureTopic = "node/BeerFridge/probe-thermometer/750301a2795d2028/temperature"
    private const val externalTemperatureTopic = "node/BeerFridge/thermometer/0:0/temperature"
    private const val powerSwitchTopic = "shellies/beer_fridge_shelly/relay/0/command"
    private const val heatingPadTopic = "node/BeerFridge/relay/0:0/state/set"
    private val predictionWindow = Duration.ofMinutes(15)

    private val log = LoggerFactory.getLogger("cz.vlada.beer.fridge.listener.FridgeMqttListener")

    private var highTemperature: Float = (LastValuesRepository.get(setHighTemperatureTopic)?.value ?: "3F").toFloat()
    private var lowTemperature: Float = (LastValuesRepository.get(setLowTemperatureTopic)?.value ?: "2F").toFloat()
    private var coldTemperature: Float = (LastValuesRepository.get(setColdTemperatureTopic)?.value ?: "-5F").toFloat()
    private var controlledByProbe: Boolean = false

    override suspend fun messageArrived(
        topic: String,
        message: MqttMessage,
        publish: suspend (String, String) -> Unit
    ) {
        val msg = String(message.payload).toFloat()
        when (topic) {
            setColdTemperatureTopic -> {
                log.info("Setting BeerFridge coldTemperature to $msg")
                coldTemperature = msg
            }
            setLowTemperatureTopic -> {
                log.info("Setting BeerFridge lowTemperature to $msg")
                lowTemperature = msg
            }
            setHighTemperatureTopic -> {
                log.info("Setting BeerFridge highTemperature to $msg")
                highTemperature = msg
            }
            probeTemperatureTopic -> controlTemperature(publish)
            externalTemperatureTopic -> controlTemperature(publish)
        }
    }

    private suspend fun controlTemperature(publish: suspend (String, String) -> Unit) {
        val (probe, external) = getActualTemperatures() ?: return
        val (predictedProbe, predictedExternal) = predictTemperatures(probe, external)
        controllFridge(probe, external, predictedProbe, predictedExternal, publish)
        controllHeatingPad(probe, external, predictedProbe, predictedExternal, publish)
    }

    private suspend fun controllFridge(
        probe: Float,
        external: Float,
        predictedProbe: Float,
        predictedExternal: Float,
        publish: suspend (String, String) -> Unit
    ) {
        if (predictedProbe > highTemperature) {
            logStatus(probe, external, predictedProbe, predictedExternal, "probe", "fridge on")
            publish(powerSwitchTopic, "on")
            controlledByProbe = true
        } else if (predictedProbe < lowTemperature) {
            logStatus(probe, external, predictedProbe, predictedExternal, "probe", "fridge off")
            publish(powerSwitchTopic, "off")
            controlledByProbe = false
        } else if (predictedExternal > highTemperature && !controlledByProbe) {
            logStatus(probe, external, predictedProbe, predictedExternal, "external", "fridge on")
            publish(powerSwitchTopic, "on")
        } else if (predictedExternal < lowTemperature && !controlledByProbe) {
            logStatus(probe, external, predictedProbe, predictedExternal, "external", "fridge off")
            publish(powerSwitchTopic, "off")
        }
    }

    private suspend fun controllHeatingPad(
        probe: Float,
        external: Float,
        predictedProbe: Float,
        predictedExternal: Float,
        publish: suspend (String, String) -> Unit
    ) {
        if (predictedProbe < coldTemperature) {
            publish(heatingPadTopic, "true")
            logStatus(probe, external, predictedProbe, predictedExternal, "probe", "heating pad on")
        } else if (predictedProbe > lowTemperature) {
            publish(heatingPadTopic, "false")
            logStatus(probe, external, predictedProbe, predictedExternal, "probe", "heating pad off")
        }
    }

    private fun logStatus(
        probe: Float,
        external: Float,
        predictedProbe: Float,
        predictedExternal: Float,
        sensor: String,
        status: String
    ) {
        log.info(
            "Turning $status ($sensor) - " +
                    "probe: ${"%.2f".format(probe)}, " +
                    "external: ${"%.2f".format(external)}, " +
                    "predictedProbe = ${"%.2f".format(predictedProbe)}, " +
                    "predictedExternal = ${"%.2f".format(predictedExternal)}, " +
                    "highTemperature: ${"%.2f".format(highTemperature)} " +
                    "lowTemperature: ${"%.2f".format(lowTemperature)} " +
                    "coldTemperature: ${"%.2f".format(coldTemperature)}"
        )
    }

    private fun predictTemperatures(probe: Float, external: Float): Pair<Float, Float> {
        val probeNow = StoredValue.fromNow(probe)
        val externalNow = StoredValue.fromNow(external)
        val earliestProbe = LastValuesRepository.getEarliest(probeTemperatureTopic) ?: probeNow
        val earliestExternal = LastValuesRepository.getEarliest(externalTemperatureTopic) ?: externalNow
        val predictionTime = Instant.now().plus(predictionWindow)
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

    override fun getTopicsToListenTo() =
        listOf(
            probeTemperatureTopic,
            externalTemperatureTopic,
            setLowTemperatureTopic,
            setHighTemperatureTopic,
            setColdTemperatureTopic
        )
}
