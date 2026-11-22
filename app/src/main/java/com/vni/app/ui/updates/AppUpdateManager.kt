package com.vni.app.ui.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.vni.app.data.model.AppVersionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateManager {

    val defaultVersions = listOf(
        AppVersionItem(
            versionKey = "v1",
            versionName = "Vni V1",
            versionCode = 1,
            lastUpdatedDate = "22/07/26",
            sizeText = "4.4 MB",
            downloadUrl = "https://raw.githubusercontent.com/vni/releases/main/vni-v1.apk",
            hasUpload = true,
            isCurrent = false
        ),
        AppVersionItem(
            versionKey = "v2",
            versionName = "Vni V2",
            versionCode = 2,
            lastUpdatedDate = "17/11/26",
            sizeText = "4.4 MB",
            downloadUrl = "https://raw.githubusercontent.com/vni/releases/main/vni-v2.apk",
            hasUpload = true,
            isCurrent = false
        ),
        AppVersionItem(
            versionKey = "v3",
            versionName = "Vni V3",
            versionCode = 3,
            lastUpdatedDate = "17/11/26",
            sizeText = "4.4 MB",
            downloadUrl = "https://github.com/vishnuV-06/vni/releases/latest/download/vni-v3.apk",
            hasUpload = true,
            isCurrent = true
        )
    )

    /**
     * Seeds or refreshes the version catalog from Firebase Realtime Database,
     * ensuring real release dates (V2 = 17/11/26) and current version state.
     */
    fun syncWithFirebase(forceRefresh: Boolean = false, onVersionsLoaded: (List<AppVersionItem>) -> Unit) {
        try {
            val db = FirebaseDatabase.getInstance()
            val ref = db.getReference("app_updates")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists() || snapshot.childrenCount == 0L || forceRefresh) {
                        // Seed or update versions to Firebase
                        defaultVersions.forEach { item ->
                            ref.child(item.versionKey).setValue(
                                mapOf(
                                    "versionKey" to item.versionKey,
                                    "versionName" to item.versionName,
                                    "versionCode" to item.versionCode,
                                    "lastUpdatedDate" to item.lastUpdatedDate,
                                    "sizeText" to item.sizeText,
                                    "downloadUrl" to item.downloadUrl,
                                    "hasUpload" to item.hasUpload,
                                    "isCurrent" to item.isCurrent
                                )
                            )
                        }
                    }

                    if (snapshot.exists() && snapshot.childrenCount > 0L) {
                        val list = mutableListOf<AppVersionItem>()
                        for (child in snapshot.children) {
                            val vKey = child.child("versionKey").getValue(String::class.java) ?: child.key ?: "v1"
                            val vName = child.child("versionName").getValue(String::class.java) ?: "Vni ${vKey.uppercase()}"
                            val vCode = child.child("versionCode").getValue(Long::class.java)?.toInt() ?: 1
                            var lDate = child.child("lastUpdatedDate").getValue(String::class.java) ?: ""
                            // Ensure real release date for V2 is reflected
                            if (vKey == "v2" && (lDate.isBlank() || lDate == "18/08/26")) {
                                lDate = "17/11/26"
                                ref.child("v2").child("lastUpdatedDate").setValue("17/11/26")
                                ref.child("v2").child("isCurrent").setValue(true)
                            }
                            val size = child.child("sizeText").getValue(String::class.java) ?: "4.4 MB"
                            var url = child.child("downloadUrl").getValue(String::class.java) ?: ""
                            if (vKey == "v3") {
                                if (url.isBlank() || url.contains("raw.githubusercontent") || url.contains("example.com")) {
                                    url = "https://github.com/vishnuV-06/vni/releases/latest/download/vni-v3.apk"
                                    ref.child("v3").child("downloadUrl").setValue(url)
                                }
                            }
                            val hasUp = child.child("hasUpload").getValue(Boolean::class.java) ?: (lDate.isNotBlank())
                            val isCur = child.child("isCurrent").getValue(Boolean::class.java) ?: (vKey == "v2")

                            list.add(
                                AppVersionItem(
                                    versionKey = vKey,
                                    versionName = vName,
                                    versionCode = vCode,
                                    lastUpdatedDate = lDate,
                                    sizeText = size,
                                    downloadUrl = url,
                                    hasUpload = hasUp,
                                    isCurrent = isCur
                                )
                            )
                        }
                        if (list.isNotEmpty()) {
                            list.sortBy { it.versionCode }
                            onVersionsLoaded(list)
                        } else {
                            onVersionsLoaded(defaultVersions)
                        }
                    } else {
                        onVersionsLoaded(defaultVersions)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onVersionsLoaded(defaultVersions)
                }
            })
        } catch (e: Exception) {
            onVersionsLoaded(defaultVersions)
        }
    }

    /**
     * Publishes an app version release directly to Firebase Realtime Database
     * without needing a full APK rebuild every time (v1 -> v2 -> v3 cycle).
     */
    fun publishVersionToFirebase(
        versionKey: String,
        versionName: String,
        versionCode: Int,
        lastUpdatedDate: String,
        sizeText: String,
        downloadUrl: String = "",
        hasUpload: Boolean = true,
        onComplete: (Boolean) -> Unit = {}
    ) {
        try {
            val db = FirebaseDatabase.getInstance()
            val ref = db.getReference("app_updates")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Mark previously current versions as non-current
                    for (child in snapshot.children) {
                        child.ref.child("isCurrent").setValue(false)
                    }
                    val newRelease = mapOf(
                        "versionKey" to versionKey,
                        "versionName" to versionName,
                        "versionCode" to versionCode,
                        "lastUpdatedDate" to lastUpdatedDate,
                        "sizeText" to sizeText,
                        "downloadUrl" to downloadUrl,
                        "hasUpload" to hasUpload,
                        "isCurrent" to true
                    )
                    ref.child(versionKey).setValue(newRelease)
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete(false)
                }
            })
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    /**
     * Downloads the APK file from GitHub Releases or configured URL with cross-domain HTTP redirect handling.
     * Shows download progress and validates the downloaded APK file.
     */
    suspend fun downloadUpdate(
        context: Context,
        version: AppVersionItem,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val targetFile = File(destDir, "vni_update_${version.versionKey}.apk")

        val effectiveUrl = if (version.versionKey == "v3" &&
            (version.downloadUrl.isBlank() || version.downloadUrl.contains("raw.githubusercontent") || version.downloadUrl.contains("example.com"))
        ) {
            "https://github.com/vishnuV-06/vni/releases/latest/download/vni-v3.apk"
        } else {
            version.downloadUrl
        }

        if (effectiveUrl.isBlank() || !effectiveUrl.startsWith("http")) {
            throw java.io.IOException("Invalid download URL for ${version.versionName}")
        }

        var currentUrl = effectiveUrl
        var conn: HttpURLConnection? = null
        var redirectCount = 0
        val maxRedirects = 6

        while (redirectCount < maxRedirects) {
            val u = URL(currentUrl)
            conn = u.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; VNI-App)")
            conn.connect()

            val code = conn.responseCode
            if (code in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER, 307, 308)) {
                val newLocation = conn.getHeaderField("Location")
                conn.disconnect()
                if (!newLocation.isNullOrBlank()) {
                    currentUrl = if (newLocation.startsWith("http")) newLocation else URL(u, newLocation).toString()
                    redirectCount++
                    continue
                }
            }
            break
        }

        if (conn == null || conn.responseCode !in 200..299) {
            val errCode = conn?.responseCode ?: -1
            throw java.io.IOException("Unable to download update (HTTP $errCode). Please check your internet connection.")
        }

        val length = conn.contentLength.toFloat().coerceAtLeast(1f)
        var downloaded = 0
        val buffer = ByteArray(8192)

        try {
            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (length > 1f) {
                            onProgress((downloaded / length).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }

        // Verify the downloaded APK is valid and uncorrupted
        if (!isApkValid(context, targetFile)) {
            if (targetFile.exists()) targetFile.delete()
            throw java.io.IOException("Downloaded update package is corrupted or incomplete. Please tap download again.")
        }

        onProgress(1f)
        targetFile
    }

    /**
     * Verifies that the APK file is non-empty, parseable, and matches this application's package name.
     */
    fun isApkValid(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() < 100_000) return false
        return try {
            val pkgInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            pkgInfo != null && pkgInfo.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Prompts the Android OS package installer to install the APK file in-place without deleting user data.
     * If permission to install from unknown sources is needed, guides the user to the system settings.
     */
    fun promptInstallApk(context: Context, apkFile: File) {
        try {
            if (!isApkValid(context, apkFile)) {
                Toast.makeText(context, "APK package is invalid or corrupted. Please re-download.", Toast.LENGTH_LONG).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    Toast.makeText(context, "Please allow VNI to install updates, then return and tap Install & Restart", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Installation error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
