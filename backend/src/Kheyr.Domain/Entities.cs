namespace Kheyr.Domain.Entities;

public class User
{
    public Guid Id { get; set; }
    public string PhoneNumberHash { get; set; } = string.Empty;
    public string? EncryptedPhoneNumber { get; set; }
    public bool SyncEnabled { get; set; }
    public bool DirectMessageEnabled { get; set; } = true;
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset LastActiveAt { get; set; }
    public DateTimeOffset? DeletedAt { get; set; }

    public ICollection<Device> Devices { get; set; } = [];
    public ICollection<Thread> Threads { get; set; } = [];
    public ICollection<Message> Messages { get; set; } = [];
}

public class Device
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public string DeviceName { get; set; } = string.Empty;
    public DeviceType DeviceType { get; set; }
    public string Platform { get; set; } = string.Empty;
    public string? PublicKey { get; set; }
    public string? PushToken { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset LastActiveAt { get; set; }
    public DateTimeOffset? RevokedAt { get; set; }

    public User User { get; set; } = null!;
}

public class Thread
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public long ClientThreadId { get; set; }
    public string? EncryptedPhoneNumber { get; set; }
    public string? PhoneNumberHash { get; set; }
    public string? EncryptedContactName { get; set; }
    public DateTimeOffset? LastMessageAt { get; set; }
    public int UnreadCount { get; set; }
    public bool IsSpam { get; set; }
    public bool IsArchived { get; set; }
    public bool IsPinned { get; set; }
    public DateTimeOffset? PinnedAt { get; set; }
    public bool IsMuted { get; set; }
    public string? NotificationMode { get; set; }
    public string? CustomRingtone { get; set; }
    public string? DefaultSimId { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
    public DateTimeOffset? DeletedAt { get; set; }

    public User User { get; set; } = null!;
    public ICollection<Message> Messages { get; set; } = [];
}

public class Message
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public Guid ThreadId { get; set; }
    public long ClientMessageId { get; set; }
    public string EncryptedBody { get; set; } = string.Empty;
    public string? EncryptedSender { get; set; }
    public string? EncryptedReceiver { get; set; }
    public MessageDirection Direction { get; set; }
    public MessageType MessageType { get; set; } = MessageType.Sms;
    public MessageStatus Status { get; set; }
    public DateTimeOffset Timestamp { get; set; }
    public string? SimId { get; set; }
    public int? SpamScore { get; set; }
    public string? SpamReasonsJson { get; set; }
    public bool IsSpam { get; set; }
    public bool IsDeleted { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
    public DateTimeOffset? DeletedAt { get; set; }

    public User User { get; set; } = null!;
    public Thread Thread { get; set; } = null!;
}

public class SpamRuleVersion
{
    public Guid Id { get; set; }
    public int Version { get; set; }
    public int Threshold { get; set; } = 70;
    public string RulesJson { get; set; } = "[]";
    public bool IsPublished { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public string? CreatedBy { get; set; }
    public string? Notes { get; set; }
}

public class SpamFeedback
{
    public Guid Id { get; set; }
    public Guid? UserId { get; set; }
    public string SenderHash { get; set; } = string.Empty;
    public string MessageHash { get; set; } = string.Empty;
    public int RuleVersion { get; set; }
    public int SpamScore { get; set; }
    public SpamFeedbackAction UserAction { get; set; }
    public string TriggeredRuleIdsJson { get; set; } = "[]";
    public DateTimeOffset CreatedAt { get; set; }
}

public class SyncCursor
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public Guid DeviceId { get; set; }
    public long Cursor { get; set; }
    public DateTimeOffset LastSyncedAt { get; set; }

    public User User { get; set; } = null!;
    public Device Device { get; set; } = null!;
}

public class SyncChange
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public long Sequence { get; set; }
    public SyncChangeType ChangeType { get; set; }
    public string PayloadJson { get; set; } = "{}";
    public DateTimeOffset CreatedAt { get; set; }

    public User User { get; set; } = null!;
}

public class PairingSession
{
    public Guid Id { get; set; }
    public Guid? UserId { get; set; }
    public Guid? DesktopDeviceId { get; set; }
    public string QrPayload { get; set; } = string.Empty;
    public PairingSessionStatus Status { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset ExpiresAt { get; set; }
    public DateTimeOffset? ApprovedAt { get; set; }
    public string? DesktopDeviceName { get; set; }
    public string? DesktopPlatform { get; set; }
    public string? DesktopPublicKey { get; set; }
}

public class DesktopSmsRequest
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public Guid DesktopDeviceId { get; set; }
    public Guid? AndroidDeviceId { get; set; }
    public string EncryptedMessageBody { get; set; } = string.Empty;
    public string EncryptedTargetNumber { get; set; } = string.Empty;
    public string? SimId { get; set; }
    public long? ThreadClientId { get; set; }
    public string? ClientMessageId { get; set; }
    public DesktopSmsRequestStatus Status { get; set; }
    public string? FailureReason { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset ExpiresAt { get; set; }
    public DateTimeOffset? CompletedAt { get; set; }
}

public class DirectMessage
{
    public Guid Id { get; set; }
    public Guid SenderUserId { get; set; }
    public string RecipientPhoneHash { get; set; } = string.Empty;
    public Guid? RecipientUserId { get; set; }
    public MessageType MessageType { get; set; }
    public string Body { get; set; } = string.Empty;
    public string? AttachmentId { get; set; }
    public string ClientMessageId { get; set; } = string.Empty;
    public DirectMessageStatus Status { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? DeliveredAt { get; set; }
    public DateTimeOffset? ReadAt { get; set; }
}

public class RefreshToken
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public Guid DeviceId { get; set; }
    public string TokenHash { get; set; } = string.Empty;
    public DateTimeOffset ExpiresAt { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? RevokedAt { get; set; }

    public User User { get; set; } = null!;
    public Device Device { get; set; } = null!;
}

public class OtpChallenge
{
    public Guid Id { get; set; }
    public string PhoneE164 { get; set; } = string.Empty;
    public string CodeHash { get; set; } = string.Empty;
    public DateTimeOffset ExpiresAt { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public int AttemptCount { get; set; }
    public bool Consumed { get; set; }
}

public class AdminUser
{
    public Guid Id { get; set; }
    public string Username { get; set; } = string.Empty;
    public string PasswordHash { get; set; } = string.Empty;
    public DateTimeOffset CreatedAt { get; set; }
}

public class SystemMetricSnapshot
{
    public Guid Id { get; set; }
    public string MetricName { get; set; } = string.Empty;
    public double Value { get; set; }
    public DateTimeOffset RecordedAt { get; set; }
}
