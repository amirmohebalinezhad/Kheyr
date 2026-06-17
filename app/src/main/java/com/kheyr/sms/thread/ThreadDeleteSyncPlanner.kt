package com.kheyr.sms.thread

data class ThreadDeletePlan(val localThreadIds: Set<Long>, val syncDeleteThreadIds: Set<Long>)
class ThreadDeleteSyncPlanner {
    fun plan(threadIds: Set<Long>, syncEnabled: Boolean): ThreadDeletePlan = ThreadDeletePlan(threadIds, if (syncEnabled) threadIds else emptySet())
}
