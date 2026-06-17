using System.Text.Json;
using Kheyr.Domain;
using Kheyr.Domain.Entities;
using Kheyr.Infrastructure.Data;
using Kheyr.Infrastructure.Services;
using Microsoft.EntityFrameworkCore;

using ThreadEntity = Kheyr.Domain.Entities.Thread;

namespace Kheyr.Infrastructure.Services;

public class DeviceService(KheyrDbContext db)
{
    public async Task<Device> RegisterAsync(Guid userId, string deviceName, string deviceType, string platform, string? pushToken, string? publicKey, CancellationToken ct)
    {
        if (!Enum.TryParse<DeviceType>(deviceType, ignoreCase: true, out var parsedType))
            parsedType = DeviceType.Android;

        var device = new Device
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            DeviceName = deviceName,
            DeviceType = parsedType,
            Platform = platform,
            PushToken = pushToken,
            PublicKey = publicKey,
            CreatedAt = DateTimeOffset.UtcNow,
            LastActiveAt = DateTimeOffset.UtcNow,
        };
        db.Devices.Add(device);
        await db.SaveChangesAsync(ct);
        return device;
    }

    public async Task<bool> RevokeAsync(Guid userId, Guid deviceId, CancellationToken ct)
    {
        var device = await db.Devices.FirstOrDefaultAsync(x => x.Id == deviceId && x.UserId == userId, ct);
        if (device is null) return false;
        device.RevokedAt = DateTimeOffset.UtcNow;
        await db.RefreshTokens.Where(x => x.DeviceId == deviceId && x.RevokedAt == null)
            .ExecuteUpdateAsync(s => s.SetProperty(x => x.RevokedAt, DateTimeOffset.UtcNow), ct);
        await db.SaveChangesAsync(ct);
        return true;
    }

    public async Task<List<Device>> ListActiveAsync(Guid userId, CancellationToken ct)
        => await db.Devices.Where(x => x.UserId == userId && x.RevokedAt == null)
            .OrderByDescending(x => x.LastActiveAt).ToListAsync(ct);
}

public class SyncUploadService(KheyrDbContext db, SyncService syncService)
{
    public async Task ProcessInitialSyncAsync(Guid userId, Guid deviceId, JsonElement threads, JsonElement messages, CancellationToken ct)
    {
        await UpsertThreadsFromJson(userId, threads, ct);
        await UpsertMessagesFromJson(userId, messages, ct);

        var user = await db.Users.FirstAsync(x => x.Id == userId, ct);
        user.SyncEnabled = true;
        user.LastActiveAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(ct);

        await syncService.AppendChangeAsync(userId, SyncChangeType.MessageCreated, new { initial = true, device_id = deviceId }, ct);
    }

    public async Task ProcessUploadAsync(Guid userId, Guid? deviceId, JsonElement changes, CancellationToken ct)
    {
        if (changes.ValueKind != JsonValueKind.Array) return;

        foreach (var item in changes.EnumerateArray())
        {
            var eventNode = item.TryGetProperty("event", out var ev) ? ev : item;
            var type = eventNode.TryGetProperty("type", out var typeProp) ? typeProp.GetString() : null;
            if (type is null) continue;

            switch (type)
            {
                case "message_created":
                    await HandleMessageCreated(userId, eventNode, ct);
                    await syncService.AppendChangeAsync(userId, SyncChangeType.MessageCreated, JsonSerializer.Deserialize<object>(eventNode.GetRawText())!, ct);
                    break;
                case "message_deleted":
                    await HandleMessageDeleted(userId, eventNode, ct);
                    await syncService.AppendChangeAsync(userId, SyncChangeType.MessageDeleted, JsonSerializer.Deserialize<object>(eventNode.GetRawText())!, ct);
                    break;
                case "spam_status_changed":
                    await HandleSpamStatus(userId, eventNode, ct);
                    await syncService.AppendChangeAsync(userId, SyncChangeType.SpamStatusChanged, JsonSerializer.Deserialize<object>(eventNode.GetRawText())!, ct);
                    break;
                case "pin_changed":
                    await HandlePinChanged(userId, eventNode, ct);
                    await syncService.AppendChangeAsync(userId, SyncChangeType.PinChanged, JsonSerializer.Deserialize<object>(eventNode.GetRawText())!, ct);
                    break;
                case "archive_changed":
                    await HandleArchiveChanged(userId, eventNode, ct);
                    await syncService.AppendChangeAsync(userId, SyncChangeType.ArchiveChanged, JsonSerializer.Deserialize<object>(eventNode.GetRawText())!, ct);
                    break;
                case "notification_setting_changed":
                    await HandleNotificationSetting(userId, eventNode, ct);
                    await syncService.AppendChangeAsync(userId, SyncChangeType.NotificationSettingChanged, JsonSerializer.Deserialize<object>(eventNode.GetRawText())!, ct);
                    break;
            }
        }

        if (deviceId.HasValue)
        {
            var maxSeq = await db.SyncChanges.Where(x => x.UserId == userId).MaxAsync(x => (long?)x.Sequence, ct) ?? 0;
            await syncService.UpsertCursorAsync(userId, deviceId.Value, maxSeq, ct);
        }
    }

    private async Task UpsertThreadsFromJson(Guid userId, JsonElement threads, CancellationToken ct)
    {
        if (threads.ValueKind != JsonValueKind.Array) return;
        foreach (var t in threads.EnumerateArray())
        {
            var clientId = t.TryGetProperty("thread_id", out var tid) ? tid.GetInt64() : t.GetProperty("id").GetInt64();
            var thread = await db.Threads.FirstOrDefaultAsync(x => x.UserId == userId && x.ClientThreadId == clientId, ct);
            if (thread is null)
            {
                thread = new ThreadEntity
                {
                    Id = Guid.NewGuid(),
                    UserId = userId,
                    ClientThreadId = clientId,
                    CreatedAt = DateTimeOffset.UtcNow,
                    UpdatedAt = DateTimeOffset.UtcNow,
                };
                db.Threads.Add(thread);
            }

            ApplyThreadFields(thread, t);
            thread.UpdatedAt = DateTimeOffset.UtcNow;
        }
        await db.SaveChangesAsync(ct);
    }

    private async Task UpsertMessagesFromJson(Guid userId, JsonElement messages, CancellationToken ct)
    {
        if (messages.ValueKind != JsonValueKind.Array) return;
        foreach (var m in messages.EnumerateArray())
        {
            var clientMessageId = m.GetProperty("message_id").GetInt64();
            var clientThreadId = m.GetProperty("thread_id").GetInt64();
            var thread = await db.Threads.FirstOrDefaultAsync(x => x.UserId == userId && x.ClientThreadId == clientThreadId, ct);
            if (thread is null)
            {
                thread = new ThreadEntity
                {
                    Id = Guid.NewGuid(),
                    UserId = userId,
                    ClientThreadId = clientThreadId,
                    CreatedAt = DateTimeOffset.UtcNow,
                    UpdatedAt = DateTimeOffset.UtcNow,
                };
                db.Threads.Add(thread);
                await db.SaveChangesAsync(ct);
            }

            var message = await db.Messages.FirstOrDefaultAsync(x => x.UserId == userId && x.ClientMessageId == clientMessageId, ct);
            if (message is null)
            {
                message = new Message
                {
                    Id = Guid.NewGuid(),
                    UserId = userId,
                    ThreadId = thread.Id,
                    ClientMessageId = clientMessageId,
                    CreatedAt = DateTimeOffset.UtcNow,
                    UpdatedAt = DateTimeOffset.UtcNow,
                };
                db.Messages.Add(message);
            }

            message.EncryptedBody = m.TryGetProperty("encrypted_body", out var body) ? body.GetString() ?? "" : message.EncryptedBody;
            message.Timestamp = m.TryGetProperty("timestamp", out var ts)
                ? DateTimeOffset.FromUnixTimeMilliseconds(ts.GetInt64())
                : message.Timestamp;
            message.Direction = m.TryGetProperty("direction", out var dir) && Enum.TryParse<MessageDirection>(dir.GetString(), true, out var parsedDir)
                ? parsedDir : message.Direction;
            message.Status = m.TryGetProperty("status", out var st) && Enum.TryParse<MessageStatus>(st.GetString(), true, out var parsedSt)
                ? parsedSt : MessageStatus.Received;
            message.IsSpam = m.TryGetProperty("is_spam", out var spam) && spam.GetBoolean();
            message.UpdatedAt = DateTimeOffset.UtcNow;
            thread.LastMessageAt = message.Timestamp;
        }
        await db.SaveChangesAsync(ct);
    }

    private async Task HandleMessageCreated(Guid userId, JsonElement ev, CancellationToken ct)
    {
        var clientMessageId = ev.GetProperty("message_id").GetInt64();
        var clientThreadId = ev.GetProperty("thread_id").GetInt64();
        var thread = await EnsureThread(userId, clientThreadId, ct);
        var message = await db.Messages.FirstOrDefaultAsync(x => x.UserId == userId && x.ClientMessageId == clientMessageId, ct);
        if (message is null)
        {
            message = new Message
            {
                Id = Guid.NewGuid(),
                UserId = userId,
                ThreadId = thread.Id,
                ClientMessageId = clientMessageId,
                EncryptedBody = ev.GetProperty("encrypted_body").GetString() ?? "",
                Direction = MessageDirection.Incoming,
                Status = MessageStatus.Received,
                Timestamp = DateTimeOffset.UtcNow,
                CreatedAt = DateTimeOffset.UtcNow,
                UpdatedAt = DateTimeOffset.UtcNow,
            };
            db.Messages.Add(message);
        }
        else
        {
            message.EncryptedBody = ev.GetProperty("encrypted_body").GetString() ?? message.EncryptedBody;
            message.UpdatedAt = DateTimeOffset.UtcNow;
        }

        thread.LastMessageAt = message.Timestamp;
        await db.SaveChangesAsync(ct);
    }

    private async Task HandleMessageDeleted(Guid userId, JsonElement ev, CancellationToken ct)
    {
        var clientMessageId = ev.GetProperty("message_id").GetInt64();
        var message = await db.Messages.FirstOrDefaultAsync(x => x.UserId == userId && x.ClientMessageId == clientMessageId, ct);
        if (message is null) return;
        message.IsDeleted = true;
        message.DeletedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(ct);
    }

    private async Task HandleSpamStatus(Guid userId, JsonElement ev, CancellationToken ct)
    {
        var clientThreadId = ev.GetProperty("thread_id").GetInt64();
        var isSpam = ev.GetProperty("is_spam").GetBoolean();
        var thread = await db.Threads.FirstOrDefaultAsync(x => x.UserId == userId && x.ClientThreadId == clientThreadId, ct);
        if (thread is null) return;
        thread.IsSpam = isSpam;
        thread.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(ct);
    }

    private async Task HandlePinChanged(Guid userId, JsonElement ev, CancellationToken ct)
    {
        var clientThreadId = ev.GetProperty("thread_id").GetInt64();
        var isPinned = ev.GetProperty("is_pinned").GetBoolean();
        var thread = await db.Threads.FirstOrDefaultAsync(x => x.UserId == userId && x.ClientThreadId == clientThreadId, ct);
        if (thread is null) return;
        thread.IsPinned = isPinned;
        thread.PinnedAt = isPinned ? DateTimeOffset.UtcNow : null;
        thread.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(ct);
    }

    private async Task HandleArchiveChanged(Guid userId, JsonElement ev, CancellationToken ct)
    {
        var clientThreadId = ev.GetProperty("thread_id").GetInt64();
        var isArchived = ev.GetProperty("is_archived").GetBoolean();
        var thread = await db.Threads.FirstOrDefaultAsync(x => x.UserId == userId && x.ClientThreadId == clientThreadId, ct);
        if (thread is null) return;
        thread.IsArchived = isArchived;
        thread.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(ct);
    }

    private async Task HandleNotificationSetting(Guid userId, JsonElement ev, CancellationToken ct)
    {
        var clientThreadId = ev.GetProperty("thread_id").GetInt64();
        var muted = ev.TryGetProperty("muted", out var m) && m.GetBoolean();
        var thread = await db.Threads.FirstOrDefaultAsync(x => x.UserId == userId && x.ClientThreadId == clientThreadId, ct);
        if (thread is null) return;
        thread.IsMuted = muted;
        if (ev.TryGetProperty("custom_ringtone", out var ring)) thread.CustomRingtone = ring.GetString();
        thread.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(ct);
    }

    private async Task<ThreadEntity> EnsureThread(Guid userId, long clientThreadId, CancellationToken ct)
    {
        var thread = await db.Threads.FirstOrDefaultAsync(x => x.UserId == userId && x.ClientThreadId == clientThreadId, ct);
        if (thread is not null) return thread;
        thread = new ThreadEntity
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            ClientThreadId = clientThreadId,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow,
        };
        db.Threads.Add(thread);
        await db.SaveChangesAsync(ct);
        return thread;
    }

    private static void ApplyThreadFields(ThreadEntity thread, JsonElement t)
    {
        if (t.TryGetProperty("encrypted_phone_number", out var phone)) thread.EncryptedPhoneNumber = phone.GetString();
        if (t.TryGetProperty("phone_number_hash", out var hash)) thread.PhoneNumberHash = hash.GetString();
        if (t.TryGetProperty("is_spam", out var spam)) thread.IsSpam = spam.GetBoolean();
        if (t.TryGetProperty("is_archived", out var archived)) thread.IsArchived = archived.GetBoolean();
        if (t.TryGetProperty("is_pinned", out var pinned)) thread.IsPinned = pinned.GetBoolean();
        if (t.TryGetProperty("unread_count", out var unread)) thread.UnreadCount = unread.GetInt32();
    }
}

public class DirectMessageService(KheyrDbContext db)
{
    public async Task<DirectMessage> SendAsync(
        Guid senderUserId, string recipientPhoneHash, string messageType, string body,
        string? attachmentId, string clientMessageId, CancellationToken ct)
    {
        var recipient = await db.Users.FirstOrDefaultAsync(x => x.PhoneNumberHash == recipientPhoneHash && x.DeletedAt == null, ct);
        var type = messageType.Equals("direct_picture", StringComparison.OrdinalIgnoreCase)
            ? MessageType.DirectPicture
            : MessageType.DirectText;

        var message = new DirectMessage
        {
            Id = Guid.NewGuid(),
            SenderUserId = senderUserId,
            RecipientPhoneHash = recipientPhoneHash,
            RecipientUserId = recipient?.Id,
            MessageType = type,
            Body = body,
            AttachmentId = attachmentId,
            ClientMessageId = clientMessageId,
            Status = DirectMessageStatus.Sent,
            CreatedAt = DateTimeOffset.UtcNow,
        };
        db.DirectMessages.Add(message);
        await db.SaveChangesAsync(ct);
        return message;
    }
}
