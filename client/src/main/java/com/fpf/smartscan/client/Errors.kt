package com.fpf.smartscan.client

class EmbedderClientException private  constructor (message: String):  Exception(message){

    companion object {
        const val CONNECTION_ERROR = "service not connected"
        const val EMBEDDING_ERROR = "embedding error"

        fun connectionError() = EmbedderClientException(CONNECTION_ERROR)

        fun embeddingError() = EmbedderClientException(EMBEDDING_ERROR)
    }
}