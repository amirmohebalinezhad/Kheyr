using System.Security.Claims;
using Kheyr.Domain.Entities;
using Kheyr.Infrastructure;
using Kheyr.Infrastructure.Data;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.EntityFrameworkCore;

namespace Kheyr.Admin.Services;

public class AdminAuthService(KheyrDbContext db)
{
    public async Task<AdminUser?> ValidateAsync(string username, string password, CancellationToken ct)
    {
        var user = await db.AdminUsers.FirstOrDefaultAsync(x => x.Username == username, ct);
        if (user is null) return null;
        return SeedData.VerifyPassword(password, user.PasswordHash) ? user : null;
    }

    public static ClaimsPrincipal CreatePrincipal(AdminUser user)
    {
        var identity = new ClaimsIdentity(CookieAuthenticationDefaults.AuthenticationScheme);
        identity.AddClaim(new Claim(ClaimTypes.Name, user.Username));
        identity.AddClaim(new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()));
        identity.AddClaim(new Claim(ClaimTypes.Role, "Admin"));
        return new ClaimsPrincipal(identity);
    }
}
