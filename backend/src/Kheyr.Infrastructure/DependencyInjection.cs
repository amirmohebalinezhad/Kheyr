using System.Security.Cryptography;
using System.Text;
using Kheyr.Domain.Entities;
using Kheyr.Domain.Options;
using Kheyr.Infrastructure.Data;
using Kheyr.Infrastructure.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace Kheyr.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AddKheyrInfrastructure(this IServiceCollection services, string connectionString, bool useSqlite = false)
    {
        services.AddDbContext<KheyrDbContext>(options =>
        {
            if (useSqlite)
                options.UseSqlite(connectionString);
            else
                options.UseNpgsql(connectionString);
        });

        services.AddScoped<TokenService>();
        services.AddScoped<OtpService>();
        services.AddScoped<SpamRuleService>();
        services.AddScoped<SyncService>();
        services.AddScoped<SyncUploadService>();
        services.AddScoped<PairingService>();
        services.AddScoped<DesktopRelayService>();
        services.AddScoped<MetricsService>();
        services.AddScoped<PrivacyService>();
        services.AddScoped<DeviceService>();
        services.AddScoped<DirectMessageService>();
        services.AddHostedService<DatabaseInitializer>();
        services.AddHostedService<RelayExpiryWorker>();

        return services;
    }

    public static IServiceCollection AddKheyrOptions(this IServiceCollection services, Microsoft.Extensions.Configuration.IConfiguration configuration)
    {
        services.Configure<JwtOptions>(configuration.GetSection(JwtOptions.SectionName));
        services.Configure<OtpOptions>(configuration.GetSection(OtpOptions.SectionName));
        services.Configure<PairingOptions>(configuration.GetSection(PairingOptions.SectionName));
        services.Configure<DesktopRelayOptions>(configuration.GetSection(DesktopRelayOptions.SectionName));
        return services;
    }
}

public class DatabaseInitializer(IServiceScopeFactory scopeFactory) : IHostedService
{
    public async Task StartAsync(CancellationToken cancellationToken)
    {
        using var scope = scopeFactory.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<KheyrDbContext>();
        await db.Database.EnsureCreatedAsync(cancellationToken);
        await SeedData.SeedAsync(db, cancellationToken);
    }

    public Task StopAsync(CancellationToken cancellationToken) => Task.CompletedTask;
}

public class RelayExpiryWorker(IServiceScopeFactory scopeFactory) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                using var scope = scopeFactory.CreateScope();
                var relay = scope.ServiceProvider.GetRequiredService<DesktopRelayService>();
                await relay.ExpireStaleRequestsAsync(stoppingToken);
            }
            catch
            {
                // ignore background errors
            }

            await Task.Delay(TimeSpan.FromMinutes(5), stoppingToken);
        }
    }
}

public static class SeedData
{
    public static async Task SeedAsync(KheyrDbContext db, CancellationToken ct)
    {
        if (!await db.SpamRuleVersions.AnyAsync(ct))
        {
            var rules = new[]
            {
                new SpamRuleDto("rule_001", "number_prefix", "+98999", 35, true),
                new SpamRuleDto("rule_002", "message_keyword", "winner", 30, true),
                new SpamRuleDto("rule_003", "url_detected", null, 40, true),
                new SpamRuleDto("rule_004", "otp_regex", "\\b\\d{4,8}\\b", -40, true),
            };
            db.SpamRuleVersions.Add(new SpamRuleVersion
            {
                Id = Guid.NewGuid(),
                Version = 1,
                Threshold = 70,
                RulesJson = System.Text.Json.JsonSerializer.Serialize(rules),
                IsPublished = true,
                CreatedAt = DateTimeOffset.UtcNow,
                CreatedBy = "seed",
                Notes = "Initial global spam rules",
            });
        }

        if (!await db.AdminUsers.AnyAsync(ct))
        {
            db.AdminUsers.Add(new AdminUser
            {
                Id = Guid.NewGuid(),
                Username = "admin",
                PasswordHash = HashPassword("admin"),
                CreatedAt = DateTimeOffset.UtcNow,
            });
        }

        await db.SaveChangesAsync(ct);
    }

    public static string HashPassword(string password)
    {
        var salt = RandomNumberGenerator.GetBytes(16);
        var hash = Rfc2898DeriveBytes.Pbkdf2(Encoding.UTF8.GetBytes(password), salt, 100_000, HashAlgorithmName.SHA256, 32);
        return $"{Convert.ToBase64String(salt)}:{Convert.ToBase64String(hash)}";
    }

    public static bool VerifyPassword(string password, string stored)
    {
        var parts = stored.Split(':');
        if (parts.Length != 2) return false;
        var salt = Convert.FromBase64String(parts[0]);
        var expected = Convert.FromBase64String(parts[1]);
        var actual = Rfc2898DeriveBytes.Pbkdf2(Encoding.UTF8.GetBytes(password), salt, 100_000, HashAlgorithmName.SHA256, 32);
        return CryptographicOperations.FixedTimeEquals(expected, actual);
    }
}
