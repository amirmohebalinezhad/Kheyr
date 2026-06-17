using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;

namespace Kheyr.Infrastructure.Hubs;

[Authorize]
public class KheyrHub : Hub
{
    public override async Task OnConnectedAsync()
    {
        var userId = Context.User?.FindFirstValue(ClaimTypes.NameIdentifier);
        var deviceId = Context.User?.FindFirstValue("device_id");
        if (userId is not null)
            await Groups.AddToGroupAsync(Context.ConnectionId, $"user:{userId}");
        if (deviceId is not null)
            await Groups.AddToGroupAsync(Context.ConnectionId, $"device:{deviceId}");
        await base.OnConnectedAsync();
    }

    public Task JoinPairingSession(string sessionId)
        => Groups.AddToGroupAsync(Context.ConnectionId, $"pairing:{sessionId}");
}
