package com.fpf.smartscan.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import com.fpf.smartscan.IImageEmbedderService
import com.fpf.smartscansdk.core.embeddings.unflattenEmbeddings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ImageEmbedderClient(
    private val context: Context
){
    companion object {
        private const val APP_PACKAGE_NAME = "com.fpf.smartscan"
        private const val INTENT_ACTION_NAME = "com.fpf.smartscan.intent.IMAGE_EMBED"
        const val TAG = "ImageEmbedderClient"
    }

    private var mTextEmbedderService: IImageEmbedderService? = null
    private var isBound = false
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val mIndexServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "Service has connected successfully")
            mTextEmbedderService = IImageEmbedderService.Stub.asInterface(service)
            _isConnected.value = true
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Log.e(TAG, "Service has unexpectedly disconnected")
            reset()
        }
    }

    fun connectService() {
        if (isBound) return
        val intent = Intent(INTENT_ACTION_NAME).apply { setPackage(APP_PACKAGE_NAME) }
        isBound = context.applicationContext.bindService(intent, mIndexServiceConnection, Context.BIND_AUTO_CREATE)
    }

    suspend fun embed(data: ByteArray): FloatArray = withContext(Dispatchers.IO) {
        if (!_isConnected.value) throw EmbedderClientException.connectionError()
        mTextEmbedderService?.embed(data) ?: throw EmbedderClientException.embeddingError()
    }

    suspend fun embed(data: Bitmap): FloatArray = withContext(Dispatchers.IO) {
        if (!_isConnected.value) throw EmbedderClientException.connectionError()
        val byteArray = bitmapToByteArray(data)
        mTextEmbedderService?.embed(byteArray) ?: throw EmbedderClientException.embeddingError()
    }

    suspend fun embedBatch( data: List<ByteArray>): List<FloatArray> = withContext(Dispatchers.IO) {
        if (!_isConnected.value) throw EmbedderClientException.connectionError()
        val encoded = encodeByteArrayPayload(data, mTextEmbedderService!!.delimiter)
        val embeddings = mTextEmbedderService?.embedBatch(encoded)
            ?: throw EmbedderClientException.embeddingError()
        unflattenEmbeddings(embeddings, mTextEmbedderService!!.embeddingDim)
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
        mTextEmbedderService = null
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