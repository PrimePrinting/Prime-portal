package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.data.model.GitHubRelease
import com.example.data.model.GitHubReleaseAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ApkVerificationInfo(
    val isValidApk: Boolean,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sha256Checksum: String,
    val fileSizeFormatted: String,
    val warningMessage: String? = null
)

class GitHubUpdateService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    /**
     * Retrieves the latest release from the specified GitHub repository.
     */
    suspend fun fetchLatestRelease(owner: String, repo: String): Result<GitHubRelease> =
        withContext(Dispatchers.IO) {
            try {
                val cleanOwner = owner.trim()
                val cleanRepo = repo.trim()
                val url = "https://api.github.com/repos/$cleanOwner/$cleanRepo/releases/latest"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("User-Agent", "CustomerPortal-Android-App")
                    .build()

                val response = client.newCall(request).execute()
                val responseCode = response.code
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    if (responseCode == 404) {
                        return@withContext Result.failure(
                            Exception("No published releases found for GitHub repository '$cleanOwner/$cleanRepo'. Please ensure releases are published with an attached .apk asset.")
                        )
                    }
                    return@withContext Result.failure(
                        Exception("GitHub API error ($responseCode): ${response.message}")
                    )
                }

                val json = JSONObject(responseBody)
                val tagName = json.optString("tag_name", "v1.0.0")
                val name = json.optString("name", "Release $tagName")
                val body = json.optString("body", "Official application release update.")
                val publishedAt = formatIsoDate(json.optString("published_at", ""))
                val htmlUrl = json.optString("html_url", "https://github.com/$cleanOwner/$cleanRepo")

                // Find APK asset from assets array
                var apkAsset: GitHubReleaseAsset? = null
                val assetsArray: JSONArray? = json.optJSONArray("assets")
                if (assetsArray != null) {
                    for (i in 0 until assetsArray.length()) {
                        val assetObj = assetsArray.getJSONObject(i)
                        val assetName = assetObj.optString("name", "")
                        val downloadUrl = assetObj.optString("browser_download_url", "")
                        val size = assetObj.optLong("size", 0L)
                        val contentType = assetObj.optString("content_type", "")
                        val downloadCount = assetObj.optInt("download_count", 0)

                        if (assetName.endsWith(".apk", ignoreCase = true) ||
                            contentType.contains("android.package-archive", ignoreCase = true)
                        ) {
                            apkAsset = GitHubReleaseAsset(
                                name = assetName,
                                browserDownloadUrl = downloadUrl,
                                sizeBytes = size,
                                contentType = contentType,
                                downloadCount = downloadCount
                            )
                            break
                        }
                    }
                }

                val release = GitHubRelease(
                    tagName = tagName,
                    name = name,
                    body = body,
                    publishedAt = publishedAt,
                    htmlUrl = htmlUrl,
                    apkAsset = apkAsset
                )

                Result.success(release)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Downloads the APK file from GitHub release asset URL while streaming live progress.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long, speedText: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            destinationFile.parentFile?.mkdirs()
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "CustomerPortal-Android-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to download APK from GitHub (HTTP ${response.code}): ${response.message}")
                )
            }

            val body = response.body
                ?: return@withContext Result.failure(Exception("Empty response body from APK download stream"))

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            val buffer = ByteArray(8 * 1024)

            var lastTime = System.currentTimeMillis()
            var bytesSinceLastTime = 0L
            var currentSpeedText = "Calculating..."

            body.byteStream().use { input: InputStream ->
                FileOutputStream(destinationFile).use { output: FileOutputStream ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        bytesSinceLastTime += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            val speedBytesPerSec = (bytesSinceLastTime * 1000.0) / elapsed
                            val speedMbPerSec = speedBytesPerSec / (1024.0 * 1024.0)
                            currentSpeedText = String.format(Locale.US, "%.1f MB/s", speedMbPerSec)
                            lastTime = now
                            bytesSinceLastTime = 0L

                            onProgress(downloadedBytes, totalBytes, currentSpeedText)
                        }
                    }
                    output.flush()
                }
            }

            onProgress(downloadedBytes, totalBytes, "Download Complete")
            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifies that the downloaded APK is a valid Android package archive and checks integrity.
     */
    fun verifyDownloadedApk(context: Context, apkFile: File): Result<ApkVerificationInfo> {
        return try {
            if (!apkFile.exists() || apkFile.length() <= 0) {
                return Result.failure(Exception("Downloaded APK file is empty or does not exist"))
            }

            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
                ?: return Result.failure(Exception("Invalid APK structure: file could not be parsed as an Android package archive"))

            val packageName = packageInfo.packageName ?: ""
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            // Calculate SHA-256 Checksum
            val sha256 = calculateSha256(apkFile)

            val sizeMb = apkFile.length() / (1024.0 * 1024.0)
            val sizeFormatted = String.format(Locale.US, "%.1f MB", sizeMb)

            var warningMessage: String? = null
            if (packageName.isNotBlank() && packageName != context.packageName) {
                warningMessage = "Notice: Package ID '$packageName' differs from installed app '${context.packageName}'."
            }

            Result.success(
                ApkVerificationInfo(
                    isValidApk = true,
                    packageName = packageName,
                    versionName = versionName,
                    versionCode = versionCode,
                    sha256Checksum = sha256,
                    fileSizeFormatted = sizeFormatted,
                    warningMessage = warningMessage
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launches the Android Package Installer using a FileProvider content URI.
     */
    fun launchApkInstaller(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(Exception("APK file not found on device storage"))
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Could not launch package installer: ${e.message}"))
        }
    }

    /**
     * Checks if the app has permission to request package installs on Android 8.0+ (Oreo).
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens system settings to allow installing unknown apps from this source.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // Fallback to application details settings
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    fun getDownloadDestinationFile(context: Context, fileName: String): File {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads")
        downloadDir.mkdirs()
        val safeName = if (fileName.endsWith(".apk", ignoreCase = true)) fileName else "$fileName.apk"
        return File(downloadDir, safeName)
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val bytes = digest.digest()
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun formatIsoDate(isoString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val date = parser.parse(isoString)
            if (date != null) formatter.format(date) else isoString
        } catch (_: Exception) {
            isoString
        }
    }
}
