package com.example.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** A GitHub release that carries an APK for this build type. */
data class Release(val tag: String, val version: String, val apkUrl: String, val apkSize: Long, val pageUrl: String, val notes: String)

/** What the Settings screen shows. One state at a time, driven by [Updater]. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data object Installing : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * In-app updater fed by GitHub Releases of [BuildConfig.UPDATE_REPO].
 * Release builds install the `*-release.apk` asset, debug builds the `*-debug.apk` asset.
 * Android itself refuses an update signed with a different key, so a tampered APK cannot install.
 */
object Updater {
    private const val PREFS = "updater"
    private const val KEY_AUTO = "auto_check"
    private const val KEY_LAST = "last_check"
    private const val AUTO_INTERVAL_MS = 12 * 60 * 60 * 1000L

    // App-wide scope: a check or download keeps going when the user leaves the Settings screen.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun checkNow(context: Context) { val app = context.applicationContext; scope.launch { check(app) } }
    fun update(context: Context, release: Release) { val app = context.applicationContext; scope.launch { downloadAndInstall(app, release) } }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    val currentVersion: String get() = BuildConfig.VERSION_NAME
    val repoUrl: String get() = "https://github.com/${BuildConfig.UPDATE_REPO}"

    fun isAutoCheck(context: Context) = prefs(context).getBoolean(KEY_AUTO, true)
    fun setAutoCheck(context: Context, on: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO, on).apply()

    /** Called on app start: checks at most every 12 hours when auto-check is on. */
    suspend fun autoCheck(context: Context) {
        val p = prefs(context)
        if (!p.getBoolean(KEY_AUTO, true)) return
        if (System.currentTimeMillis() - p.getLong(KEY_LAST, 0) < AUTO_INTERVAL_MS) return
        check(context)
    }

    suspend fun check(context: Context) {
        _state.value = UpdateState.Checking
        _state.value = try {
            val release = fetchLatest()
            prefs(context).edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
            when {
                release == null -> UpdateState.Failed("No ${BuildConfig.BUILD_TYPE} APK in the latest release")
                isNewer(release.version, currentVersion) -> UpdateState.Available(release)
                else -> UpdateState.UpToDate
            }
        } catch (e: Exception) {
            UpdateState.Failed("Couldn't reach GitHub: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** Downloads the APK and hands it to the system installer (which asks the user to confirm). */
    suspend fun downloadAndInstall(context: Context, release: Release) {
        if (!canInstall(context)) {
            openInstallPermissionSettings(context)
            _state.value = UpdateState.Failed("Allow \"Install unknown apps\" for this app, then tap Update again")
            return
        }
        try {
            val apk = download(context, release)
            _state.value = UpdateState.Installing
            install(context, apk)
        } catch (e: Exception) {
            _state.value = UpdateState.Failed("Update failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    internal fun onInstallResult(status: Int, message: String?) {
        _state.value = when (status) {
            PackageInstaller.STATUS_SUCCESS -> UpdateState.UpToDate
            PackageInstaller.STATUS_FAILURE_ABORTED -> UpdateState.Idle
            else -> UpdateState.Failed("Install failed: ${message ?: "status $status"}")
        }
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** "v1.2.3" / "1.2.3" / "1.2" compared numerically; a non-numeric part counts as 0. */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(candidate); val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        val conn = URL("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest").openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.connectTimeout = 15_000; conn.readTimeout = 15_000
        try {
            if (conn.responseCode == 404) return@withContext null
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            val suffix = "-${BuildConfig.BUILD_TYPE}.apk"
            val assets = json.getJSONArray("assets")
            val allowed = "https://github.com/${BuildConfig.UPDATE_REPO}/releases/download/"
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val url = a.getString("browser_download_url")
                if (a.getString("name").endsWith(suffix) && url.startsWith(allowed)) {
                    return@withContext Release(tag, tag.removePrefix("v"), url, a.optLong("size"),
                        json.optString("html_url", "https://github.com/${BuildConfig.UPDATE_REPO}/releases"), json.optString("body"))
                }
            }
            null
        } finally { conn.disconnect() }
    }

    private suspend fun download(context: Context, release: Release): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val out = File(dir, "update.apk")
        var url = URL(release.apkUrl)
        var conn: HttpURLConnection
        var hops = 0
        while (true) { // follow GitHub's redirect to its download CDN (https only)
            conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15_000; conn.readTimeout = 30_000
            val code = conn.responseCode
            if (code in 300..399 && hops++ < 5) {
                url = URL(url, conn.getHeaderField("Location")); conn.disconnect()
                require(url.protocol == "https") { "insecure redirect" }
                continue
            }
            if (code != 200) error("HTTP $code")
            break
        }
        val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
        conn.inputStream.use { input ->
            out.outputStream().use { output ->
                val buf = ByteArray(64 * 1024); var done = 0L
                while (true) {
                    val n = input.read(buf); if (n < 0) break
                    output.write(buf, 0, n); done += n
                    if (total > 0) _state.value = UpdateState.Downloading(release, done.toFloat() / total)
                }
            }
        }
        conn.disconnect()
        if (release.apkSize > 0 && out.length() != release.apkSize) error("download incomplete")
        out
    }

    private suspend fun install(context: Context, apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("update.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val callback = PendingIntent.getBroadcast(context, id, Intent(context, UpdateReceiver::class.java), flags)
            session.commit(callback.intentSender)
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
