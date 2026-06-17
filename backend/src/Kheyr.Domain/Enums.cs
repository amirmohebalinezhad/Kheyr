namespace Kheyr.Domain;

public enum DeviceType
{
    Android,
    Desktop,
}

public enum MessageDirection
{
    Incoming,
    Outgoing,
}

public enum MessageType
{
    Sms,
    DirectText,
    DirectPicture,
}

public enum MessageStatus
{
    Received,
    Sending,
    Sent,
    Delivered,
    Failed,
    Read,
}

public enum DesktopSmsRequestStatus
{
    Queued,
    WaitingForPhone,
    Sending,
    Sent,
    Failed,
    Expired,
}

public enum PairingSessionStatus
{
    Pending,
    Approved,
    Expired,
    Revoked,
}

public enum SyncChangeType
{
    MessageCreated,
    MessageUpdated,
    MessageDeleted,
    ThreadUpdated,
    ThreadDeleted,
    SpamStatusChanged,
    PinChanged,
    ArchiveChanged,
    ReadStateChanged,
    NotificationSettingChanged,
}

public enum SpamFeedbackAction
{
    Spam,
    NotSpam,
}

public enum DirectMessageStatus
{
    Sent,
    Delivered,
    Read,
    Failed,
}
