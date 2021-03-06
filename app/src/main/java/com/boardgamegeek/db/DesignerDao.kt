package com.boardgamegeek.db

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.PersonGameEntity
import com.boardgamegeek.entities.PersonImagesEntity
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.model.PersonResponse2
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.io.model.Person
import com.boardgamegeek.provider.BggContract
import timber.log.Timber

class DesignerDao(private val context: BggApplication) {
    fun loadDesignerAsLiveData(id: Int): LiveData<PersonEntity> {
        return RegisteredLiveData(context, BggContract.Designers.buildDesignerUri(id), true) {
            return@RegisteredLiveData loadDesigner(id)
        }
    }

    private fun loadDesigner(id: Int): PersonEntity? {
        return context.contentResolver.load(
                BggContract.Designers.buildDesignerUri(id),
                arrayOf(
                        BggContract.Designers.DESIGNER_ID,
                        BggContract.Designers.DESIGNER_NAME,
                        BggContract.Designers.DESIGNER_DESCRIPTION,
                        BggContract.Designers.UPDATED
                )
        )?.use {
            if (it.moveToFirst()) {
                PersonEntity(
                        it.getInt(BggContract.Designers.DESIGNER_ID),
                        it.getStringOrEmpty(BggContract.Designers.DESIGNER_NAME),
                        it.getStringOrEmpty(BggContract.Designers.DESIGNER_DESCRIPTION),
                        it.getLongOrZero(BggContract.Designers.UPDATED)
                )
            } else null
        }
    }

    fun saveDesigner(id: Int, designer: Person?, updateTime: Long = System.currentTimeMillis()): Int {
        if (designer != null && !designer.name.isNullOrBlank()) {
            val values = contentValuesOf(
                    BggContract.Designers.DESIGNER_NAME to designer.name,
                    BggContract.Designers.DESIGNER_DESCRIPTION to (if (designer.description == "This page does not exist. You can edit this page to create it.") "" else designer.description),
                    BggContract.Designers.UPDATED to updateTime
            )
            return upsert(values, id)
        }
        return 0
    }

    fun loadDesignerImagesAsLiveData(id: Int): LiveData<PersonImagesEntity> {
        return RegisteredLiveData(context, BggContract.Designers.buildDesignerUri(id), true) {
            return@RegisteredLiveData loadDesignerImages(id)
        }
    }

    private fun loadDesignerImages(id: Int): PersonImagesEntity? {
        return context.contentResolver.load(
                BggContract.Designers.buildDesignerUri(id),
                arrayOf(
                        BggContract.Designers.DESIGNER_ID,
                        BggContract.Designers.DESIGNER_IMAGE_URL,
                        BggContract.Designers.DESIGNER_THUMBNAIL_URL,
                        BggContract.Designers.DESIGNER_HERO_IMAGE_URL,
                        BggContract.Designers.DESIGNER_IMAGES_UPDATED_TIMESTAMP
                )
        )?.use {
            if (it.moveToFirst()) {
                PersonImagesEntity(
                        it.getInt(BggContract.Designers.DESIGNER_ID),
                        it.getStringOrEmpty(BggContract.Designers.DESIGNER_IMAGE_URL),
                        it.getStringOrEmpty(BggContract.Designers.DESIGNER_THUMBNAIL_URL),
                        it.getStringOrEmpty(BggContract.Designers.DESIGNER_HERO_IMAGE_URL),
                        it.getLongOrZero(BggContract.Designers.DESIGNER_IMAGES_UPDATED_TIMESTAMP)
                )
            } else null
        }
    }

    fun saveDesignerImage(id: Int, designer: PersonResponse2?, updateTime: Long = System.currentTimeMillis()): Int {
        if (designer != null) {
            val values = contentValuesOf(
                    BggContract.Designers.DESIGNER_IMAGE_URL to designer.items[0].image,
                    BggContract.Designers.DESIGNER_THUMBNAIL_URL to designer.items[0].thumbnail,
                    BggContract.Designers.DESIGNER_IMAGES_UPDATED_TIMESTAMP to updateTime
            )
            return upsert(values, id)
        }
        return 0
    }

    fun loadCollectionAsLiveData(id: Int): LiveData<List<PersonGameEntity>>? {
        return RegisteredLiveData(context, BggContract.Designers.buildDesignerCollectionUri(id), true) {
            return@RegisteredLiveData loadCollection(id)
        }
    }

    private fun loadCollection(designerId: Int): List<PersonGameEntity> {
        val list = arrayListOf<PersonGameEntity>()
        context.contentResolver.load(
                BggContract.Designers.buildDesignerCollectionUri(designerId),
                arrayOf(
                        "games." + BggContract.Collection.GAME_ID,
                        BggContract.Collection.GAME_NAME,
                        BggContract.Collection.COLLECTION_NAME,
                        BggContract.Collection.COLLECTION_YEAR_PUBLISHED,
                        BggContract.Collection.COLLECTION_THUMBNAIL_URL,
                        BggContract.Collection.THUMBNAIL_URL,
                        BggContract.Collection.HERO_IMAGE_URL
                )
        )?.use {
            if (it.moveToFirst()) {
                do {
                    list += PersonGameEntity(
                            it.getInt(BggContract.Collection.GAME_ID),
                            it.getStringOrEmpty(BggContract.Collection.GAME_NAME),
                            it.getStringOrEmpty(BggContract.Collection.COLLECTION_NAME),
                            it.getIntOrNull(BggContract.Collection.COLLECTION_YEAR_PUBLISHED) ?: YEAR_UNKNOWN,
                            it.getStringOrEmpty(BggContract.Collection.COLLECTION_THUMBNAIL_URL),
                            it.getStringOrEmpty(BggContract.Collection.THUMBNAIL_URL),
                            it.getStringOrEmpty(BggContract.Collection.HERO_IMAGE_URL)
                    )
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun update(designerId: Int, values: ContentValues): Int {
        return context.contentResolver.update(BggContract.Designers.buildDesignerUri(designerId), values, null, null)
    }

    private fun upsert(values: ContentValues, designerId: Int): Int {
        val resolver = context.contentResolver
        val uri = BggContract.Designers.buildDesignerUri(designerId)
        return if (resolver.rowExists(uri)) {
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d designer rows at %s", count, uri)
            count
        } else {
            values.put(BggContract.Designers.DESIGNER_ID, designerId)
            val insertedUri = resolver.insert(BggContract.Designers.CONTENT_URI, values)
            Timber.d("Inserted designer at %s", insertedUri)
            1
        }
    }
}