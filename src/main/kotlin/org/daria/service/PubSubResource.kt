package org.daria.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.Subscriber
import com.google.common.util.concurrent.MoreExecutors
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import io.quarkiverse.googlecloudservices.pubsub.QuarkusPubSub
import jakarta.annotation.PostConstruct
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import org.daria.persistence.CurrencyPairEntity
import org.daria.persistence.CurrencyRepository
import org.eclipse.microprofile.config.inject.ConfigProperty


@Path("/pubsub")
class PubSubResource(val pubSub: QuarkusPubSub, val repository: CurrencyRepository, val objectMapper: ObjectMapper) {

    @field:ConfigProperty(name = "demo.topic.name")
    lateinit var topicName: String

    @field:ConfigProperty(name = "demo.topic.subscription")
    lateinit var subscriptionName: String

    private val logger = KotlinLogging.logger {}

    private lateinit var subscriber: Subscriber


    @PostConstruct
    fun init() {
        logger.info { "PubSubResource init" }
        pubSub.createSubscription(topicName, subscriptionName)

        val receiver = MessageReceiver { message: PubsubMessage, consumer: AckReplyConsumer ->
            logger.info { "Got message ${message.data.toStringUtf8()}" }
            val currencyEntity = objectMapper.readValue(message.data.toStringUtf8(), CurrencyPairEntity::class.java)
            repository.save(currencyEntity)
            consumer.ack()
        }
        subscriber = pubSub.subscriber(subscriptionName, receiver)
        subscriber.startAsync().awaitRunning()
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun pubsub(currency: CurrencyPairEntity) {
        val publisher: Publisher = pubSub.publisher(topicName)

        try {
            val data = objectMapper.writeValueAsString(currency).toByteStringUtf8()
            val pubsubMessage: PubsubMessage = PubsubMessage.newBuilder().setData(data).build()
            val messageIdFuture: ApiFuture<String> = publisher.publish(pubsubMessage) // Publish the message
            ApiFutures.addCallback(messageIdFuture, object : ApiFutureCallback<String> {
                override fun onSuccess(messageId: String) {
                    logger.info { "published with message id $messageId" }
                }

                override fun onFailure(t: Throwable) {
                    logger.warn { "failed to publish: $t" }
                }
            }, MoreExecutors.directExecutor())
        } finally {
            publisher.shutdown()
            publisher.awaitTermination(1, TimeUnit.MINUTES)
        }
    }

    @GET
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    fun getAllCurrencies(): MutableList<CurrencyPairEntity> {
        return repository.findAll()
    }

    private fun String.toByteStringUtf8(): ByteString = ByteString.copyFromUtf8(this)
}