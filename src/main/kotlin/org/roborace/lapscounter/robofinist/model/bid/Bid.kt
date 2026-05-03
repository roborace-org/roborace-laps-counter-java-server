package org.roborace.lapscounter.robofinist.model.bid

data class Bid(
    val id: Int,
    val name: String,
    val status: Int,
    val organizations: List<Organization>,
)

data class Organization(
    val id: Int,
    val name: String,
)
