# SmartScan Client

## Overview

The **SmartScan Client** library enables Android apps to connect to the [SmartScan](https://github.com/dev-diaries41/smartscan) app’s AIDL services for generating embeddings from text or images.
It provides simple client interfaces for the **Text Embedder** and **Image Embedder** services.

> **Note:** The **SmartScan** app must be installed on the same device for these clients to function.

---

## Dependencies

The **SmartScan Client** depends only on and re-exports the **`smartscan-sdk core`** module.

This provides users with the **minimal core non-ML functionality** (indexing, search, embedding-utils and more) used in the SmartScan app, while allowing access to the ML inference`` capabilities via the **AIDL services** and the SmartScan app.

For details, see the [smartscan-sdk](https://github.com/dev-diaries41/smartscan-sdk).


## Usage

### 1. `TextEmbedderClient`

Provides access to the **Text Embedding Service**.

**Purpose:**
Generate vector embeddings from text strings or batches of text for downstream tasks such as semantic search or similarity comparison.

**Usage Example:**

```kotlin
val textEmbedderClient = TextEmbedderClient(context)
textEmbedderClient.connectService()

val text = "Text to embed"
val embedding = textEmbedderClient.embed(text)

textEmbedderClient.disconnectService()
```

**Batch Example:**

```kotlin
val texts = listOf("first sentence", "second sentence")
val embeddings = textEmbedderClient.embedBatch(texts)
```

---

### 2. `ImageEmbedderClient`

Provides access to the **Image Embedding Service**.

**Purpose:**
Generate vector embeddings from images (as `Bitmap`) for visual search or similarity tasks.

**Usage Example**

```kotlin
val imageEmbedderClient = ImageEmbedderClient(context)
imageEmbedderClient.connectService()

val embedding = imageEmbedderClient.embed(bitmap)

```


**Batch Example:**

```kotlin
val images: List<Bitmap> = ...
val embeddings = imageEmbedderClient.embedBatch(images)
```

---

## Connection Lifecycle

Both clients expose:

* `connectService()` — Bind to the SmartScan service.
* `disconnectService()` — Unbind and reset the connection.
* `isConnected: StateFlow<Boolean>` — Reflects connection state in real time.

---

## Error Handling

Embedding methods throw `EmbedderClientException` if:

* The service is not connected.
* The embedding operation fails.

---

## Direct AIDL Usage (Without the Client Library)

Developers who prefer to manage service binding and communication manually can use the AIDL interfaces directly.

### `ITextEmbedderService.aidl`

```aidl
package com.fpf.smartscan;

import java.util.List;

interface ITextEmbedderService {
    int getEmbeddingDim();
    void closeSession();
    float[] embed(in String data);
    float[] embedBatch(in List<String> data); // concatenated output
}
```

### `IImageEmbedderService.aidl`

```aidl
package com.fpf.smartscan;

import java.util.List;

interface IImageEmbedderService {
    int getEmbeddingDim();
    byte[] getDelimiter();
    void closeSession();
    float[] embed(in byte[] data);
    float[] embedBatch(in byte[] data); // inputs and outputs are concatenated
}
```

Using these interfaces, apps can bind to the SmartScan services (`com.fpf.smartscan.intent.TEXT_EMBED` and `com.fpf.smartscan.intent.IMAGE_EMBED`) and handle embedding operations directly without the `SmartScan Client` library. You will also need to add the relevant permissions and queries to your manifest if you do not use the library. See the libraries manifest to see how.

---

