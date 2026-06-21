using Kheyr.Domain.Entities;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Storage.ValueConversion;
using ThreadEntity = Kheyr.Domain.Entities.Thread;
using DeleteBehavior = Microsoft.EntityFrameworkCore.DeleteBehavior;

namespace Kheyr.Infrastructure.Data;

public class KheyrDbContext(DbContextOptions<KheyrDbContext> options) : DbContext(options)
{
    public DbSet<User> Users => Set<User>();
    public DbSet<Device> Devices => Set<Device>();
    public DbSet<ThreadEntity> Threads => Set<ThreadEntity>();
    public DbSet<Message> Messages => Set<Message>();
    public DbSet<SpamRuleVersion> SpamRuleVersions => Set<SpamRuleVersion>();
    public DbSet<SpamFeedback> SpamFeedback => Set<SpamFeedback>();
    public DbSet<SyncCursor> SyncCursors => Set<SyncCursor>();
    public DbSet<SyncChange> SyncChanges => Set<SyncChange>();
    public DbSet<PairingSession> PairingSessions => Set<PairingSession>();
    public DbSet<DesktopSmsRequest> DesktopSmsRequests => Set<DesktopSmsRequest>();
    public DbSet<DirectMessage> DirectMessages => Set<DirectMessage>();
    public DbSet<RefreshToken> RefreshTokens => Set<RefreshToken>();
    public DbSet<OtpChallenge> OtpChallenges => Set<OtpChallenge>();
    public DbSet<AdminUser> AdminUsers => Set<AdminUser>();
    public DbSet<SystemMetricSnapshot> SystemMetricSnapshots => Set<SystemMetricSnapshot>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasIndex(x => x.PhoneNumberHash).IsUnique();
        });

        modelBuilder.Entity<Device>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasOne(x => x.User).WithMany(x => x.Devices).HasForeignKey(x => x.UserId);
            entity.HasIndex(x => new { x.UserId, x.RevokedAt });
        });

        modelBuilder.Entity<ThreadEntity>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasOne(x => x.User).WithMany(x => x.Threads).HasForeignKey(x => x.UserId);
            entity.HasIndex(x => new { x.UserId, x.ClientThreadId }).IsUnique();
        });

        modelBuilder.Entity<Message>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasOne(x => x.User).WithMany(x => x.Messages).HasForeignKey(x => x.UserId);
            entity.HasOne(x => x.Thread).WithMany(x => x.Messages).HasForeignKey(x => x.ThreadId).OnDelete(DeleteBehavior.NoAction);
            entity.HasIndex(x => new { x.UserId, x.ClientMessageId }).IsUnique();
        });

        modelBuilder.Entity<SpamRuleVersion>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasIndex(x => x.Version).IsUnique();
            entity.HasIndex(x => x.IsPublished);
        });

        modelBuilder.Entity<SyncCursor>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasOne(x => x.User).WithMany().HasForeignKey(x => x.UserId);
            entity.HasOne(x => x.Device).WithMany().HasForeignKey(x => x.DeviceId).OnDelete(DeleteBehavior.NoAction);
            entity.HasIndex(x => new { x.UserId, x.DeviceId }).IsUnique();
        });

        modelBuilder.Entity<SyncChange>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasIndex(x => new { x.UserId, x.Sequence }).IsUnique();
            entity.HasIndex(x => new { x.UserId, x.CreatedAt });
        });

        modelBuilder.Entity<PairingSession>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasIndex(x => x.Status);
        });

        modelBuilder.Entity<DesktopSmsRequest>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasIndex(x => new { x.UserId, x.Status });
        });

        modelBuilder.Entity<DirectMessage>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasIndex(x => x.ClientMessageId);
        });

        modelBuilder.Entity<RefreshToken>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasOne(x => x.User).WithMany().HasForeignKey(x => x.UserId);
            entity.HasOne(x => x.Device).WithMany().HasForeignKey(x => x.DeviceId).OnDelete(DeleteBehavior.NoAction);
            entity.HasIndex(x => x.TokenHash).IsUnique();
        });

        modelBuilder.Entity<OtpChallenge>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasIndex(x => x.PhoneE164);
        });

        modelBuilder.Entity<AdminUser>(entity =>
        {
            entity.HasKey(x => x.Id);
            entity.HasIndex(x => x.Username).IsUnique();
        });

        ConfigureDateTimeOffsetConverters(modelBuilder);
    }

    private static void ConfigureDateTimeOffsetConverters(ModelBuilder modelBuilder)
    {
        var converter = new ValueConverter<DateTimeOffset, long>(
            v => v.ToUnixTimeMilliseconds(),
            v => DateTimeOffset.FromUnixTimeMilliseconds(v));
        var nullableConverter = new ValueConverter<DateTimeOffset?, long?>(
            v => v.HasValue ? v.Value.ToUnixTimeMilliseconds() : null,
            v => v.HasValue ? DateTimeOffset.FromUnixTimeMilliseconds(v.Value) : null);

        foreach (var entityType in modelBuilder.Model.GetEntityTypes())
        {
            foreach (var property in entityType.GetProperties())
            {
                if (property.ClrType == typeof(DateTimeOffset))
                    property.SetValueConverter(converter);
                else if (property.ClrType == typeof(DateTimeOffset?))
                    property.SetValueConverter(nullableConverter);
            }
        }
    }
}
