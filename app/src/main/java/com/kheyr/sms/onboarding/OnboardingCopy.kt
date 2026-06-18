package com.kheyr.sms.onboarding

object OnboardingCopy {
    val steps = listOf(
        OnboardingStepInfo(
            title = "Welcome",
            headline = WelcomeScreenCopy.title,
            body = WelcomeScreenCopy.subtitle,
            detail = WelcomeScreenCopy.defaultSmsRequirement,
        ),
        OnboardingStepInfo(
            title = "Default SMS",
            headline = "Set Kheyr as your default SMS app",
            body = "Android only lets one app handle incoming and outgoing text messages.",
            detail = "This is required so Kheyr can replace your stock SMS app.",
        ),
        OnboardingStepInfo(
            title = "Permissions",
            headline = "Allow the essentials",
            body = "Grant SMS, contacts, and notification access so conversations stay organized and you never miss a message.",
            detail = "You can change these later in system settings.",
        ),
        OnboardingStepInfo(
            title = "Sync account",
            headline = "Optional encrypted sync",
            body = "Create an account to sync messages across devices and use desktop relay.",
            detail = "Sync is optional. Message bodies are encrypted on your phone before upload — the server never sees plaintext.",
        ),
        OnboardingStepInfo(
            title = "Import & rules",
            headline = "Almost ready",
            body = "Kheyr will import your existing SMS threads and download spam-filtering rules in the background.",
            detail = "You can start messaging as soon as you open your inbox.",
        ),
    )
}

data class OnboardingStepInfo(
    val title: String,
    val headline: String,
    val body: String,
    val detail: String,
)
