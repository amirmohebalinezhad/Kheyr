namespace Kheyr.Domain.Options;

public class JwtOptions
{
    public const string SectionName = "Jwt";
    public string Issuer { get; set; } = "kheyr";
    public string Audience { get; set; } = "kheyr-clients";
    public string SigningKey { get; set; } = "CHANGE-ME-use-a-long-random-secret-in-production";
    public int AccessTokenMinutes { get; set; } = 60;
    public int RefreshTokenDays { get; set; } = 30;
}

public class OtpOptions
{
    public const string SectionName = "Otp";
    public int CodeLength { get; set; } = 6;
    public int ExpiryMinutes { get; set; } = 10;
    public int MaxAttempts { get; set; } = 5;
    /// <summary>When true, OTP codes are logged and returned in API responses for local development.</summary>
    public bool DevelopmentMode { get; set; } = true;
    public string? FixedCode { get; set; }
}

public class PairingOptions
{
    public const string SectionName = "Pairing";
    public int SessionExpiryMinutes { get; set; } = 10;
}

public class DesktopRelayOptions
{
    public const string SectionName = "DesktopRelay";
    public int RequestExpiryMinutes { get; set; } = 30;
}
