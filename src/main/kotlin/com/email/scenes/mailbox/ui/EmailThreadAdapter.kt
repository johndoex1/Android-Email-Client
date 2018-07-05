package com.email.scenes.mailbox.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.email.R
import com.email.db.models.Label
import com.email.email_preview.EmailPreview
import com.email.scenes.mailbox.VirtualEmailThreadList
import com.email.scenes.mailbox.holders.EmailHolder
import com.email.utils.ui.EmptyViewHolder
import com.email.utils.virtuallist.VirtualListAdapter

/**
 * Created by sebas on 1/23/18.
 */

class EmailThreadAdapter(private val mContext : Context,
                          private val threadListener : OnThreadEventListener,
                          private val sentByMe: Boolean,
                          private val threadList: VirtualEmailThreadList)
    : VirtualListAdapter(threadList) {

    private fun toggleThreadSelection(mailThread: EmailPreview, position: Int) {
        threadListener.onToggleThreadSelection(mailThread, position)
    }

    private fun goToMail(emailPreview: EmailPreview) {
        threadListener.onGoToMail(emailPreview)
    }

    override fun onApproachingEnd() {
        threadListener.onApproachingEnd()
    }

    private fun setEmailHolderListeners(holder: EmailHolder, position: Int, mail: EmailPreview) {
        val itemClickListener = {
            toggleThreadSelection(mail, position)
        }
        holder.setOnAvatarClickedListener(itemClickListener)
        holder.setOnIconBackClickedListener(itemClickListener)

        holder.itemView.setOnClickListener {
            if (! threadList.isInMultiSelectMode) goToMail(emailPreview = mail)
            else toggleThreadSelection(mail, position)
        }

        holder.itemView.setOnLongClickListener({
            toggleThreadSelection(mail, position)
            true
        })
    }

    override fun createEmptyViewHolder(parent: ViewGroup): EmptyViewHolder {
        val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_no_emails, parent,
                        false)
        return EmptyViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is EmailHolder -> {
                if (holder.itemView == null) return
                val mail = threadList[position]
                holder.bindEmailPreview(mail, sentByMe)
                setEmailHolderListeners(holder, position, mail)
            }
        }
    }

    private fun createMailItemView(): View {
        return View.inflate(mContext, R.layout.mail_item, null)
    }

    override fun onCreateActualViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView: View = createMailItemView()
        return EmailHolder(itemView)
    }

    override fun getActualItemViewType(position: Int): Int {
        return 1
    }

    interface OnThreadEventListener {
        fun onToggleThreadSelection(thread: EmailPreview, position: Int)
        fun onGoToMail(emailPreview: EmailPreview)
        fun onApproachingEnd()
    }

    override fun getActualItemId(position: Int): Long {
        return threadList[position].emailId
    }
}
