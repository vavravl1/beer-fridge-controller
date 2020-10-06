package cz.vlada.beer.fridge

import cz.vlada.beer.fridge.listener.DelegatingMqttListener
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MqttConnector(mqttBrokerUrl: String, username: String, password: String, listener: DelegatingMqttListener) {
    private val log = LoggerFactory.getLogger("cz.vlada.beer.fridge.listener.MqttListener")

    private val client: MqttAsyncClient = MqttAsyncClient(mqttBrokerUrl, "beer_fridge_controller", MemoryPersistence())

    init {
        val connOpts = MqttConnectOptions()
        connOpts.userName = username
        connOpts.password = password.toCharArray()
        connOpts.isAutomaticReconnect = true
        connOpts.isCleanSession = true
        client.connect(connOpts, null, object : IMqttActionListener {
            val topics = listener.getTopicsToListenTo()
            val mqttListener = listener.createListener(::publish)
            override fun onSuccess(asyncActionToken: IMqttToken) {
                client.subscribe(
                    topics.toTypedArray(),
                    topics.map { 0 }.toIntArray(),
                    topics.map { mqttListener }.toTypedArray()
                )
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                log.error("Unable to connect to mqtt broker, ", exception)
            }
        })
    }

    private suspend fun publish(topic: String, msg: String) {
        return suspendCoroutine { continuation ->
            client.publish(topic, MqttMessage(msg.toByteArray()), null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
//                    log.debug("Message sent successfully")
                    continuation.resume(Unit)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable) {
                    log.error("Message sent failed")
                    continuation.resumeWithException(exception)
                }
            })
        }
    }
}
