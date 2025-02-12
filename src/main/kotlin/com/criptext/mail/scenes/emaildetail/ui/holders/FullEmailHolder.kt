package com.criptext.mail.scenes.emaildetail.ui.holders

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.criptext.mail.IHostActivity
import com.criptext.mail.R
import com.criptext.mail.db.DeliveryTypes
import com.criptext.mail.db.models.FileDetail
import com.criptext.mail.db.models.FullEmail
import com.criptext.mail.db.models.Label
import com.criptext.mail.scenes.composer.ui.holders.AttachmentViewObserver
import com.criptext.mail.scenes.emaildetail.WebviewJavascriptInterface
import com.criptext.mail.scenes.emaildetail.ui.EmailContactInfoPopup
import com.criptext.mail.scenes.emaildetail.ui.FileListAdapter
import com.criptext.mail.scenes.emaildetail.ui.FullEmailListAdapter
import com.criptext.mail.utils.*
import com.criptext.mail.utils.ui.MyZoomLayout
import com.otaliastudios.zoom.ZoomEngine
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.lang.Exception


/**
 * Created by sebas on 3/12/18.
 */

class FullEmailHolder(view: View) : ParentEmailHolder(view) {

    private val context = view.context
    private val layout : FrameLayout
    private val rootView : LinearLayout
    private val continueDraftView: ImageView
    private val deleteDraftView: ImageView
    private val replyView: ImageView
    private val threePointsView: ImageView
    private val moreButton: TextView
    private val toView: TextView
    private val readView: ImageView
    private val contactInfoPopUp: EmailContactInfoPopup
    private val bodyWebView: WebView
    private val bodyContainer : LinearLayout
    private val zoomLayout: MyZoomLayout
    private val attachmentsRecyclerView: RecyclerView
    private val leftImageView: CircleImageView
    private val unsendProgressBar: ProgressBar
    private val isSecure : ImageView

    private var listener: FullEmailListAdapter.OnFullEmailEventListener? = null

    override fun setListeners(fullEmail: FullEmail, fileDetails: List<FileDetail>,
                     emailListener: FullEmailListAdapter.OnFullEmailEventListener?,
                     adapter: FullEmailListAdapter, position: Int) {
        listener = emailListener

        view.setOnClickListener {

            emailListener?.ontoggleViewOpen(
                    fullEmail = fullEmail,
                    position = position,
                    viewOpen = false)
        }
        threePointsView.setOnClickListener {
            displayPopMenu(emailListener, fullEmail, position - 1)
        }

        moreButton.setOnClickListener {
            contactInfoPopUp.createPopup(fullEmail)
        }

        continueDraftView.setOnClickListener{
            emailListener?.onContinueDraftOptionSelected(fullEmail)
        }

        deleteDraftView.setOnClickListener{
            emailListener?.onDeleteDraftOptionSelected(fullEmail)
        }

        replyView.setOnClickListener{
            emailListener?.onReplyOptionSelected(
                    fullEmail = fullEmail,
                    position = position,
                    all = false)
        }

        isSecure.visibility = if(fullEmail.email.secure) View.VISIBLE else View.GONE

        showStartGuideEmailIsRead(emailListener, fullEmail)

        showStartGuideMenu(emailListener, fullEmail)

        setAttachments(fileDetails, emailListener, adapter)

        emailListener?.contextMenuRegister(bodyWebView)

    }

    override fun setBackground(drawable: Drawable) {
        rootView.background = drawable
    }

    override fun setBottomMargin(marginBottom: Int) {
        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = marginBottom
        rootView.layoutParams = params
    }

    private fun displayPopMenu(emailListener: FullEmailListAdapter.OnFullEmailEventListener?, fullEmail: FullEmail, position: Int){
        val popupMenu = createPopupMenu(fullEmail)
        if(fullEmail.email.delivered == DeliveryTypes.NONE && fullEmail.email.boundary != null)
            popupMenu.menu.findItem(R.id.source).isVisible = true

        if(fullEmail.email.delivered in listOf(DeliveryTypes.FAIL, DeliveryTypes.SENDING))
            popupMenu.menu.findItem(R.id.retry)?.isVisible = true

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.reply_all ->
                    emailListener?.onReplyAllOptionSelected(
                            fullEmail = fullEmail,
                            position = position,
                            all = false)
                R.id.reply ->
                    emailListener?.onReplyOptionSelected(
                            fullEmail = fullEmail,
                            position = position,
                            all = false)
                R.id.forward ->
                    emailListener?.onForwardOptionSelected(
                            fullEmail = fullEmail,
                            position = position,
                            all = false)
                R.id.mark_read, R.id.mark_unread -> {
                    emailListener?.onToggleReadOption(
                            fullEmail = fullEmail,
                            position = position,
                            markAsRead = item.itemId == R.id.mark_read)
                }
                R.id.unsend -> {
                    emailListener?.onUnsendEmail(
                                fullEmail = fullEmail,
                                position = position)
                }
                R.id.delete ->
                    emailListener?.onDeleteOptionSelected(
                            fullEmail = fullEmail,
                            position = position )
                R.id.mark_spam ->
                    emailListener?.onSpamOptionSelected(
                            fullEmail = fullEmail,
                            position = position )
                R.id.print ->
                    emailListener?.onPrintOptionSelected(
                            fullEmail = fullEmail)
                R.id.source ->
                    emailListener?.onSourceOptionSelected(
                            fullEmail = fullEmail)
                R.id.retry ->
                    emailListener?.onRetrySendOptionSelected(
                            fullEmail = fullEmail,
                            position = position)

            }
            false
        }

        popupMenu.gravity = Gravity.END
        popupMenu.show()

    }

    private fun createPopupMenu(fullEmail: FullEmail): PopupMenu {
        val wrapper = ContextThemeWrapper(context, R.style.email_detail_popup_menu)
        val popupMenu = PopupMenu(wrapper , threePointsView)

        val popuplayout = if(fullEmail.email.delivered == DeliveryTypes.NONE
                || fullEmail.email.delivered == DeliveryTypes.UNSEND
                || !fullEmail.email.secure) {
                    if (fullEmail.email.unread) {
                        when {
                            fullEmail.labels.contains(Label.defaultItems.trash) -> R.menu.mail_options_unread_menu_in_trash
                            fullEmail.labels.contains(Label.defaultItems.spam) -> R.menu.mail_options_unread_menu_in_spam
                            else -> R.menu.mail_options_unread_menu
                        }
                    }else {
                        when {
                            fullEmail.labels.contains(Label.defaultItems.trash) -> R.menu.mail_options_read_menu_in_trash
                            fullEmail.labels.contains(Label.defaultItems.spam) -> R.menu.mail_options_read_menu_in_spam
                            else -> R.menu.mail_options_read_menu
                        }
                     }
        }else{
            if (fullEmail.email.unread) {
                when {
                    fullEmail.labels.contains(Label.defaultItems.trash) -> R.menu.mail_options_unread_menu_sent_in_trash
                    fullEmail.labels.contains(Label.defaultItems.spam) -> R.menu.mail_options_unread_menu_sent_in_spam
                    else -> R.menu.mail_options_unread_menu_sent
                }
            }else {
                when {
                    fullEmail.labels.contains(Label.defaultItems.trash) -> R.menu.mail_options_read_menu_sent_in_trash
                    fullEmail.labels.contains(Label.defaultItems.spam) -> R.menu.mail_options_read_menu_sent_in_spam
                    else -> R.menu.mail_options_read_menu_sent
                }
            }
        }

        popupMenu.inflate(popuplayout)
        return popupMenu
    }

    override fun bindFullMail(fullEmail: FullEmail) {

        if(fullEmail.email.delivered != DeliveryTypes.UNSEND) {
            val content = if(HTMLUtils.isHtmlEmpty(fullEmail.email.content)){
                "<font color=\"#6a707e\"><i>" + context.getLocalizedUIMessage(UIMessage(R.string.nocontent)) + "</i></font>"
            } else {
                fullEmail.email.content
            }
            bodyWebView.loadDataWithBaseURL("", HTMLUtils.
                    changedHeaderHtml(content, EmailUtils.checkIfItsForward(fullEmail.email.subject)), "text/html", "utf-8", "")
            bodyWebView.setBackgroundColor(context.getColorFromAttr(R.attr.criptextColorBackground))
            setDefaultBackgroundColors()
        }
        else {
            val unsentDate = fullEmail.email.unsentDate
            val finalDate = if(unsentDate != null){
                DateAndTimeUtils.getUnsentDate(unsentDate.time, context)
            }else{
                context.getLocalizedUIMessage(UIMessage(R.string.unsent))
            }
            rootView.background = ContextCompat.getDrawable(
                    view.context, R.drawable.background_cardview_unsend)
            bodyWebView.loadDataWithBaseURL("", HTMLUtils.
                    changedHeaderHtml(finalDate, EmailUtils.checkIfItsForward(fullEmail.email.subject)),
                    "text/html", "utf-8", "")
            deactivateElementsForUnsend()
        }

        dateView.text = DateAndTimeUtils.getFormattedDate(fullEmail.email.date.time, context)
        headerView.text =
                if(EmailThreadValidator.isLabelInList(fullEmail.labels, Label.LABEL_DRAFT)) {
                    headerView.setTextColor(ContextCompat.getColor(headerView.context, R.color.colorUnsent))
                    headerView.context.getString(R.string.draft)
                }
                else {
                    headerView.setTextColor(context.getColorFromAttr(R.attr.criptextPrimaryTextColor))
                    fullEmail.from.name
                }

        if(fullEmail.isUnsending)
            unsendProgressBar.visibility = View.VISIBLE
        else
            unsendProgressBar.visibility = View.INVISIBLE

        val contactFrom = fullEmail.from
        val domain = EmailAddressUtils.extractEmailAddressDomain(contactFrom.email)
        UIUtils.setProfilePicture(
                iv = leftImageView,
                resources = context.resources,
                recipientId = EmailAddressUtils.extractRecipientIdFromAddress(contactFrom.email, domain),
                name = contactFrom.name,
                runnable = null,
                domain = domain)

        setToText(fullEmail)
        setDraftIcon(fullEmail)
        setIcons(fullEmail.email.delivered)
    }

    private fun setAttachments(files: List<FileDetail>, emailListener: FullEmailListAdapter.OnFullEmailEventListener?,
                               fullEmailListAdapter: FullEmailListAdapter){
        val nonInlineFiles = files.filter { it.file.cid == null || it.file.cid == "" }
        val adapter = FileListAdapter(view.context, nonInlineFiles)
        val mLayoutManager = LinearLayoutManager(view.context)
        adapter.observer = object: AttachmentViewObserver {
            override fun onAttachmentViewClick(position: Int) {
                val emailPosition = if(!fullEmailListAdapter.isExpanded) {
                    if(adapterPosition == 1)
                        adapterPosition - 1
                    else
                        fullEmailListAdapter.getEmailsSize() - 1
                } else {
                    adapterPosition - 1
                }
                emailListener?.onAttachmentSelected(emailPosition, position)
            }
            override fun onRemoveAttachmentClicked(position: Int) {}
        }
        attachmentsRecyclerView.layoutManager = mLayoutManager
        attachmentsRecyclerView.adapter = adapter
    }

    private fun setDraftIcon(fullEmail: FullEmail){
        if(fullEmail.labels.contains(Label.defaultItems.draft)){
            continueDraftView.visibility = View.VISIBLE
            deleteDraftView.visibility = View.VISIBLE
            replyView.visibility = View.GONE
            threePointsView.visibility = View.GONE
        }
        else{
            continueDraftView.visibility = View.GONE
            deleteDraftView.visibility = View.GONE
            replyView.visibility = View.VISIBLE
            threePointsView.visibility = View.VISIBLE
        }
    }

    private fun setToText(fullEmail: FullEmail){
        val numberContacts = fullEmail.to.size
        val isFromMe = (fullEmail.email.delivered != DeliveryTypes.NONE
                || EmailThreadValidator.isLabelInList(fullEmail.labels, Label.LABEL_DRAFT))
        toView.text = when {
            isFromMe ->
                "${toView.resources.getString(R.string.to)} ${fullEmail.to.joinToString { it.name }}"
            numberContacts == 2 ->
                "${toView.resources.getString(R.string.to_me)} and ${fullEmail.to.joinToString { it.name }}"
            numberContacts > 2 ->
                "${toView.resources.getString(R.string.to_me)}, ${fullEmail.to.joinToString { it.name }}"
            else ->
                toView.resources.getString(R.string.to_me)
        }
    }

    private fun setIcons(deliveryType: DeliveryTypes){

        readView.visibility = View.VISIBLE

        when(deliveryType){
            DeliveryTypes.UNSEND -> {
                readView.visibility = View.GONE
            }
            DeliveryTypes.SENDING -> {
                setIconAndColor(R.drawable.clock, R.color.sent)
            }
            DeliveryTypes.READ -> {
                setIconAndColor(R.drawable.read, R.color.azure)
            }
            DeliveryTypes.DELIVERED -> {
                setIconAndColor(R.drawable.read, R.color.sent)
            }
            DeliveryTypes.SENT -> {
                setIconAndColor(R.drawable.mail_sent, R.color.sent)
            }
            else -> {
                readView.visibility = View.GONE
            }
        }
    }

    private fun setIconAndColor(drawable: Int, color: Int){
        Picasso.get().load(drawable).into(readView, object : Callback {
            override fun onError(e: Exception?) {

            }
            override fun onSuccess() {
                DrawableCompat.setTint(readView.drawable,
                        ContextCompat.getColor(view.context, color))
            }
        })
    }

    private fun setupWebview(){
        val metrics = DisplayMetrics()
        val display = (context.getSystemService(
                Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        display.getMetrics(metrics)
        bodyWebView.layoutParams = FrameLayout.LayoutParams(
                metrics.widthPixels - context.resources.
                        getDimension(R.dimen.webview_left_margin).toInt(), bodyWebView.layoutParams.height)

        val webSettings = bodyWebView.settings
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        bodyWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                WebViewUtils.openUrl(bodyWebView.context!!, url)
                return true
            }
            @TargetApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                WebViewUtils.openUrl(bodyWebView.context!!, request.url.toString())
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript("""window.scrollTo(0,0);""") { }
                reSizeZoomLayout(view, true)
                setupZoomLayout()
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                if (url?.contains("cid") == true) {
                    val cid = url.substringAfter("cid:")
                    listener?.onResourceLoaded(cid)
                } else {
                    super.onLoadResource(view, url)
                }
            }
        }
        val javascriptInterface = WebviewJavascriptInterface(context, zoomLayout, bodyWebView)
        bodyWebView.addJavascriptInterface(javascriptInterface, "CriptextSecureEmail")
    }

    private fun reSizeZoomLayout(view: WebView?, pageFinished: Boolean){
        if(view == null){
            return
        }
        if(view.height > 0) {
            zoomLayout.layoutParams = LinearLayout.LayoutParams(view.width, view.height)
            return
        }
        //Sometimes onPageFinished is called when the webView has not finished loading
        //So I put a temporal height to the webView and then call the manual zoom method
        if(pageFinished){
            zoomLayout.layoutParams = LinearLayout.LayoutParams(view.width, 250)
            view.postDelayed({
                zoomLayout.realZoomTo(1.0f, false)
            }, 500)
        }
    }

    private fun setupZoomLayout(){
        zoomLayout.mListener = object : MyZoomLayout.ZoomUpdateListener{
            override fun onUpdate(helper: ZoomEngine?, matrix: Matrix?) {
                val values = FloatArray(9)
                matrix?.getValues(values)
                val scaleY = values[Matrix.MSCALE_Y]
                zoomLayout.layoutParams = LinearLayout.LayoutParams(bodyWebView.width, (scaleY * bodyWebView.height).toInt())
            }
        }
    }

    private fun setupWeChromeClient(): WebChromeClient{
        return object: WebChromeClient(){
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if(newProgress >= 80) {
                    reSizeZoomLayout(view, false)
                }
            }
        }
    }

    private fun deactivateElementsForUnsend() {
        bodyContainer.alpha = 0.4.toFloat()
    }

    private fun setDefaultBackgroundColors() {
        bodyContainer.alpha = 1.toFloat()
        bodyContainer.isEnabled = true

    }

    fun updateInlineImage(cid: String, filePath: String){
        bodyWebView.evaluateJavascript("replace('cid:$cid', 'file://$filePath');", null)
    }

    fun updateAttachmentProgress(attachmentPosition: Int){
        attachmentsRecyclerView.adapter?.notifyItemChanged(attachmentPosition)
    }

    fun removeAttachmentAt(attachmentPosition: Int){
        attachmentsRecyclerView.adapter?.notifyItemRemoved(attachmentPosition)
    }

    private fun showStartGuideMenu(emailListener: FullEmailListAdapter.OnFullEmailEventListener?, fullEmail: FullEmail){
        if(fullEmail.email.delivered == DeliveryTypes.SENT){
            emailListener?.showStartGuideMenu(threePointsView)
        }
    }

    private fun showStartGuideEmailIsRead(emailListener: FullEmailListAdapter.OnFullEmailEventListener?, fullEmail: FullEmail){
        if(fullEmail.email.delivered == DeliveryTypes.READ){
            emailListener?.showStartGuideEmailIsRead(readView)
        }
    }


    init {
        layout = view.findViewById(R.id.open_full_mail_item_container)
        toView = view.findViewById(R.id.to)
        threePointsView = view.findViewById(R.id.more)
        moreButton = view.findViewById(R.id.more_text)
        replyView = view.findViewById(R.id.reply)
        continueDraftView = view.findViewById(R.id.continue_draft)
        deleteDraftView = view.findViewById(R.id.delete_draft)
        readView =  view.findViewById(R.id.check)
        contactInfoPopUp = EmailContactInfoPopup(moreButton)
        bodyWebView = view.findViewById(R.id.email_body)
        bodyWebView.webChromeClient = setupWeChromeClient()
        bodyContainer = view.findViewById(R.id.body_container)
        rootView = view.findViewById(R.id.cardview)
        attachmentsRecyclerView = view.findViewById(R.id.attachments_recycler_view)
        leftImageView = view.findViewById(R.id.mail_item_left_name)
        unsendProgressBar = view.findViewById(R.id.loadingPanel)
        zoomLayout = view.findViewById(R.id.zoomLayout)
        isSecure = view.findViewById(R.id.email_is_secure)
        setupWebview()
    }
}
