package com.fpf.smartscan.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import com.fpf.smartscan.IImageEmbedderService
import com.fpf.smartscansdk.core.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.unflattenEmbeddings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ImageEmbedderClient(
    private val context: Context
): ImageEmbeddingProvider{
    companion object {
        private const val APP_PACKAGE_NAME = "com.fpf.smartscan"
        private const val INTENT_ACTION_NAME = "com.fpf.smartscan.intent.IMAGE_EMBED"
        const val TAG = "ImageEmbedderClient"
    }

    private var mImageEmbedderService: IImageEmbedderService? = null
    private var isBound = false
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val mIndexServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "Service has connected successfully")
            mImageEmbedderService = IImageEmbedderService.Stub.asInterface(service)
            _isConnected.value = true
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Log.e(TAG, "Service has unexpectedly disconnected")
            reset()
        }
    }

    override val embeddingDim: Int
        get() = mImageEmbedderService?.embeddingDim?: throw EmbedderClientException.connectionError()


    fun connectService() {
        if (isBound) return
        val intent = Intent(INTENT_ACTION_NAME).apply { setPackage(APP_PACKAGE_NAME) }
        isBound = context.applicationContext.bindService(intent, mIndexServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override suspend fun embed(data: Bitmap): FloatArray = withContext(Dispatchers.IO) {
        if (!_isConnected.value) throw EmbedderClientException.connectionError()
        val byteArray = bitmapToByteArray(data)
        mImageEmbedderService?.embed(byteArray) ?: throw EmbedderClientException.embeddingError()
    }

    override suspend fun embedBatch( data: List<Bitmap>): List<FloatArray> = withContext(Dispatchers.IO) {
        if (!_isConnected.value) throw EmbedderClientException.connectionError()
        val encoded = encodeByteArrayPayload(data.map{bitmapToByteArray(it)}, mImageEmbedderService!!.delimiter)
        val embeddings = mImageEmbedderService?.embedBatch(encoded)
            ?: throw EmbedderClientException.embeddingError()
        unflattenEmbeddings(embeddings, mImageEmbedderService!!.embeddingDim)
    }

    override suspend fun initialize() {
        // service handles internally
    }

    override fun isInitialized(): Boolean {
        // service handles actual initialisation internally
        return mImageEmbedderService != null
    }


    fun listModels(): List<String> {
        if (!_isConnected.value) throw EmbedderClientException.connectionError()
        return mImageEmbedderService!!.listModels()
    }

    fun selectModel(model: String): Boolean{
        if (!_isConnected.value) throw EmbedderClientException.connectionError()
        return mImageEmbedderService!!.selectModel(model)
    }


    fun disconnectService(){
        if (isBound){
            context.applicationContext.unbindService(mIndexServiceConnection)
        }
        reset()
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray{
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun reset(){
        mImageEmbedderService = null
        isBound = false
        _isConnected.value = false
    }

    private fun encodeByteArrayPayload(payload: List<ByteArray>, delimiter: ByteArray): ByteArray{
        val totalSize = payload.sumOf { it.size } + (delimiter.size * (payload.size - 1))
        val merged = ByteArray(totalSize)
        var pos = 0

        payload.forEachIndexed {
                index, bytes ->
            System.arraycopy(bytes, 0, merged, pos, bytes.size)
            pos += bytes.size

            if(index < payload.size - 1){
                System.arraycopy(delimiter, 0, merged, pos, delimiter.size)
                pos += delimiter.size
            }
        }

        return merged
    }

}