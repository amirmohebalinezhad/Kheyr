package com.kheyr.sms.direct

enum class MessageTransportMode { Sms, Direct }
data class RecipientCapability(val registeredForDirect: Boolean)
object DirectMessageModeResolver {
    fun resolve(preferDirect: Boolean, capability: RecipientCapability): MessageTransportMode = if (preferDirect && capability.registeredForDirect) MessageTransportMode.Direct else MessageTransportMode.Sms
}
