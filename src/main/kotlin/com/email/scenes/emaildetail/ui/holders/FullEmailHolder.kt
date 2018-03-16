package com.email.scenes.emaildetail.ui.holders

import android.support.v7.widget.PopupMenu
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.email.R
import com.email.db.models.FullEmail
import com.email.scenes.emaildetail.ui.AttachmentHistoryPopUp
import com.email.scenes.emaildetail.ui.EmailContactInfoPopup
import com.email.scenes.emaildetail.ui.FullEmailListAdapter
import com.email.scenes.emaildetail.ui.ReadHistoryPopUp

/**
 * Created by sebas on 3/12/18.
 */

class FullEmailHolder(view: View) : ParentEmailHolder(view) {

    private val context = view.context
    private val layout : FrameLayout
    private val replyView: ImageView
    private val moreView: ImageView
    private val toView: TextView
    private val attachmentView: ImageView
    private val readView: ImageView
    private val unsendView: ImageView
    private val layoutAttachment : RelativeLayout
    private val contactInfoPopUp: EmailContactInfoPopup
    private val readHistoryPopUp: ReadHistoryPopUp
    private val attachmentHistoryPopUp: AttachmentHistoryPopUp

    override fun setListeners(fullEmail: FullEmail,
                     emailListener: FullEmailListAdapter.OnFullEmailEventListener?,
                     adapter: FullEmailListAdapter, position: Int) {
        view.setOnClickListener {

            emailListener?.ontoggleViewOpen(
                    fullEmail = fullEmail,
                    position = position,
                    viewOpen = false)
        }
        moreView.setOnClickListener({
            displayPopMenu(emailListener, fullEmail, adapter, position)
        })

        readView.setOnClickListener({
            readHistoryPopUp.createPopup(fullEmail, null)
        })

        toView.setOnClickListener({
            contactInfoPopUp.createPopup(fullEmail, null)
        })

        attachmentView.setOnClickListener {
            attachmentHistoryPopUp.createPopup(fullEmail, null)
        }

        layoutAttachment.setOnClickListener{
            TODO("HANDLE CLICK TO ATTACHMENT")
        }
    }

    private fun displayPopMenu(emailListener: FullEmailListAdapter.OnFullEmailEventListener?, fullEmail: FullEmail,
                               adapter: FullEmailListAdapter, position: Int){
        val popupMenu = createPopupMenu(fullEmail)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.forward ->
                    emailListener?.onReplyOptionSelected(
                            fullEmail = fullEmail,
                            position = position,
                            all = false)
                R.id.mark_read, R.id.mark_unread -> {
                    emailListener?.onToggleReadOption(
                            fullEmail = fullEmail,
                            position = position,
                            markAsRead = item.itemId == R.id.mark_read)
                }
                R.id.delete ->
                    emailListener?.onDeleteOptionSelected(
                            fullEmail = fullEmail,
                            position = position )
            }
            false
        }

        popupMenu.show()

    }

    private fun createPopupMenu(fullEmail: FullEmail): PopupMenu {
        val popupMenu = PopupMenu(context , moreView)

    val popuplayout =
            if (fullEmail.email.unread)
                R.menu.mail_options_unread_menu
            else
                R.menu.mail_options_read_menu

        popupMenu.inflate(popuplayout)
        return popupMenu
    }

    override fun bindFullMail(fullEmail: FullEmail) {
        bodyView.text = fullEmail.email.content
        val numberContacts = fullEmail.to.size
        if(fullEmail.from != null) {
            headerView.text = fullEmail.from.name
            toView.text = "To me and ${numberContacts  - 1} contacts"
        }
        else {
            headerView.text = "Me"
            replyView.visibility = View.GONE
            toView.text = "To ${numberContacts} contacts"
        }
    }

    init {
        layout = view.findViewById(R.id.open_full_mail_item_container)
        toView = view.findViewById(R.id.to)
        moreView = view.findViewById(R.id.more)
        replyView = view.findViewById(R.id.reply)
        attachmentView =  view.findViewById(R.id.attachment)
        readView =  view.findViewById(R.id.read)
        unsendView =  view.findViewById(R.id.unsend)

        layoutAttachment = view.findViewById(R.id.open_full_mail_attachment_container)
        contactInfoPopUp = EmailContactInfoPopup(toView)
        readHistoryPopUp = ReadHistoryPopUp(readView)
        attachmentHistoryPopUp = AttachmentHistoryPopUp(attachmentView)
    }

}
