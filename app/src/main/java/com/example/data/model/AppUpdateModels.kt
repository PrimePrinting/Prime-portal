package com.example.data.model

import java.io.File

data class GitHubReleaseAsset(
    val name: String,
    val browserDownloadUrl: String,
    val sizeBytes: Long,
    val contentType: String,
    val downloadCount: Int
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "Unknown size"
            val mb = sizeBytes / (1024.0 * 1024.0)
            return String.format("%.1f MB", mb)
        }
}

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val apkAsset: GitHubReleaseAsset?
) {
    val cleanVersionName: String
        get() = tagName.removePrefix("v").trim()
}

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    object Checking : DownloadStatus()
    object Connecting : DownloadStatus()

    data class Downloading(
        val progress: Float, // 0.0 to 1.0
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedText: String
    ) : DownloadStatus() {
        val percentageInt: Int get() = (progress * 100).toInt().coerceIn(0, 100)
        val downloadedFormatted: String
            get() = String.format("%.1f MB", downloadedBytes / (1024.0 * 1024.0))
        val totalFormatted: String
            get() = if (totalBytes > 0) String.format("%.1f MB", totalBytes / (1024.0 * 1024.0)) else "Unknown"
    }

    object Verifying : DownloadStatus()

    data class ReadyToInstall(
        val apkFile: File,
        val packageName: String,
        val versionName: String,
        val sha256Checksum: String,
        val fileSizeFormatted: String
    ) : DownloadStatus()

    object Completed : DownloadStatus()

    data class Error(
        val message: String,
        val isRetryable: Boolean = true
    ) : DownloadStatus()
}

data class UpdateCheckResult(
    val installedVersionName: String,
    val installedVersionCode: Int,
    val latestVersionName: String?,
    val isUpdateAvailable: Boolean,
    val release: GitHubRelease?,
    val checkTimeFormatted: String,
    val errorMessage: String? = null
)
