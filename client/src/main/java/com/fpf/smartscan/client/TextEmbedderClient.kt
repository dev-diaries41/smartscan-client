package com.fpf.smartscan.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.fpf.smartscan.ITextEmbedderService
import com.fpf.smartscansdk.core.data.TextEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.unflattenEmbeddings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class TextEmbedderClient(
    private val context: Context
): TextEmbeddingProvider {

    companion object {
        private const val APP_PACKAGE_NAME = "com.fpf.smartscan"
        private const val INTENT_ACTION_NAME = "com.fpf.smartscan.intent.TEXT_EMBED"
        const val TAG = "TextEmbedderClient"
    }

    private var mTextEmbedderService: ITextEmbedderService? = null
    private var isBound = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val mIndexServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "Service has connected successfully")
            mTextEmbedderService = ITextEmbedderService.Stub.asInterface(service)
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

    override suspend fun embed(data: String): FloatArray = withContext(Dispatchers.IO) {
        if (!_isConnected.value) throw EmbedderClientException.connectionError()
        mTextEmbedderService?.embed(data) ?: throw EmbedderClientException.embeddingError()
    }

    override suspend fun embedBatch( data: List<String>): List<FloatArray> =
        withContext(Dispatchers.IO) {
            if (!_isConnected.value) throw EmbedderClientException.connectionError()
            val embeddings = mTextEmbedderService?.embedBatch(data)
                ?: throw EmbedderClientException.embeddingError()
            unflattenEmbeddings(embeddings, mTextEmbedderService!!.embeddingDim)
        }

    fun disconnectService(){
        if (isBound){
            context.applicationContext.unbindService(mIndexServiceConnection)
        }
        reset()
    }

    private fun reset(){
        mTextEmbedderService = null
        isBound = false
        _isConnected.value = false
    }
}