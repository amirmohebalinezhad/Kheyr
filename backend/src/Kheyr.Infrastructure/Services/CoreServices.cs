using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Kheyr.Domain;
using Kheyr.Domain.Entities;
using Kheyr.Domain.Options;
using Kheyr.Infrastructure.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace Kheyr.Infrastructure.Services;

public static class Hashing
{
    public static string Sha256(string value)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(value));
        return Convert.ToHexString(bytes).ToLowerInvariant();
    }

    public static string PhoneHash(string phoneE164, string salt)
        => Sha256($"{salt}:{phoneE164}");
}

public class TokenService(IOptions<JwtOptions> jwtOptions)
{
    private readonly JwtOptions _options = jwtOptions.Value;

    public (string AccessToken, DateTimeOffset ExpiresAt) CreateAccessToken(Guid userId, Guid deviceId, DeviceType deviceType)
    {
        var expires = DateTimeOffset.UtcNow.AddMinutes(_options.AccessTokenMinutes);
        var claims = new[]
        {
            new System.Security.Claims.Claim(System.Security.Claims.ClaimTypes.NameIdentifier, userId.ToString()),
            new System.Security.Claims.Claim("device_id", deviceId.ToString()),
            new System.Security.Claims.Claim("device_type", deviceType.ToString()),
        };

        var key = new Microsoft.IdentityModel.Tokens.SymmetricSecurityKey(Encoding.UTF8.GetBytes(_options.SigningKey));
        var creds = new Microsoft.IdentityModel.Tokens.SigningCredentials(key, Microsoft.IdentityModel.Tokens.SecurityAlgorithms.HmacSha256);
        var token = new System.IdentityModel.Tokens.Jwt.JwtSecurityToken(
            issuer: _options.Issuer,
            audience: _options.Audience,
            claims: claims,
            expires: expires.UtcDateTime,
            signingCredentials: creds);

        var handler = new System.IdentityModel.Tokens.Jwt.JwtSecurityTokenHandler();
        return (handler.WriteToken(token), expires);
    }

    public string CreateRefreshTokenValue() => Convert.ToBase64String(RandomNumberGenerator.GetBytes(48));

    public string HashRefreshToken(string token) => Hashing.Sha256(token);
}

public class OtpService(
    KheyrDbContext db,
    IOptions<OtpOptions> otpOptions,
    ILogger<OtpService> logger)
{
    private readonly OtpOptions _options = otpOptions.Value;
    private const string PhoneSalt = "kheyr-phone-v1";

    public async Task<(bool Success, string? DevCode)> RequestOtpAsync(string phoneE164, CancellationToken ct)
    {
        var normalized = NormalizePhone(phoneE164);
        if (normalized is null) return (false, null);

        var code = _options.FixedCode ?? GenerateCode();
        var challenge = new OtpChallenge
        {
            Id = Guid.NewGuid(),
            PhoneE164 = normalized,
            CodeHash = Hashing.Sha256(code),
            ExpiresAt = DateTimeOffset.UtcNow.AddMinutes(_options.ExpiryMinutes),
            CreatedAt = DateTimeOffset.UtcNow,
        };

        db.OtpChallenges.Add(challenge);
        await db.SaveChangesAsync(ct);

        if (_options.DevelopmentMode)
        {
            logger.LogInformation("OTP for {Phone}: {Code}", normalized, code);
            return (true, code);
        }

        logger.LogInformation("OTP requested for {Phone}", normalized);
        return (true, null);
    }

    public async Task<User?> VerifyOtpAsync(string phoneE164, string code, CancellationToken ct)
    {
        var normalized = NormalizePhone(phoneE164);
        if (normalized is null) return null;

        var now = DateTimeOffset.UtcNow;
        var challenges = await db.OtpChallenges
            .Where(x => x.PhoneE164 == normalized && !x.Consumed)
            .OrderByDescending(x => x.CreatedAt)
            .Take(5)
            .ToListAsync(ct);
        var challenge = challenges.FirstOrDefault(x => x.ExpiresAt > now);

        if (challenge is null) return null;
        if (challenge.AttemptCount >= _options.MaxAttempts) return null;
        challenge.AttemptCount++;

        if (challenge.CodeHash != Hashing.Sha256(code))
        {
            await db.SaveChangesAsync(ct);
            return null;
        }

        challenge.Consumed = true;
        var phoneHash = Hashing.PhoneHash(normalized, PhoneSalt);
        var user = await db.Users.FirstOrDefaultAsync(x => x.PhoneNumberHash == phoneHash, ct);
        if (user is null)
        {
            user = new User
            {
                Id = Guid.NewGuid(),
                PhoneNumberHash = phoneHash,
                CreatedAt = DateTimeOffset.UtcNow,
                LastActiveAt = DateTimeOffset.UtcNow,
            };
            db.Users.Add(user);
        }
        else
        {
            user.LastActiveAt = DateTimeOffset.UtcNow;
        }

        await db.SaveChangesAsync(ct);
        return user;
    }

    private string GenerateCode()
    {
        var max = (int)Math.Pow(10, _options.CodeLength);
        var value = RandomNumberGenerator.GetInt32(0, max);
        return value.ToString($"D{_options.CodeLength}");
    }

    public static string? NormalizePhone(string phone)
    {
        var trimmed = phone.Trim();
        if (!trimmed.StartsWith('+')) return null;
        var digits = new string(trimmed.Where(c => char.IsDigit(c) || c == '+').ToArray());
        return digits.Length >= 8 ? digits : null;
    }
}

public class SpamRuleService(KheyrDbContext db)
{
    public async Task<SpamRuleVersion?> GetLatestPublishedAsync(CancellationToken ct)
        => await db.SpamRuleVersions
            .Where(x => x.IsPublished)
            .OrderByDescending(x => x.Version)
            .FirstOrDefaultAsync(ct);

    public async Task<SpamRuleVersion> PublishAsync(int version, int threshold, IEnumerable<SpamRuleDto> rules, string? createdBy, string? notes, CancellationToken ct)
    {
        ValidateRules(rules, threshold);
        var rulesJson = JsonSerializer.Serialize(rules);

        var existing = await db.SpamRuleVersions.FirstOrDefaultAsync(x => x.Version == version, ct);
        if (existing is not null)
        {
            existing.Threshold = threshold;
            existing.RulesJson = rulesJson;
            existing.IsPublished = true;
            existing.Notes = notes;
            existing.CreatedBy = createdBy;
            await UnpublishOthersAsync(version, ct);
            await db.SaveChangesAsync(ct);
            return existing;
        }

        var entity = new SpamRuleVersion
        {
            Id = Guid.NewGuid(),
            Version = version,
            Threshold = threshold,
            RulesJson = rulesJson,
            IsPublished = true,
            CreatedAt = DateTimeOffset.UtcNow,
            CreatedBy = createdBy,
            Notes = notes,
        };
        db.SpamRuleVersions.Add(entity);
        await UnpublishOthersAsync(version, ct);
        await db.SaveChangesAsync(ct);
        return entity;
    }

    public async Task<SpamRuleVersion?> RollbackAsync(int version, CancellationToken ct)
    {
        var target = await db.SpamRuleVersions.FirstOrDefaultAsync(x => x.Version == version, ct)
            ?? throw new InvalidOperationException($"Rule version {version} not found.");

        target.IsPublished = true;
        await UnpublishOthersAsync(version, ct);
        await db.SaveChangesAsync(ct);
        return target;
    }

    private async Task UnpublishOthersAsync(int publishedVersion, CancellationToken ct)
    {
        await db.SpamRuleVersions
            .Where(x => x.Version != publishedVersion && x.IsPublished)
            .ExecuteUpdateAsync(s => s.SetProperty(x => x.IsPublished, false), ct);
    }

    public static void ValidateRules(IEnumerable<SpamRuleDto> rules, int threshold)
    {
        if (threshold is < 0 or > 100) throw new ArgumentException("Threshold must be between 0 and 100.");
        foreach (var rule in rules)
        {
            if (string.IsNullOrWhiteSpace(rule.Id)) throw new ArgumentException("Rule id is required.");
            if (string.IsNullOrWhiteSpace(rule.Type)) throw new ArgumentException($"Rule {rule.Id} type is required.");
            if (rule.Score is < -100 or > 100) throw new ArgumentException($"Rule {rule.Id} score out of range.");
        }
    }

    public static List<SpamRuleDto> ParseRules(string rulesJson)
        => JsonSerializer.Deserialize<List<SpamRuleDto>>(rulesJson) ?? [];
}

public record SpamRuleDto(string Id, string Type, string? Pattern, int Score, bool Enabled = true);

public class SyncService(KheyrDbContext db)
{
    public async Task<long> AppendChangeAsync(Guid userId, SyncChangeType changeType, object payload, CancellationToken ct)
    {
        var maxSeq = await db.SyncChanges.Where(x => x.UserId == userId).MaxAsync(x => (long?)x.Sequence, ct) ?? 0;
        var change = new SyncChange
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            Sequence = maxSeq + 1,
            ChangeType = changeType,
            PayloadJson = JsonSerializer.Serialize(payload),
            CreatedAt = DateTimeOffset.UtcNow,
        };
        db.SyncChanges.Add(change);
        await db.SaveChangesAsync(ct);
        return change.Sequence;
    }

    public async Task<(List<SyncChange> Changes, long? NextCursor, bool HasMore)> GetUpdatesAsync(
        Guid userId, long? cursor, int pageSize, CancellationToken ct)
    {
        var query = db.SyncChanges.Where(x => x.UserId == userId);
        if (cursor.HasValue)
            query = query.Where(x => x.Sequence > cursor.Value);

        var changes = await query
            .OrderBy(x => x.Sequence)
            .Take(pageSize + 1)
            .ToListAsync(ct);

        var hasMore = changes.Count > pageSize;
        if (hasMore) changes = changes.Take(pageSize).ToList();
        var nextCursor = changes.Count == 0 ? cursor : changes[^1].Sequence;
        return (changes, nextCursor, hasMore);
    }

    public async Task UpsertCursorAsync(Guid userId, Guid deviceId, long cursor, CancellationToken ct)
    {
        var existing = await db.SyncCursors.FirstOrDefaultAsync(x => x.UserId == userId && x.DeviceId == deviceId, ct);
        if (existing is null)
        {
            db.SyncCursors.Add(new SyncCursor
            {
                Id = Guid.NewGuid(),
                UserId = userId,
                DeviceId = deviceId,
                Cursor = cursor,
                LastSyncedAt = DateTimeOffset.UtcNow,
            });
        }
        else
        {
            existing.Cursor = cursor;
            existing.LastSyncedAt = DateTimeOffset.UtcNow;
        }

        await db.SaveChangesAsync(ct);
    }
}

public class PairingService(
    KheyrDbContext db,
    IOptions<PairingOptions> pairingOptions)
{
    private readonly PairingOptions _options = pairingOptions.Value;

    public async Task<PairingSession> CreateSessionAsync(string? desktopName, string? platform, string? publicKey, CancellationToken ct)
    {
        var sessionId = Guid.NewGuid();
        var expires = DateTimeOffset.UtcNow.AddMinutes(_options.SessionExpiryMinutes);
        var qrPayload = JsonSerializer.Serialize(new
        {
            session_id = sessionId,
            server = "kheyr",
            expires_at = expires.ToUnixTimeSeconds(),
        });

        var session = new PairingSession
        {
            Id = sessionId,
            QrPayload = qrPayload,
            Status = PairingSessionStatus.Pending,
            CreatedAt = DateTimeOffset.UtcNow,
            ExpiresAt = expires,
            DesktopDeviceName = desktopName,
            DesktopPlatform = platform,
            DesktopPublicKey = publicKey,
        };
        db.PairingSessions.Add(session);
        await db.SaveChangesAsync(ct);
        return session;
    }

    public async Task<(Device DesktopDevice, PairingSession Session)?> ApproveAsync(
        Guid userId, Guid sessionId, string deviceName, CancellationToken ct)
    {
        var session = await db.PairingSessions.FirstOrDefaultAsync(x => x.Id == sessionId, ct);
        if (session is null || session.Status != PairingSessionStatus.Pending) return null;
        if (session.ExpiresAt <= DateTimeOffset.UtcNow)
        {
            session.Status = PairingSessionStatus.Expired;
            await db.SaveChangesAsync(ct);
            return null;
        }

        var device = new Device
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            DeviceName = deviceName,
            DeviceType = DeviceType.Desktop,
            Platform = session.DesktopPlatform ?? "unknown",
            PublicKey = session.DesktopPublicKey,
            CreatedAt = DateTimeOffset.UtcNow,
            LastActiveAt = DateTimeOffset.UtcNow,
        };
        db.Devices.Add(device);

        session.UserId = userId;
        session.DesktopDeviceId = device.Id;
        session.Status = PairingSessionStatus.Approved;
        session.ApprovedAt = DateTimeOffset.UtcNow;
        session.DesktopDeviceName = deviceName;

        await db.SaveChangesAsync(ct);
        return (device, session);
    }
}

public class DesktopRelayService(
    KheyrDbContext db,
    IOptions<DesktopRelayOptions> relayOptions)
{
    private readonly DesktopRelayOptions _options = relayOptions.Value;

    public async Task<DesktopSmsRequest> CreateRequestAsync(
        Guid userId, Guid desktopDeviceId, string encryptedBody, string encryptedTarget,
        string? simId, long? threadClientId, string? clientMessageId, CancellationToken ct)
    {
        var android = await db.Devices
            .Where(x => x.UserId == userId && x.DeviceType == DeviceType.Android && x.RevokedAt == null)
            .OrderByDescending(x => x.LastActiveAt)
            .FirstOrDefaultAsync(ct);

        var request = new DesktopSmsRequest
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            DesktopDeviceId = desktopDeviceId,
            AndroidDeviceId = android?.Id,
            EncryptedMessageBody = encryptedBody,
            EncryptedTargetNumber = encryptedTarget,
            SimId = simId,
            ThreadClientId = threadClientId,
            ClientMessageId = clientMessageId,
            Status = android is null ? DesktopSmsRequestStatus.WaitingForPhone : DesktopSmsRequestStatus.Queued,
            CreatedAt = DateTimeOffset.UtcNow,
            ExpiresAt = DateTimeOffset.UtcNow.AddMinutes(_options.RequestExpiryMinutes),
        };
        db.DesktopSmsRequests.Add(request);
        await db.SaveChangesAsync(ct);
        return request;
    }

    public async Task ExpireStaleRequestsAsync(CancellationToken ct)
    {
        var now = DateTimeOffset.UtcNow;
        await db.DesktopSmsRequests
            .Where(x => x.Status == DesktopSmsRequestStatus.Queued || x.Status == DesktopSmsRequestStatus.WaitingForPhone)
            .Where(x => x.ExpiresAt < now)
            .ExecuteUpdateAsync(s => s
                .SetProperty(x => x.Status, DesktopSmsRequestStatus.Expired)
                .SetProperty(x => x.CompletedAt, now), ct);
    }
}

public class MetricsService(KheyrDbContext db)
{
    public async Task RecordAsync(string metricName, double value, CancellationToken ct)
    {
        db.SystemMetricSnapshots.Add(new SystemMetricSnapshot
        {
            Id = Guid.NewGuid(),
            MetricName = metricName,
            Value = value,
            RecordedAt = DateTimeOffset.UtcNow,
        });
        await db.SaveChangesAsync(ct);
    }

    public async Task<Dictionary<string, double>> GetLatestAsync(CancellationToken ct)
    {
        var metrics = await db.SystemMetricSnapshots
            .GroupBy(x => x.MetricName)
            .Select(g => new { MetricName = g.Key, Value = g.OrderByDescending(x => x.RecordedAt).First().Value })
            .ToListAsync(ct);
        return metrics.ToDictionary(x => x.MetricName, x => x.Value);
    }

    public async Task<SystemHealthSummary> GetHealthSummaryAsync(CancellationToken ct)
    {
        var now = DateTimeOffset.UtcNow;
        var weekAgo = now.AddDays(-7);
        var users = await db.Users.Where(x => x.DeletedAt == null).ToListAsync(ct);
        var activeUsers = users.Count(x => x.LastActiveAt > weekAgo);
        var devices = await db.Devices.CountAsync(x => x.RevokedAt == null, ct);
        var syncEnabled = users.Count(x => x.SyncEnabled);
        var relayTotal = await db.DesktopSmsRequests.CountAsync(ct);
        var relayFailed = await db.DesktopSmsRequests.CountAsync(x => x.Status == DesktopSmsRequestStatus.Failed, ct);
        var feedbackSpam = await db.SpamFeedback.CountAsync(x => x.UserAction == SpamFeedbackAction.Spam, ct);
        var feedbackNotSpam = await db.SpamFeedback.CountAsync(x => x.UserAction == SpamFeedbackAction.NotSpam, ct);

        return new SystemHealthSummary(activeUsers, devices, syncEnabled, relayTotal, relayFailed, feedbackSpam, feedbackNotSpam);
    }
}

public record SystemHealthSummary(
    int ActiveUsers,
    int Devices,
    int SyncEnabledUsers,
    int RelayTotal,
    int RelayFailed,
    int FeedbackSpam,
    int FeedbackNotSpam);

public class PrivacyService(KheyrDbContext db, SyncService syncService)
{
    public async Task DeleteCloudDataAsync(Guid userId, CancellationToken ct)
    {
        await db.Messages.Where(x => x.UserId == userId).ExecuteDeleteAsync(ct);
        await db.Threads.Where(x => x.UserId == userId).ExecuteDeleteAsync(ct);
        await db.SyncChanges.Where(x => x.UserId == userId).ExecuteDeleteAsync(ct);
        await db.SyncCursors.Where(x => x.UserId == userId).ExecuteDeleteAsync(ct);
        await db.DesktopSmsRequests.Where(x => x.UserId == userId).ExecuteDeleteAsync(ct);
        await db.DirectMessages.Where(x => x.SenderUserId == userId || x.RecipientUserId == userId).ExecuteDeleteAsync(ct);

        var user = await db.Users.FirstAsync(x => x.Id == userId, ct);
        user.SyncEnabled = false;
        await db.SaveChangesAsync(ct);
        await syncService.AppendChangeAsync(userId, SyncChangeType.ThreadDeleted, new { all = true }, ct);
    }

    public async Task<object> ExportCloudDataAsync(Guid userId, CancellationToken ct)
    {
        var threads = await db.Threads.Where(x => x.UserId == userId && x.DeletedAt == null)
            .Select(x => new
            {
                x.ClientThreadId,
                x.EncryptedPhoneNumber,
                x.PhoneNumberHash,
                x.IsSpam,
                x.IsArchived,
                x.IsPinned,
                x.UnreadCount,
            }).ToListAsync(ct);

        var messages = await db.Messages.Where(x => x.UserId == userId && !x.IsDeleted)
            .Select(x => new
            {
                x.ClientMessageId,
                thread_id = x.Thread.ClientThreadId,
                x.EncryptedBody,
                x.Direction,
                x.Status,
                x.Timestamp,
            }).ToListAsync(ct);

        return new
        {
            exported_at = DateTimeOffset.UtcNow,
            user_id = userId,
            threads,
            messages,
            note = "SMS bodies are client-encrypted ciphertext only.",
        };
    }
}
