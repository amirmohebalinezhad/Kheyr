package com.kheyr.sms.privacy

enum class CloudExportStatus { Pending, Running, Completed, Failed }

data class CloudDataExportRequest(val requestedAtMillis: Long, val status: CloudExportStatus = CloudExportStatus.Pending)
