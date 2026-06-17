using System.Security.Claims;
using System.Text;
using System.Text.Json;
using Kheyr.Domain;
using Kheyr.Domain.Entities;
using Kheyr.Infrastructure.Data;
using Kheyr.Infrastructure.Hubs;
using Kheyr.Infrastructure.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;

namespace Kheyr.Api.Controllers;

[ApiController]
[Route("api/v1/auth")]
public class AuthController(
    OtpService otpService,
    TokenService tokenService,
    DeviceService deviceService,
    KheyrDbContext db) : ControllerBase
{
    [HttpPost("otp/request")]
    [AllowAnonymous]
    public async Task<IActionResult> RequestOtp([FromBody] OtpRequestDto dto, CancellationToken ct)
    {
        var (success, devCode) = await otpService.RequestOtpAsync(dto.Phone, ct);
        if (!success) return BadRequest(new { error = "invalid_phone" });
        return Ok(new { sent = true, development_code = devCode });
    }

    [HttpPost("otp/verify")]
    [AllowAnonymous]
    public async Task<IActionResult> VerifyOtp([FromBody] OtpVerifyDto dto, CancellationToken ct)
    {
        var user = await otpService.VerifyOtpAsync(dto.Phone, dto.Code, ct);
        if (user is null) return Unauthorized(new { error = "invalid_code" });

        var device = await db.Devices
            .Where(x => x.UserId == user.Id && x.RevokedAt == null)
            .OrderByDescending(x => x.LastActiveAt)
            .FirstOrDefaultAsync(ct)
            ?? await deviceService.RegisterAsync(user.Id, "Android", "android", "android", null, null, ct);

        return Ok(await IssueTokensAsync(user, device, ct));
    }

    [HttpPost("refresh")]
    [AllowAnonymous]
    public async Task<IActionResult> Refresh([FromBody] RefreshDto dto, CancellationToken ct)
    {
        var hash = tokenService.HashRefreshToken(dto.RefreshToken);
        var now = DateTimeOffset.UtcNow;
        var tokens = await db.RefreshTokens
            .Include(x => x.User)
            .Include(x => x.Device)
            .Where(x => x.TokenHash == hash && x.RevokedAt == null)
            .ToListAsync(ct);
        var stored = tokens.FirstOrDefault(x => x.ExpiresAt > now);
        if (stored is null || stored.Device.RevokedAt is not null) return Unauthorized();

        stored.RevokedAt = DateTimeOffset.UtcNow;
        return Ok(await IssueTokensAsync(stored.User, stored.Device, ct));
    }

    [HttpPost("logout")]
    [Authorize]
    public async Task<IActionResult> Logout(CancellationToken ct)
    {
        var deviceId = Guid.Parse(User.FindFirstValue("device_id")!);
        await db.RefreshTokens.Where(x => x.DeviceId == deviceId && x.RevokedAt == null)
            .ExecuteUpdateAsync(s => s.SetProperty(x => x.RevokedAt, DateTimeOffset.UtcNow), ct);
        return Ok(new { logged_out = true });
    }

    private async Task<object> IssueTokensAsync(User user, Device device, CancellationToken ct)
    {
        var (accessToken, expiresAt) = tokenService.CreateAccessToken(user.Id, device.Id, device.DeviceType);
        var refreshValue = tokenService.CreateRefreshTokenValue();
        db.RefreshTokens.Add(new RefreshToken
        {
            Id = Guid.NewGuid(),
            UserId = user.Id,
            DeviceId = device.Id,
            TokenHash = tokenService.HashRefreshToken(refreshValue),
            ExpiresAt = DateTimeOffset.UtcNow.AddDays(30),
            CreatedAt = DateTimeOffset.UtcNow,
        });
        user.LastActiveAt = DateTimeOffset.UtcNow;
        device.LastActiveAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(ct);
        return new
        {
            access_token = accessToken,
            refresh_token = refreshValue,
            expires_in = (long)(expiresAt - DateTimeOffset.UtcNow).TotalSeconds,
            device_id = device.Id,
        };
    }
}

public record OtpRequestDto(string Phone);
public record OtpVerifyDto(string Phone, string Code);
public record RefreshDto([property: System.Text.Json.Serialization.JsonPropertyName("refresh_token")] string RefreshToken);

[ApiController]
[Route("api/v1/devices")]
[Authorize]
public class DevicesController(DeviceService deviceService, KheyrDbContext db) : ControllerBase
{
    [HttpPost]
    public async Task<IActionResult> Register([FromBody] DeviceRegisterDto dto, CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var device = await deviceService.RegisterAsync(userId, dto.DeviceName, dto.DeviceType, dto.Platform, dto.PushToken, dto.PublicKey, ct);
        return Ok(new
        {
            device_id = device.Id,
            device_name = device.DeviceName,
            device_type = device.DeviceType.ToString().ToLowerInvariant(),
            platform = device.Platform,
        });
    }

    [HttpGet]
    public async Task<IActionResult> List(CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var devices = await deviceService.ListActiveAsync(userId, ct);
        return Ok(new
        {
            devices = devices.Select(d => new
            {
                device_id = d.Id,
                device_name = d.DeviceName,
                device_type = d.DeviceType.ToString().ToLowerInvariant(),
                platform = d.Platform,
                last_active_at = d.LastActiveAt.ToUnixTimeSeconds(),
            }),
        });
    }
}

public record DeviceRegisterDto(
    [property: System.Text.Json.Serialization.JsonPropertyName("device_name")] string DeviceName,
    [property: System.Text.Json.Serialization.JsonPropertyName("device_type")] string DeviceType,
    string Platform,
    [property: System.Text.Json.Serialization.JsonPropertyName("push_token")] string? PushToken,
    [property: System.Text.Json.Serialization.JsonPropertyName("public_key")] string? PublicKey);

[ApiController]
[Route("api/v1/spam-rules")]
public class SpamRulesController(SpamRuleService spamRules) : ControllerBase
{
    [HttpGet("latest")]
    [AllowAnonymous]
    public async Task<IActionResult> Latest(CancellationToken ct)
    {
        var version = await spamRules.GetLatestPublishedAsync(ct);
        if (version is null) return NotFound();
        var rules = SpamRuleService.ParseRules(version.RulesJson);
        return Ok(new
        {
            version = version.Version,
            threshold = version.Threshold,
            rules = rules.Select(r => new { id = r.Id, type = r.Type, pattern = r.Pattern, score = r.Score, enabled = r.Enabled }),
            created_at = version.CreatedAt.ToUnixTimeSeconds(),
        });
    }
}

[ApiController]
[Route("api/v1/spam-feedback")]
[Authorize]
public class SpamFeedbackController(KheyrDbContext db, MetricsService metrics) : ControllerBase
{
    [HttpPost]
    public async Task<IActionResult> Submit([FromBody] SpamFeedbackDto dto, CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        db.SpamFeedback.Add(new SpamFeedback
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            SenderHash = dto.SenderHash,
            MessageHash = dto.MessageHash,
            RuleVersion = dto.RuleVersion,
            SpamScore = dto.SpamScore,
            UserAction = dto.UserAction.Equals("not_spam", StringComparison.OrdinalIgnoreCase)
                ? SpamFeedbackAction.NotSpam : SpamFeedbackAction.Spam,
            TriggeredRuleIdsJson = JsonSerializer.Serialize(dto.TriggeredRuleIds ?? []),
            CreatedAt = DateTimeOffset.UtcNow,
        });
        await db.SaveChangesAsync(ct);
        await metrics.RecordAsync($"spam_feedback.{dto.UserAction.ToLowerInvariant()}", 1, ct);
        return Ok(new { accepted = true });
    }
}

public record SpamFeedbackDto(
    [property: System.Text.Json.Serialization.JsonPropertyName("sender_hash")] string SenderHash,
    [property: System.Text.Json.Serialization.JsonPropertyName("message_hash")] string MessageHash,
    [property: System.Text.Json.Serialization.JsonPropertyName("rule_version")] int RuleVersion,
    [property: System.Text.Json.Serialization.JsonPropertyName("spam_score")] int SpamScore,
    [property: System.Text.Json.Serialization.JsonPropertyName("user_action")] string UserAction,
    [property: System.Text.Json.Serialization.JsonPropertyName("triggered_rule_ids")] string[]? TriggeredRuleIds);

[ApiController]
[Route("api/v1/sync")]
[Authorize]
public class SyncController(SyncUploadService uploadService, SyncService syncService) : ControllerBase
{
    [HttpPost("initial")]
    public async Task<IActionResult> Initial([FromBody] JsonElement body, CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var deviceId = body.GetProperty("device_id").GetGuid();
        var threads = body.TryGetProperty("encrypted_threads", out var t) ? t : default;
        var messages = body.TryGetProperty("encrypted_messages", out var m) ? m : default;
        await uploadService.ProcessInitialSyncAsync(userId, deviceId, threads, messages, ct);
        return Ok(new { accepted = true });
    }

    [HttpPost("upload")]
    public async Task<IActionResult> Upload([FromBody] JsonElement body, CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        Guid? deviceId = body.TryGetProperty("device_id", out var d) ? d.GetGuid() : null;
        var changes = body.GetProperty("changes");
        await uploadService.ProcessUploadAsync(userId, deviceId, changes, ct);
        return Ok(new { accepted = true });
    }

    [HttpGet("updates")]
    public async Task<IActionResult> Updates([FromQuery] long? cursor, CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var (changes, nextCursor, hasMore) = await syncService.GetUpdatesAsync(userId, cursor, 100, ct);
        return Ok(new
        {
            changes = changes.Select(c => new
            {
                sequence = c.Sequence,
                type = c.ChangeType.ToString(),
                payload = JsonSerializer.Deserialize<JsonElement>(c.PayloadJson),
                created_at = c.CreatedAt.ToUnixTimeSeconds(),
            }),
            next_cursor = nextCursor?.ToString(),
            has_more = hasMore,
        });
    }
}

[ApiController]
[Route("api/v1/pairing")]
public class PairingController(
    PairingService pairingService,
    DeviceService deviceService,
    TokenService tokenService,
    KheyrDbContext db,
    IHubContext<KheyrHub> hub) : ControllerBase
{
    [HttpPost("session")]
    [AllowAnonymous]
    public async Task<IActionResult> CreateSession([FromBody] PairingSessionRequestDto? dto, CancellationToken ct)
    {
        var session = await pairingService.CreateSessionAsync(dto?.DeviceName, dto?.Platform, dto?.PublicKey, ct);
        return Ok(new
        {
            session_id = session.Id,
            qr_payload = session.QrPayload,
            expires_at = session.ExpiresAt.ToUnixTimeSeconds(),
        });
    }

    [HttpPost("approve")]
    [Authorize]
    public async Task<IActionResult> Approve([FromBody] PairingApproveDto dto, CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var result = await pairingService.ApproveAsync(userId, dto.SessionId, dto.DeviceName, ct);
        if (result is null) return BadRequest(new { error = "invalid_or_expired_session" });

        var (desktop, session) = result.Value;
        var tokens = await IssueDesktopTokensAsync(desktop, ct);
        await hub.Clients.Group($"pairing:{session.Id}").SendAsync("PairingApproved", tokens, ct);
        return Ok(new
        {
            desktop_device_id = desktop.Id,
            session_id = session.Id,
            approved = true,
        });
    }

    [HttpPost("revoke")]
    [Authorize]
    public async Task<IActionResult> Revoke([FromBody] RevokeDeviceDto dto, CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var ok = await deviceService.RevokeAsync(userId, dto.DeviceId, ct);
        if (!ok) return NotFound();
        await hub.Clients.Group($"device:{dto.DeviceId}").SendAsync("DeviceRevoked", ct);
        return Ok(new { revoked = true });
    }

    private async Task<object> IssueDesktopTokensAsync(Device device, CancellationToken ct)
    {
        var (accessToken, expiresAt) = tokenService.CreateAccessToken(device.UserId, device.Id, device.DeviceType);
        var refreshValue = tokenService.CreateRefreshTokenValue();
        db.RefreshTokens.Add(new RefreshToken
        {
            Id = Guid.NewGuid(),
            UserId = device.UserId,
            DeviceId = device.Id,
            TokenHash = tokenService.HashRefreshToken(refreshValue),
            ExpiresAt = DateTimeOffset.UtcNow.AddDays(30),
            CreatedAt = DateTimeOffset.UtcNow,
        });
        await db.SaveChangesAsync(ct);
        return new
        {
            access_token = accessToken,
            refresh_token = refreshValue,
            expires_in = (long)(expiresAt - DateTimeOffset.UtcNow).TotalSeconds,
            device_id = device.Id,
        };
    }
}

public record PairingSessionRequestDto(
    [property: System.Text.Json.Serialization.JsonPropertyName("device_name")] string? DeviceName,
    string? Platform,
    [property: System.Text.Json.Serialization.JsonPropertyName("public_key")] string? PublicKey);
public record PairingApproveDto(
    [property: System.Text.Json.Serialization.JsonPropertyName("session_id")] Guid SessionId,
    [property: System.Text.Json.Serialization.JsonPropertyName("device_name")] string DeviceName);
public record RevokeDeviceDto([property: System.Text.Json.Serialization.JsonPropertyName("device_id")] Guid DeviceId);

[ApiController]
[Route("api/v1/desktop/sms")]
[Authorize]
public class DesktopSmsController(
    DesktopRelayService relayService,
    KheyrDbContext db,
    IHubContext<KheyrHub> hub,
    MetricsService metrics) : ControllerBase
{
    [HttpPost("send")]
    public async Task<IActionResult> Send([FromBody] DesktopSmsSendDto dto, CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var request = await relayService.CreateRequestAsync(
            userId, dto.DesktopDeviceId, dto.EncryptedMessageBody, dto.EncryptedTargetNumber,
            dto.SimId, dto.ThreadId, dto.ClientMessageId, ct);

        if (request.AndroidDeviceId is Guid androidId)
        {
            await hub.Clients.Group($"device:{androidId}").SendAsync("DesktopSmsRequest", new
            {
                request_id = request.Id,
                encrypted_message_body = request.EncryptedMessageBody,
                encrypted_target_number = request.EncryptedTargetNumber,
                sim_id = request.SimId,
                thread_id = request.ThreadClientId,
                client_message_id = request.ClientMessageId,
            }, ct);
        }

        await metrics.RecordAsync("desktop_relay.created", 1, ct);
        return Ok(new
        {
            request_id = request.Id,
            status = request.Status.ToString(),
            expires_at = request.ExpiresAt.ToUnixTimeSeconds(),
        });
    }

    [HttpPost("status")]
    [Authorize]
    public async Task<IActionResult> UpdateStatus([FromBody] DesktopSmsStatusDto dto, CancellationToken ct)
    {
        var request = await db.DesktopSmsRequests.FirstOrDefaultAsync(x => x.Id == dto.RequestId, ct);
        if (request is null) return NotFound();
        if (Enum.TryParse<DesktopSmsRequestStatus>(dto.Status, true, out var status))
            request.Status = status;
        request.FailureReason = dto.FailureReason;
        if (status is DesktopSmsRequestStatus.Sent or DesktopSmsRequestStatus.Failed or DesktopSmsRequestStatus.Expired)
            request.CompletedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(ct);

        await hub.Clients.Group($"device:{request.DesktopDeviceId}").SendAsync("DesktopSmsStatus", new
        {
            request_id = request.Id,
            status = request.Status.ToString(),
            failure_reason = request.FailureReason,
        }, ct);

        await metrics.RecordAsync($"desktop_relay.{request.Status.ToString().ToLowerInvariant()}", 1, ct);
        return Ok(new { updated = true });
    }
}

public record DesktopSmsSendDto(
    [property: System.Text.Json.Serialization.JsonPropertyName("desktop_device_id")] Guid DesktopDeviceId,
    [property: System.Text.Json.Serialization.JsonPropertyName("encrypted_target_number")] string EncryptedTargetNumber,
    [property: System.Text.Json.Serialization.JsonPropertyName("encrypted_message_body")] string EncryptedMessageBody,
    [property: System.Text.Json.Serialization.JsonPropertyName("sim_id")] string? SimId,
    [property: System.Text.Json.Serialization.JsonPropertyName("thread_id")] long? ThreadId,
    [property: System.Text.Json.Serialization.JsonPropertyName("client_message_id")] string? ClientMessageId);
public record DesktopSmsStatusDto(
    [property: System.Text.Json.Serialization.JsonPropertyName("request_id")] Guid RequestId,
    string Status,
    [property: System.Text.Json.Serialization.JsonPropertyName("failure_reason")] string? FailureReason);

[ApiController]
[Route("api/v1/direct")]
[Authorize]
public class DirectMessagesController(DirectMessageService directMessages, IHubContext<KheyrHub> hub) : ControllerBase
{
    [HttpPost("messages")]
    public async Task<IActionResult> Send([FromBody] DirectMessageDto dto, CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var message = await directMessages.SendAsync(
            userId, dto.RecipientPhoneHash, dto.MessageType, dto.Body,
            dto.AttachmentId, dto.ClientMessageId, ct);

        if (message.RecipientUserId is Guid recipientId)
        {
            await hub.Clients.Group($"user:{recipientId}").SendAsync("DirectMessage", new
            {
                message_id = message.Id,
                sender_user_id = message.SenderUserId,
                body = message.Body,
                message_type = message.MessageType.ToString(),
                client_message_id = message.ClientMessageId,
            }, ct);
        }

        return Ok(new
        {
            message_id = message.Id,
            status = message.Status.ToString(),
            client_message_id = message.ClientMessageId,
        });
    }
}

public record DirectMessageDto(
    [property: System.Text.Json.Serialization.JsonPropertyName("recipient_phone_hash")] string RecipientPhoneHash,
    [property: System.Text.Json.Serialization.JsonPropertyName("message_type")] string MessageType,
    string Body,
    [property: System.Text.Json.Serialization.JsonPropertyName("attachment_id")] string? AttachmentId,
    [property: System.Text.Json.Serialization.JsonPropertyName("client_message_id")] string ClientMessageId);

[ApiController]
[Route("api/v1/privacy")]
[Authorize]
public class PrivacyController(PrivacyService privacy, KheyrDbContext db) : ControllerBase
{
    [HttpPost("delete")]
    public async Task<IActionResult> DeleteCloudData(CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        await privacy.DeleteCloudDataAsync(userId, ct);
        return Ok(new { deleted = true });
    }

    [HttpGet("export")]
    public async Task<IActionResult> ExportCloudData(CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var data = await privacy.ExportCloudDataAsync(userId, ct);
        return Ok(data);
    }
}

[ApiController]
[Route("api/v1/account")]
[Authorize]
public class AccountController(KheyrDbContext db, PrivacyService privacy, DeviceService deviceService) : ControllerBase
{
    [HttpDelete]
    public async Task<IActionResult> DeleteAccount(CancellationToken ct)
    {
        var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        await privacy.DeleteCloudDataAsync(userId, ct);
        var user = await db.Users.FirstAsync(x => x.Id == userId, ct);
        user.DeletedAt = DateTimeOffset.UtcNow;
        var devices = await db.Devices.Where(x => x.UserId == userId && x.RevokedAt == null).ToListAsync(ct);
        foreach (var device in devices)
            await deviceService.RevokeAsync(userId, device.Id, ct);
        await db.SaveChangesAsync(ct);
        return Ok(new { deleted = true });
    }
}

[ApiController]
[Route("api/v1/health")]
[AllowAnonymous]
public class HealthController(MetricsService metrics) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> Get(CancellationToken ct)
    {
        var summary = await metrics.GetHealthSummaryAsync(ct);
        return Ok(new
        {
            status = "healthy",
            timestamp = DateTimeOffset.UtcNow,
            metrics = summary,
        });
    }
}
