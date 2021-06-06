package cz.vlada.beer.fridge.listener

import cz.vlada.beer.fridge.listener.MessageExtentions.asString
import org.eclipse.paho.client.mqttv3.MqttMessage

object ISpindel: MqttListener {
    private val mappedTopics = listOf(
        TopicMapping(
            "ispindel/iSpindel/temperature",
            "node/FermentationVessel/thermometer/-/temperature"
        ),
        TopicMapping(
            "ispindel/iSpindel/tilt",
            "node/FermentationVessel/tilt/-/tilt"
        ),
        TopicMapping(
            "ispindel/iSpindel/battery",
            "node/FermentationVessel/battery/-/voltage"
        ),
        TopicMapping(
            "ispindel/iSpindel/gravity",
            "node/FermentationVessel/tilt/-/gravity"
        )
    )

    override suspend fun messageArrived(topic: String, message: MqttMessage, publish: MqttPublisher) {
        mappedTopics
            .filter { it.sourceTopic == topic }
            .forEach { publish(it.destinationTopic, message.asString()) }
    }

    override fun getTopicsToListenTo(): List<String> = mappedTopics.map { it.sourceTopic }

    private data class TopicMapping(
        val sourceTopic: String,
        val destinationTopic: String
    )
}
