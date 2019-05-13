package dev.sargunv.modsman

import com.google.gson.annotations.SerializedName
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface CurseforgeService {
    @POST("addon")
    fun getAddonsAsync(@Body addonIds: List<Int>): Deferred<List<CurseforgeAddon>>

    @POST("addon/files")
    fun getFilesAsync(@Body fileRequests: List<CurseforgeFileRequest>): Deferred<Map<String, List<CurseforgeFile>>>

    @GET("addon/{id}/files")
    fun getAddonFilesAsync(@Path("id") addonId: Int): Deferred<List<CurseforgeFile>>

    @POST("fingerprint")
    fun fingerprintAsync(@Body hashes: List<Long>): Deferred<CurseforgeFingerprintResults>

    companion object {
        fun createClient(): CurseforgeService = Retrofit.Builder()
            .baseUrl("https://addons-ecs.forgesvc.net/api/")
            .addConverterFactory(
                GsonConverterFactory.create())
            .addCallAdapterFactory(
                CoroutineCallAdapterFactory())
            .build()
            .create(CurseforgeService::class.java)
    }
}

internal data class CurseforgeAddon(
    @SerializedName("id")
    val addonId: Int,
    val name: String
)

internal data class CurseforgeFile(
    @SerializedName("id")
    val fileId: Int,
    val name: Int,
    val fileNameOnDisk: String,
    val downloadUrl: String,
    val fileDate: String,
    val releaseType: Int,
    val isAlternate: Boolean,
    val isAvailable: Boolean,
    val gameVersion: List<String>,
    val packageFingerprint: Long
)

internal data class CurseforgeFileRequest(
    val addonId: Int,
    val fileId: Int
)

internal data class CurseforgeFingerprintResults(
    val exactMatches: List<Result>,
    val exactFingerprints: List<Long>
) {
    data class Result(
        @SerializedName("id")
        val projectId: Int,
        val file: CurseforgeFile,
        val latestFiles: List<CurseforgeFile>
    )
}
