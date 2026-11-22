package com.vni.app.data

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File

/**
 * Manages persistent local data backup outside the app's sandboxed storage.
 * When the app is uninstalled, files stored in Documents/VniBackup/ are preserved by Android.
 * When the app is re-installed, this manager automatically detects and restores all user data.
 */
object PersistentStorageManager {

    private const val BACKUP_DIR_NAME = "VniBackup"
    private const val BACKUP_FILE_NAME = "vni_data_backup.json"
    private const val PREFS_MAIN = "vni_app_prefs_v2"
    private const val PREFS_SECURITY = "vni_security_prefs"

    private fun getBackupFiles(): List<File> {
        val files = mutableListOf<File>()
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val vniDir = File(docsDir, BACKUP_DIR_NAME)
            if (!vniDir.exists()) vniDir.mkdirs()
            files.add(File(vniDir, BACKUP_FILE_NAME))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val vniDir = File(downloadsDir, BACKUP_DIR_NAME)
            if (!vniDir.exists()) vniDir.mkdirs()
            files.add(File(vniDir, BACKUP_FILE_NAME))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return files
    }

    /**
     * Backs up all current SharedPreferences data to external Documents directory.
     */
    fun backupData(context: Context) {
        try {
            val root = JSONObject()
            val mainPrefs = context.getSharedPreferences(PREFS_MAIN, Context.MODE_PRIVATE)
            val mainObj = JSONObject()
            for ((key, value) in mainPrefs.all) {
                mainObj.put(key, value)
            }
            root.put("main_prefs", mainObj)

            val secPrefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
            val secObj = JSONObject()
            for ((key, value) in secPrefs.all) {
                secObj.put(key, value)
            }
            root.put("security_prefs", secObj)

            root.put("backup_timestamp", System.currentTimeMillis())

            val jsonStr = root.toString(2)
            for (file in getBackupFiles()) {
                try {
                    file.parentFile?.mkdirs()
                    file.writeText(jsonStr)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if a persistent backup file exists on the device and restores it into SharedPreferences.
     * Returns true if data was restored.
     */
    fun restoreDataIfNeeded(context: Context): Boolean {
        try {
            for (file in getBackupFiles()) {
                if (file.exists() && file.length() > 0) {
                    val content = file.readText()
                    if (content.isBlank()) continue

                    val root = JSONObject(content)
                    if (root.has("main_prefs")) {
                        val mainObj = root.getJSONObject("main_prefs")
                        val mainPrefs = context.getSharedPreferences(PREFS_MAIN, Context.MODE_PRIVATE).edit()
                        val keys = mainObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            when (val v = mainObj.get(key)) {
                                is String -> mainPrefs.putString(key, v)
                                is Boolean -> mainPrefs.putBoolean(key, v)
                                is Int -> mainPrefs.putInt(key, v)
                                is Long -> mainPrefs.putLong(key, v)
                                is Float -> mainPrefs.putFloat(key, v)
                                is Double -> mainPrefs.putFloat(key, v.toFloat())
                                else -> mainPrefs.putString(key, v.toString())
                            }
                        }
                        mainPrefs.apply()
                    }

                    if (root.has("security_prefs")) {
                        val secObj = root.getJSONObject("security_prefs")
                        val secPrefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE).edit()
                        val keys = secObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            when (val v = secObj.get(key)) {
                                is String -> secPrefs.putString(key, v)
                                is Boolean -> secPrefs.putBoolean(key, v)
                                is Int -> secPrefs.putInt(key, v)
                                is Long -> secPrefs.putLong(key, v)
                                else -> secPrefs.putString(key, v.toString())
                            }
                        }
                        secPrefs.apply()
                    }

                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Clears any legacy dummy backup files so clean install starts completely empty.
     */
    fun deleteOldDummyBackup(context: Context) {
        try {
            for (file in getBackupFiles()) {
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
