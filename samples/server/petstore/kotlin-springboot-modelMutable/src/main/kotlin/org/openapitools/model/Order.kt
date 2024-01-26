package org.openapitools.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import javax.validation.constraints.*
import javax.validation.Valid
import io.swagger.v3.oas.annotations.media.Schema

/**
 * An order for a pets from the pet store
 * @param id 
 * @param petId 
 * @param quantity 
 * @param shipDate 
 * @param status Order Status
 * @param complete 
 */
data class Order(

    @Schema(example = "null", description = "")
    @get:JsonProperty("id") var id: kotlin.Long? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("petId") var petId: kotlin.Long? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("quantity") var quantity: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("shipDate") var shipDate: java.time.OffsetDateTime? = null,

    @Schema(example = "null", description = "Order Status")
    @get:JsonProperty("status") var status: Order.Status? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("complete") var complete: kotlin.Boolean? = false
) {

    /**
    * Order Status
    * Values: placed,approved,delivered
    */
    enum class Status(val value: kotlin.String) {

        @JsonProperty("placed") placed("placed"),
        @JsonProperty("approved") approved("approved"),
        @JsonProperty("delivered") delivered("delivered")
    }

}

