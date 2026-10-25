package com.ehan.videoplayer2

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getLocalVideos(context: Context): List<LocalVideo> =
    withContext(Dispatchers.IO) {
        val videos = mutableListOf<LocalVideo>()

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )

        val sortOrder =
            "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Video.Media._ID
            )
            val nameColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Video.Media.DISPLAY_NAME
            )
            val durationColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Video.Media.DURATION
            )
            val sizeColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Video.Media.SIZE
            )

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)

                videos.add(
                    LocalVideo(
                        id = id,
                        uri = ContentUris.withAppendedId(
                            collection,
                            id
                        ),
                        name = cursor.getString(nameColumn)
                            ?: "Unknown video",
                        durationMs = cursor.getLong(durationColumn),
                        sizeBytes = cursor.getLong(sizeColumn)
                    )
                )
            }
        }

        videos
    }