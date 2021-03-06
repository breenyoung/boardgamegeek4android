package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.PersonGameEntity
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadUrl
import com.boardgamegeek.ui.GameActivity
import kotlinx.android.synthetic.main.row_collection.view.*
import kotlin.properties.Delegates

class PersonCollectionAdapter : RecyclerView.Adapter<PersonCollectionAdapter.DetailViewHolder>(), AutoUpdatableAdapter {
    init {
        setHasStableIds(true)
    }

    var items: List<PersonGameEntity> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o.gameId == n.gameId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        return DetailViewHolder(parent.inflate(R.layout.row_collection))
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.gameId?.toLong() ?: RecyclerView.NO_ID

    inner class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(gameDetail: PersonGameEntity?) {
            gameDetail?.let { entity ->
                itemView.name.text = entity.collectionName
                itemView.year.text = entity.yearPublished.asYear(itemView.context)
                itemView.thumbnail.loadUrl(entity.collectionThumbnailUrl)
                itemView.setOnClickListener { GameActivity.start(itemView.context, entity.gameId, entity.gameName, entity.gameThumbnailUrl, entity.gameHeroImageUrl) }
            }
        }
    }
}
