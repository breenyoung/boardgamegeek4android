package com.boardgamegeek.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.fadeIn
import com.boardgamegeek.setViewBackground
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.PlayerNumberRow
import kotlinx.android.synthetic.main.fragment_poll_suggested_player_count.*
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx

class SuggestedPlayerCountPollFragment : DialogFragment() {
    val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(act).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_poll_suggested_player_count, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog.setTitle(R.string.suggested_numplayers)

        addKeyRow(R.color.best, R.string.best)
        addKeyRow(R.color.recommended, R.string.recommended)
        addKeyRow(R.color.not_recommended, R.string.not_recommended)

        viewModel.playerPoll.observe(this, Observer { entity ->
            val totalVoteCount = entity?.totalVotes ?: 0
            totalVoteView?.text = resources.getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount)

            pollList?.visibility = if (totalVoteCount == 0) View.GONE else View.VISIBLE
            keyContainer?.visibility = if (totalVoteCount == 0) View.GONE else View.VISIBLE
            noVotesSwitch?.visibility = if (totalVoteCount == 0) View.GONE else View.VISIBLE
            if (totalVoteCount > 0) {
                pollList?.removeAllViews()
                for ((_, playerCount, bestVoteCount, recommendedVoteCount, notRecommendedVoteCount) in entity!!.results) {
                    val row = PlayerNumberRow(ctx)
                    row.setText(playerCount)
                    row.setVotes(bestVoteCount, recommendedVoteCount, notRecommendedVoteCount, totalVoteCount)
                    row.setOnClickListener { v ->
                        for (i in 0 until pollList.childCount) {
                            (pollList.getChildAt(i) as PlayerNumberRow).clearHighlight()
                        }
                        val playerNumberRow = v as PlayerNumberRow
                        playerNumberRow.setHighlight()

                        val voteCount = playerNumberRow.votes
                        for (i in 0 until keyContainer.childCount) {
                            keyContainer.getChildAt(i).findViewById<TextView>(R.id.infoView).text = voteCount[i].toString()
                        }
                    }
                    pollList.addView(row)
                }

                noVotesSwitch?.setOnClickListener {
                    for (i in 0 until pollList.childCount) {
                        val row = pollList.getChildAt(i) as PlayerNumberRow
                        row.showNoVotes(noVotesSwitch.isChecked)
                    }
                }
            }

            progressView?.hide()
            scrollView.fadeIn()
        })
    }

    private fun addKeyRow(@ColorRes colorResId: Int, @StringRes textResId: Int) {
        val v = LayoutInflater.from(context).inflate(R.layout.row_poll_key, keyContainer, false) as ViewGroup
        v.findViewById<TextView>(R.id.textView).setText(textResId)
        v.findViewById<View>(R.id.colorView).setViewBackground(ContextCompat.getColor(ctx, colorResId))
        keyContainer?.addView(v)
    }

    companion object {
        fun launch(host: Fragment) {
            val dialog = SuggestedPlayerCountPollFragment()
            dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
            host.showAndSurvive(dialog)
        }
    }
}
