using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Kheyr.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class InitialCreate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "AdminUsers",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    Username = table.Column<string>(type: "nvarchar(450)", nullable: false),
                    PasswordHash = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_AdminUsers", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "DesktopSmsRequests",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    DesktopDeviceId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    AndroidDeviceId = table.Column<Guid>(type: "uniqueidentifier", nullable: true),
                    EncryptedMessageBody = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    EncryptedTargetNumber = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    SimId = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    ThreadClientId = table.Column<long>(type: "bigint", nullable: true),
                    ClientMessageId = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    Status = table.Column<int>(type: "int", nullable: false),
                    FailureReason = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false),
                    ExpiresAt = table.Column<long>(type: "bigint", nullable: false),
                    CompletedAt = table.Column<long>(type: "bigint", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_DesktopSmsRequests", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "DirectMessages",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    SenderUserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    RecipientPhoneHash = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    RecipientUserId = table.Column<Guid>(type: "uniqueidentifier", nullable: true),
                    MessageType = table.Column<int>(type: "int", nullable: false),
                    Body = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    AttachmentId = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    ClientMessageId = table.Column<string>(type: "nvarchar(450)", nullable: false),
                    Status = table.Column<int>(type: "int", nullable: false),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false),
                    DeliveredAt = table.Column<long>(type: "bigint", nullable: true),
                    ReadAt = table.Column<long>(type: "bigint", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_DirectMessages", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "OtpChallenges",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    PhoneE164 = table.Column<string>(type: "nvarchar(450)", nullable: false),
                    CodeHash = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    ExpiresAt = table.Column<long>(type: "bigint", nullable: false),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false),
                    AttemptCount = table.Column<int>(type: "int", nullable: false),
                    Consumed = table.Column<bool>(type: "bit", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_OtpChallenges", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "PairingSessions",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: true),
                    DesktopDeviceId = table.Column<Guid>(type: "uniqueidentifier", nullable: true),
                    QrPayload = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    Status = table.Column<int>(type: "int", nullable: false),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false),
                    ExpiresAt = table.Column<long>(type: "bigint", nullable: false),
                    ApprovedAt = table.Column<long>(type: "bigint", nullable: true),
                    DesktopDeviceName = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    DesktopPlatform = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    DesktopPublicKey = table.Column<string>(type: "nvarchar(max)", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_PairingSessions", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "SpamFeedback",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: true),
                    SenderHash = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    MessageHash = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    RuleVersion = table.Column<int>(type: "int", nullable: false),
                    SpamScore = table.Column<int>(type: "int", nullable: false),
                    UserAction = table.Column<int>(type: "int", nullable: false),
                    TriggeredRuleIdsJson = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SpamFeedback", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "SpamRuleVersions",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    Version = table.Column<int>(type: "int", nullable: false),
                    Threshold = table.Column<int>(type: "int", nullable: false),
                    RulesJson = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    IsPublished = table.Column<bool>(type: "bit", nullable: false),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false),
                    CreatedBy = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    Notes = table.Column<string>(type: "nvarchar(max)", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SpamRuleVersions", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "SystemMetricSnapshots",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    MetricName = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    Value = table.Column<double>(type: "float", nullable: false),
                    RecordedAt = table.Column<long>(type: "bigint", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SystemMetricSnapshots", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "Users",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    PhoneNumberHash = table.Column<string>(type: "nvarchar(450)", nullable: false),
                    EncryptedPhoneNumber = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    SyncEnabled = table.Column<bool>(type: "bit", nullable: false),
                    DirectMessageEnabled = table.Column<bool>(type: "bit", nullable: false),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false),
                    LastActiveAt = table.Column<long>(type: "bigint", nullable: false),
                    DeletedAt = table.Column<long>(type: "bigint", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Users", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "Devices",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    DeviceName = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    DeviceType = table.Column<int>(type: "int", nullable: false),
                    Platform = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    PublicKey = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    PushToken = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false),
                    LastActiveAt = table.Column<long>(type: "bigint", nullable: false),
                    RevokedAt = table.Column<long>(type: "bigint", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Devices", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Devices_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "SyncChanges",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    Sequence = table.Column<long>(type: "bigint", nullable: false),
                    ChangeType = table.Column<int>(type: "int", nullable: false),
                    PayloadJson = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SyncChanges", x => x.Id);
                    table.ForeignKey(
                        name: "FK_SyncChanges_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "Threads",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    ClientThreadId = table.Column<long>(type: "bigint", nullable: false),
                    EncryptedPhoneNumber = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    PhoneNumberHash = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    EncryptedContactName = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    LastMessageAt = table.Column<long>(type: "bigint", nullable: true),
                    UnreadCount = table.Column<int>(type: "int", nullable: false),
                    IsSpam = table.Column<bool>(type: "bit", nullable: false),
                    IsArchived = table.Column<bool>(type: "bit", nullable: false),
                    IsPinned = table.Column<bool>(type: "bit", nullable: false),
                    PinnedAt = table.Column<long>(type: "bigint", nullable: true),
                    IsMuted = table.Column<bool>(type: "bit", nullable: false),
                    NotificationMode = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    CustomRingtone = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    DefaultSimId = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false),
                    UpdatedAt = table.Column<long>(type: "bigint", nullable: false),
                    DeletedAt = table.Column<long>(type: "bigint", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Threads", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Threads_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "RefreshTokens",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    DeviceId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    TokenHash = table.Column<string>(type: "nvarchar(450)", nullable: false),
                    ExpiresAt = table.Column<long>(type: "bigint", nullable: false),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false),
                    RevokedAt = table.Column<long>(type: "bigint", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_RefreshTokens", x => x.Id);
                    table.ForeignKey(
                        name: "FK_RefreshTokens_Devices_DeviceId",
                        column: x => x.DeviceId,
                        principalTable: "Devices",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_RefreshTokens_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "SyncCursors",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    DeviceId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    Cursor = table.Column<long>(type: "bigint", nullable: false),
                    LastSyncedAt = table.Column<long>(type: "bigint", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SyncCursors", x => x.Id);
                    table.ForeignKey(
                        name: "FK_SyncCursors_Devices_DeviceId",
                        column: x => x.DeviceId,
                        principalTable: "Devices",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_SyncCursors_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "Messages",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    ThreadId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    ClientMessageId = table.Column<long>(type: "bigint", nullable: false),
                    EncryptedBody = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    EncryptedSender = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    EncryptedReceiver = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    Direction = table.Column<int>(type: "int", nullable: false),
                    MessageType = table.Column<int>(type: "int", nullable: false),
                    Status = table.Column<int>(type: "int", nullable: false),
                    Timestamp = table.Column<long>(type: "bigint", nullable: false),
                    SimId = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    SpamScore = table.Column<int>(type: "int", nullable: true),
                    SpamReasonsJson = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    IsSpam = table.Column<bool>(type: "bit", nullable: false),
                    IsDeleted = table.Column<bool>(type: "bit", nullable: false),
                    CreatedAt = table.Column<long>(type: "bigint", nullable: false),
                    UpdatedAt = table.Column<long>(type: "bigint", nullable: false),
                    DeletedAt = table.Column<long>(type: "bigint", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Messages", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Messages_Threads_ThreadId",
                        column: x => x.ThreadId,
                        principalTable: "Threads",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_Messages_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_AdminUsers_Username",
                table: "AdminUsers",
                column: "Username",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_DesktopSmsRequests_UserId_Status",
                table: "DesktopSmsRequests",
                columns: new[] { "UserId", "Status" });

            migrationBuilder.CreateIndex(
                name: "IX_Devices_UserId_RevokedAt",
                table: "Devices",
                columns: new[] { "UserId", "RevokedAt" });

            migrationBuilder.CreateIndex(
                name: "IX_DirectMessages_ClientMessageId",
                table: "DirectMessages",
                column: "ClientMessageId");

            migrationBuilder.CreateIndex(
                name: "IX_Messages_ThreadId",
                table: "Messages",
                column: "ThreadId");

            migrationBuilder.CreateIndex(
                name: "IX_Messages_UserId_ClientMessageId",
                table: "Messages",
                columns: new[] { "UserId", "ClientMessageId" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_OtpChallenges_PhoneE164",
                table: "OtpChallenges",
                column: "PhoneE164");

            migrationBuilder.CreateIndex(
                name: "IX_PairingSessions_Status",
                table: "PairingSessions",
                column: "Status");

            migrationBuilder.CreateIndex(
                name: "IX_RefreshTokens_DeviceId",
                table: "RefreshTokens",
                column: "DeviceId");

            migrationBuilder.CreateIndex(
                name: "IX_RefreshTokens_TokenHash",
                table: "RefreshTokens",
                column: "TokenHash",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_RefreshTokens_UserId",
                table: "RefreshTokens",
                column: "UserId");

            migrationBuilder.CreateIndex(
                name: "IX_SpamRuleVersions_IsPublished",
                table: "SpamRuleVersions",
                column: "IsPublished");

            migrationBuilder.CreateIndex(
                name: "IX_SpamRuleVersions_Version",
                table: "SpamRuleVersions",
                column: "Version",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_SyncChanges_UserId_CreatedAt",
                table: "SyncChanges",
                columns: new[] { "UserId", "CreatedAt" });

            migrationBuilder.CreateIndex(
                name: "IX_SyncChanges_UserId_Sequence",
                table: "SyncChanges",
                columns: new[] { "UserId", "Sequence" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_SyncCursors_DeviceId",
                table: "SyncCursors",
                column: "DeviceId");

            migrationBuilder.CreateIndex(
                name: "IX_SyncCursors_UserId_DeviceId",
                table: "SyncCursors",
                columns: new[] { "UserId", "DeviceId" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_Threads_UserId_ClientThreadId",
                table: "Threads",
                columns: new[] { "UserId", "ClientThreadId" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_Users_PhoneNumberHash",
                table: "Users",
                column: "PhoneNumberHash",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "AdminUsers");

            migrationBuilder.DropTable(
                name: "DesktopSmsRequests");

            migrationBuilder.DropTable(
                name: "DirectMessages");

            migrationBuilder.DropTable(
                name: "Messages");

            migrationBuilder.DropTable(
                name: "OtpChallenges");

            migrationBuilder.DropTable(
                name: "PairingSessions");

            migrationBuilder.DropTable(
                name: "RefreshTokens");

            migrationBuilder.DropTable(
                name: "SpamFeedback");

            migrationBuilder.DropTable(
                name: "SpamRuleVersions");

            migrationBuilder.DropTable(
                name: "SyncChanges");

            migrationBuilder.DropTable(
                name: "SyncCursors");

            migrationBuilder.DropTable(
                name: "SystemMetricSnapshots");

            migrationBuilder.DropTable(
                name: "Threads");

            migrationBuilder.DropTable(
                name: "Devices");

            migrationBuilder.DropTable(
                name: "Users");
        }
    }
}
